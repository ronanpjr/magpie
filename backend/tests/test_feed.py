from __future__ import annotations

from sqlmodel import Session, select

from app.models.catalog import Album, Artist
from app.models.follow import Follow
from app.models.review import Review, ReviewTarget
from app.models.user import User
from app.services.auth_service import get_password_hash


def _seed_album(session: Session) -> int:
    artist = Artist(spotify_id="artist-feed", name="Feed Artist", genres='["rock"]')
    session.add(artist)
    session.flush()
    album = Album(spotify_id="album-feed", title="Feed Album", artist_id=artist.id or 0)
    session.add(album)
    session.commit()
    return album.id or 0


def test_authenticated_feed_prioritizes_followed_users(auth_client, client, db_session: Session):
    session = db_session
    album_id = _seed_album(session)

    followed = User(username="followed", email="followed@example.com", hashed_password=get_password_hash("password123"), display_name="Followed")
    other = User(username="other", email="other@example.com", hashed_password=get_password_hash("password123"), display_name="Other")
    session.add(followed)
    session.add(other)
    session.flush()
    alice = session.exec(select(User).where(User.username == "alice")).first()
    session.add(Follow(follower_id=alice.id or 0, followed_id=followed.id or 0))
    session.add(Review(author_id=followed.id or 0, target_type=ReviewTarget.album, target_id=album_id, rating=5.0, body="Followed review"))
    session.add(Review(author_id=other.id or 0, target_type=ReviewTarget.album, target_id=album_id, rating=4.0, body="Other review"))
    session.commit()

    response = auth_client.get("/feed")
    assert response.status_code == 200
    items = response.json()["items"]
    assert items[0]["author"]["username"] == "followed"


def test_popular_feed_returns_without_auth(client):
    response = client.get("/feed/popular")
    assert response.status_code == 200
