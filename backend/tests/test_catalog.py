from __future__ import annotations

from types import SimpleNamespace

from sqlmodel import Session

from app.models.catalog import Album, Artist


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
