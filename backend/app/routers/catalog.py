import json

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlmodel import Session, func, select

from app.database import get_db
from app.models.catalog import Album, Artist, Track
from app.models.review import Review, ReviewTarget
from app.schemas.catalog import AlbumRead, ArtistRead, CatalogSearchResponse, TrackRead
from app.services.spotify_service import fetch_album_tracks, fetch_artist_albums, search_and_cache


router = APIRouter(prefix="/catalog", tags=["catalog"])


def _artist_avg_rating(db: Session, artist_id: int) -> float:
    row = db.exec(
        select(func.avg(Review.rating))
        .select_from(Review)
        .join(Album, (Review.target_type == ReviewTarget.album) & (Review.target_id == Album.id))
        .where(Album.artist_id == artist_id)
    ).one()
    return float(row or 0.0)


def _album_stats(db: Session, album_id: int) -> tuple[float, int]:
    row = db.exec(select(func.avg(Review.rating), func.count()).where(Review.target_type == ReviewTarget.album, Review.target_id == album_id)).one()
    return float(row[0] or 0.0), int(row[1] or 0)


def _track_stats(db: Session, track_id: int) -> tuple[float, int]:
    row = db.exec(select(func.avg(Review.rating), func.count()).where(Review.target_type == ReviewTarget.track, Review.target_id == track_id)).one()
    return float(row[0] or 0.0), int(row[1] or 0)


def _artist_to_read(db: Session, artist: Artist) -> ArtistRead:
    genres: list[str] = []
    if artist.genres:
        try:
            parsed = json.loads(artist.genres)
            if isinstance(parsed, list):
                genres = [str(item) for item in parsed]
        except json.JSONDecodeError:
            genres = []
    return ArtistRead(id=artist.id or 0, spotify_id=artist.spotify_id, name=artist.name, image_url=artist.image_url, genres=genres, avg_rating=_artist_avg_rating(db, artist.id or 0))


def _album_to_read(db: Session, album: Album) -> AlbumRead:
    avg_rating, review_count = _album_stats(db, album.id or 0)
    artist = db.get(Artist, album.artist_id)
    return AlbumRead(id=album.id or 0, spotify_id=album.spotify_id, title=album.title, artist_name=artist.name if artist else "", artist_id=album.artist_id, image_url=album.image_url, release_date=album.release_date, album_type=album.album_type, avg_rating=avg_rating, review_count=review_count)


def _track_to_read(db: Session, track: Track) -> TrackRead:
    avg_rating, review_count = _track_stats(db, track.id or 0)
    album = db.get(Album, track.album_id)
    artist = db.get(Artist, album.artist_id) if album else None
    return TrackRead(id=track.id or 0, spotify_id=track.spotify_id, title=track.title, artist_name=artist.name if artist else "", album_title=album.title if album else "", album_id=track.album_id, album_image_url=album.image_url if album else None, duration_ms=track.duration_ms, preview_url=track.preview_url, avg_rating=avg_rating, review_count=review_count)


@router.get("/search", response_model=CatalogSearchResponse)
def search_catalog(q: str = Query(min_length=2), type: str = Query("all"), limit: int = Query(10, ge=1, le=50), db: Session = Depends(get_db)) -> CatalogSearchResponse:
    results = search_and_cache(q, db, limit=limit)
    want_artists = type in {"all", "artist"}
    want_albums = type in {"all", "album"}
    want_tracks = type in {"all", "track"}
    return CatalogSearchResponse(
        artists=[_artist_to_read(db, artist) for artist in results["artists"][:limit] if artist.id] if want_artists else [],
        albums=[_album_to_read(db, album) for album in results["albums"][:limit] if album.id] if want_albums else [],
        tracks=[_track_to_read(db, track) for track in results["tracks"][:limit] if track.id] if want_tracks else [],
    )


@router.get("/artists/{artist_id}", response_model=ArtistRead)
def read_artist(artist_id: int, db: Session = Depends(get_db)) -> ArtistRead:
    artist = db.get(Artist, artist_id)
    if artist is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    return _artist_to_read(db, artist)


@router.get("/artists/{artist_id}/albums", response_model=list[AlbumRead])
def artist_albums(artist_id: int, db: Session = Depends(get_db)) -> list[AlbumRead]:
    artist = db.get(Artist, artist_id)
    if artist is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    albums = db.exec(select(Album).where(Album.artist_id == artist_id)).all()
    if not albums:
        albums = fetch_artist_albums(artist.spotify_id, db)
        db.commit()
    return [_album_to_read(db, album) for album in albums]


@router.get("/albums/{album_id}", response_model=AlbumRead)
def read_album(album_id: int, db: Session = Depends(get_db)) -> AlbumRead:
    album = db.get(Album, album_id)
    if album is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    return _album_to_read(db, album)


@router.get("/albums/{album_id}/tracks", response_model=list[TrackRead])
def album_tracks(album_id: int, db: Session = Depends(get_db)) -> list[TrackRead]:
    album = db.get(Album, album_id)
    if album is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    tracks = db.exec(select(Track).where(Track.album_id == album_id)).all()
    if not tracks:
        tracks = fetch_album_tracks(album.spotify_id, db)
        db.commit()
    return [_track_to_read(db, track) for track in tracks]


@router.get("/tracks/{track_id}", response_model=TrackRead)
def read_track(track_id: int, db: Session = Depends(get_db)) -> TrackRead:
    track = db.get(Track, track_id)
    if track is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    return _track_to_read(db, track)
