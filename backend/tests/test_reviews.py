from __future__ import annotations

from sqlmodel import Session

from app.models.catalog import Album, Artist, Track


def _create_music(session: Session) -> tuple[int, int]:
    artist = Artist(spotify_id="artist-1", name="Famous Artist", genres='["rock"]')
    session.add(artist)
    session.flush()
    album = Album(spotify_id="album-1", title="Famous Album", artist_id=artist.id or 0)
    session.add(album)
    session.flush()
    track = Track(spotify_id="track-1", title="Famous Track", album_id=album.id or 0)
    session.add(track)
    session.commit()
    return album.id or 0, track.id or 0


def test_create_review_duplicate_like_flow(auth_client, db_session: Session):
    session = db_session
    album_id, _ = _create_music(session)

    create = auth_client.post(
        "/reviews",
        json={"target_type": "album", "target_id": album_id, "rating": 4.5, "body": "Great album"},
    )
    assert create.status_code == 201

    duplicate = auth_client.post(
        "/reviews",
        json={"target_type": "album", "target_id": album_id, "rating": 4.5, "body": "Great album"},
    )
    assert duplicate.status_code == 409

    like = auth_client.post(f"/reviews/{create.json()['id']}/like")
    assert like.status_code == 200

    unlike = auth_client.delete(f"/reviews/{create.json()['id']}/like")
    assert unlike.status_code == 200


def test_edit_and_delete_authorization(client, db_session: Session):
    client.post(
        "/auth/register",
        json={"username": "author1", "email": "a1@example.com", "password": "password123", "display_name": "Author 1"},
    )
    token1 = client.post("/auth/login", json={"username": "author1", "password": "password123"}).json()["access_token"]
    client.post(
        "/auth/register",
        json={"username": "author2", "email": "a2@example.com", "password": "password123", "display_name": "Author 2"},
    )
    token2 = client.post("/auth/login", json={"username": "author2", "password": "password123"}).json()["access_token"]

    session = db_session
    album_id, _ = _create_music(session)
    client.headers["Authorization"] = f"Bearer {token1}"
    review = client.post(
        "/reviews",
        json={"target_type": "album", "target_id": album_id, "rating": 4.0, "body": "Nice"},
    )
    review_id = review.json()["id"]

    client.headers["Authorization"] = f"Bearer {token2}"
    forbidden = client.put(f"/reviews/{review_id}", json={"body": "edit"})
    assert forbidden.status_code == 403

    client.headers["Authorization"] = f"Bearer {token1}"
    deleted = client.delete(f"/reviews/{review_id}")
    assert deleted.status_code == 204
