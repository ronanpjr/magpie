from __future__ import annotations

import json
from datetime import datetime, timedelta

import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
from sqlmodel import Session, select

from app.config import settings
from app.models.catalog import Album, Artist, Track


CACHE_TTL_DAYS = 7


def get_spotify_client() -> spotipy.Spotify:
    if not settings.spotify_client_id or not settings.spotify_client_secret:
        raise RuntimeError("spotify_credentials_missing")
    auth = SpotifyClientCredentials(
        client_id=settings.spotify_client_id,
        client_secret=settings.spotify_client_secret,
    )
    return spotipy.Spotify(auth_manager=auth)


def _is_stale(cached_at: datetime | None) -> bool:
    return cached_at is None or datetime.utcnow() - cached_at > timedelta(days=CACHE_TTL_DAYS)


def _genre_list(genres: str | None) -> list[str]:
    if not genres:
        return []
    try:
        value = json.loads(genres)
        return value if isinstance(value, list) else []
    except json.JSONDecodeError:
        return []


def search_and_cache(q: str, db: Session, limit: int = 10) -> dict[str, list]:
    artists = db.exec(select(Artist).where(Artist.name.ilike(f"%{q}%")).limit(limit)).all()
    albums = db.exec(select(Album).where(Album.title.ilike(f"%{q}%")).limit(limit)).all()
    tracks = db.exec(select(Track).where(Track.title.ilike(f"%{q}%")).limit(limit)).all()
    if len(artists) + len(albums) + len(tracks) >= 3:
        return {"artists": artists, "albums": albums, "tracks": tracks}

    try:
        client = get_spotify_client()
    except RuntimeError:
        return {"artists": artists, "albums": albums, "tracks": tracks}

    results = client.search(q=q, type="artist,album,track", limit=limit)
    for artist_item in results.get("artists", {}).get("items", []):
        artists.append(_upsert_artist(db, artist_item))
    for album_item in results.get("albums", {}).get("items", []):
        albums.append(_upsert_album(db, album_item))
    for track_item in results.get("tracks", {}).get("items", []):
        tracks.append(_upsert_track(db, track_item))
    db.commit()
    return {"artists": artists, "albums": albums, "tracks": tracks}


def _upsert_artist(db: Session, payload: dict) -> Artist:
    spotify_id = payload["id"]
    artist = db.exec(select(Artist).where(Artist.spotify_id == spotify_id)).first()
    if artist is None:
        artist = Artist(
            spotify_id=spotify_id,
            name=payload.get("name", "Unknown Artist"),
            image_url=(payload.get("images") or [{}])[0].get("url"),
            genres=json.dumps(payload.get("genres") or []),
        )
        db.add(artist)
    else:
        artist.name = payload.get("name", artist.name)
        artist.image_url = (payload.get("images") or [{}])[0].get("url")
        artist.genres = json.dumps(payload.get("genres") or _genre_list(artist.genres))
        artist.cached_at = datetime.utcnow()
    db.flush()
    return artist


def _upsert_album(db: Session, payload: dict) -> Album:
    spotify_id = payload["id"]
    album = db.exec(select(Album).where(Album.spotify_id == spotify_id)).first()
    artist_payload = (payload.get("artists") or [{}])[0]
    artist = db.exec(select(Artist).where(Artist.spotify_id == artist_payload.get("id"))).first()
    if artist is None:
        artist = _upsert_artist(
            db,
            {
                "id": artist_payload.get("id", spotify_id),
                "name": artist_payload.get("name", "Unknown Artist"),
                "images": [],
                "genres": [],
            },
        )
    if album is None:
        album = Album(
            spotify_id=spotify_id,
            title=payload.get("name", "Untitled Album"),
            artist_id=artist.id or 0,
            image_url=(payload.get("images") or [{}])[0].get("url"),
            release_date=payload.get("release_date"),
            album_type=payload.get("album_type"),
        )
        db.add(album)
    else:
        album.title = payload.get("name", album.title)
        album.artist_id = artist.id or album.artist_id
        album.image_url = (payload.get("images") or [{}])[0].get("url")
        album.release_date = payload.get("release_date")
        album.album_type = payload.get("album_type")
        album.cached_at = datetime.utcnow()
    db.flush()
    return album


def _upsert_track(db: Session, payload: dict) -> Track:
    spotify_id = payload["id"]
    track = db.exec(select(Track).where(Track.spotify_id == spotify_id)).first()
    album_payload = payload.get("album") or {}
    album = db.exec(select(Album).where(Album.spotify_id == album_payload.get("id"))).first()
    if album is None:
        album = _upsert_album(
            db,
            {
                "id": album_payload.get("id", spotify_id),
                "name": album_payload.get("name", "Untitled Album"),
                "artists": album_payload.get("artists") or [],
                "images": album_payload.get("images") or [],
                "release_date": album_payload.get("release_date"),
                "album_type": album_payload.get("album_type"),
            },
        )
    if track is None:
        track = Track(
            spotify_id=spotify_id,
            title=payload.get("name", "Untitled Track"),
            album_id=album.id or 0,
            duration_ms=payload.get("duration_ms"),
            track_number=payload.get("track_number"),
            preview_url=payload.get("preview_url"),
        )
        db.add(track)
    else:
        track.title = payload.get("name", track.title)
        track.album_id = album.id or track.album_id
        track.duration_ms = payload.get("duration_ms")
        track.track_number = payload.get("track_number")
        track.preview_url = payload.get("preview_url")
        track.cached_at = datetime.utcnow()
    db.flush()
    return track


def fetch_artist_albums(spotify_id: str, db: Session) -> list[Album]:
    try:
        client = get_spotify_client()
    except RuntimeError:
        return []
    albums_payload = client.artist_albums(spotify_id, album_type="album,single,compilation", limit=50).get("items", [])
    return [_upsert_album(db, payload) for payload in albums_payload]


def fetch_album_tracks(spotify_id: str, db: Session) -> list[Track]:
    try:
        client = get_spotify_client()
    except RuntimeError:
        return []
    album_payload = client.album(spotify_id)
    album = _upsert_album(db, album_payload)
    tracks_payload = client.album_tracks(spotify_id, limit=50).get("items", [])
    tracks = []
    for item in tracks_payload:
        item["album"] = album_payload
        tracks.append(_upsert_track(db, item))
    db.commit()
    return tracks
