from __future__ import annotations

from types import SimpleNamespace

from sqlmodel import Session

from app.models.catalog import Album, Artist, Track


def _get_session(client) -> Session:
    override = client.app.dependency_overrides
    db_dep = override[next(iter(override.keys()))]
    generator = db_dep()
    return next(generator)


def test_search_returns_local_cache_results(client):
    session = _get_session(client)
    artist = Artist(spotify_id="artist-local", name="Local Artist", genres='["rock"]')
    session.add(artist)
    session.flush()
    album = Album(spotify_id="album-local", title="Local Album", artist_id=artist.id or 0)
    session.add(album)
    session.commit()

    response = client.get("/catalog/search?q=Local")
    assert response.status_code == 200
    body = response.json()
    assert body["artists"]
    assert body["albums"]


def test_search_with_cache_miss_triggers_spotify_mock(monkeypatch, client):
    class FakeSpotify:
        def search(self, q, type, limit):
            return {
                "artists": {"items": [{"id": "s-artist", "name": "Spotify Artist", "images": [], "genres": ["rock"]}]},
                "albums": {"items": [{"id": "s-album", "name": "Spotify Album", "artists": [{"id": "s-artist", "name": "Spotify Artist"}], "images": [], "release_date": "2024-01-01", "album_type": "album"}]},
                "tracks": {"items": []},
            }

    monkeypatch.setattr("app.services.spotify_service.get_spotify_client", lambda: FakeSpotify())
    response = client.get("/catalog/search?q=Spotify")
    assert response.status_code == 200
    assert response.json()["artists"]


def test_missing_artist_returns_404(client):
    response = client.get("/catalog/artists/999999")
    assert response.status_code == 404


def test_album_tracks_refreshes_partial_cache(monkeypatch, client):
    session = _get_session(client)
    artist = Artist(spotify_id="artist-refresh", name="Refresh Artist")
    session.add(artist)
    session.flush()
    album = Album(spotify_id="album-refresh", title="Refresh Album", artist_id=artist.id or 0)
    session.add(album)
    session.flush()
    session.add_all(
        [
            Track(spotify_id="track-1", title="Track 1", album_id=album.id or 0),
            Track(spotify_id="track-2", title="Track 2", album_id=album.id or 0),
        ]
    )
    session.commit()

    class FakeSpotify:
        def album(self, spotify_id):
            return {"id": spotify_id, "name": "Refresh Album", "artists": [{"id": artist.spotify_id, "name": artist.name}], "images": [], "release_date": "2024-01-01", "album_type": "album", "total_tracks": 4}

        def album_tracks(self, spotify_id, limit=50):
            return {"items": [
                {"id": "track-1", "name": "Track 1", "album": {"id": spotify_id, "name": "Refresh Album", "artists": [{"id": artist.spotify_id, "name": artist.name}], "images": [], "release_date": "2024-01-01", "album_type": "album"}, "duration_ms": 1000, "track_number": 1, "preview_url": None},
                {"id": "track-2", "name": "Track 2", "album": {"id": spotify_id, "name": "Refresh Album", "artists": [{"id": artist.spotify_id, "name": artist.name}], "images": [], "release_date": "2024-01-01", "album_type": "album"}, "duration_ms": 2000, "track_number": 2, "preview_url": None},
                {"id": "track-3", "name": "Track 3", "album": {"id": spotify_id, "name": "Refresh Album", "artists": [{"id": artist.spotify_id, "name": artist.name}], "images": [], "release_date": "2024-01-01", "album_type": "album"}, "duration_ms": 3000, "track_number": 3, "preview_url": None},
                {"id": "track-4", "name": "Track 4", "album": {"id": spotify_id, "name": "Refresh Album", "artists": [{"id": artist.spotify_id, "name": artist.name}], "images": [], "release_date": "2024-01-01", "album_type": "album"}, "duration_ms": 4000, "track_number": 4, "preview_url": None},
            ]}

    monkeypatch.setattr("app.services.spotify_service.get_spotify_client", lambda: FakeSpotify())

    response = client.get(f"/catalog/albums/{album.id}/tracks")
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 4
    assert [item["title"] for item in body] == ["Track 1", "Track 2", "Track 3", "Track 4"]


def test_album_tracks_uses_complete_cache(monkeypatch, client):
    session = _get_session(client)
    artist = Artist(spotify_id="artist-cached", name="Cached Artist")
    session.add(artist)
    session.flush()
    album = Album(spotify_id="album-cached", title="Cached Album", artist_id=artist.id or 0)
    session.add(album)
    session.flush()
    session.add_all(
        [
            Track(spotify_id="track-a", title="Track A", album_id=album.id or 0),
            Track(spotify_id="track-b", title="Track B", album_id=album.id or 0),
        ]
    )
    session.commit()

    class FakeSpotify:
        def album(self, spotify_id):
            return {"id": spotify_id, "total_tracks": 2}

        def album_tracks(self, spotify_id, limit=50):
            raise AssertionError("should not refetch a complete cache")

    monkeypatch.setattr("app.services.spotify_service.get_spotify_client", lambda: FakeSpotify())

    response = client.get(f"/catalog/albums/{album.id}/tracks")
    assert response.status_code == 200
    assert len(response.json()) == 2
