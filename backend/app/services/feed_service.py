from __future__ import annotations

import json
from datetime import datetime, timedelta

from sqlmodel import Session, select

from app.models.catalog import Album, Artist, Track
from app.models.follow import Follow
from app.models.like import Like
from app.models.review import Review
from app.models.review import ReviewTarget


def assemble_feed(db: Session, user_id: int | None, limit: int = 20, page: int = 1) -> list[Review]:
    if user_id is None:
        rows = _popular_reviews(db)
        return rows[(page - 1) * limit : page * limit]

    recent_cutoff = datetime.utcnow() - timedelta(hours=48)
    followed_ids = select(Follow.followed_id).where(Follow.follower_id == user_id)

    followed_recent = db.exec(
        select(Review)
        .where(Review.author_id.in_(followed_ids), Review.created_at >= recent_cutoff)
        .order_by(Review.created_at.desc())
    ).all()

    seen_ids = {review.id for review in followed_recent if review.id is not None}
    history_genres = _user_genres(db, user_id)

    history_popular = [review for review in _popular_reviews(db, genres=history_genres) if review.id not in seen_ids]
    seen_ids.update(review.id for review in history_popular if review.id is not None)

    global_popular = [review for review in _popular_reviews(db) if review.id not in seen_ids]

    rows = followed_recent + history_popular + global_popular
    unique: list[Review] = []
    seen: set[int] = set()
    for row in rows:
        if row.id and row.id not in seen:
            seen.add(row.id)
            unique.append(row)
    return unique[(page - 1) * limit : page * limit]


def _user_genres(db: Session, user_id: int) -> set[str]:
    genres: set[str] = set()
    album_review_targets = db.exec(select(Review.target_id).where(Review.author_id == user_id, Review.target_type == ReviewTarget.album)).all()
    if not album_review_targets:
        return genres
    albums = db.exec(select(Album).where(Album.id.in_(album_review_targets))).all()
    artist_ids = {album.artist_id for album in albums}
    if not artist_ids:
        return genres
    artists = db.exec(select(Artist).where(Artist.id.in_(artist_ids))).all()
    for artist in artists:
        if artist.genres:
            try:
                parsed = json.loads(artist.genres)
                if isinstance(parsed, list):
                    genres.update(str(item).lower() for item in parsed)
            except json.JSONDecodeError:
                continue
    return genres


def _popular_reviews(db: Session, genres: set[str] | None = None) -> list[Review]:
    rows = db.exec(select(Review).order_by(Review.created_at.desc())).all()
    if genres:
        rows = [review for review in rows if _review_matches_genres(db, review, genres)]
    return sorted(rows, key=lambda review: (_review_like_count(db, review.id or 0), review.created_at), reverse=True)


def _review_like_count(db: Session, review_id: int) -> int:
    return int(db.exec(select(Like).where(Like.review_id == review_id)).all().__len__())


def _review_matches_genres(db: Session, review: Review, genres: set[str]) -> bool:
    artist = _review_artist(db, review)
    if artist is None or not artist.genres:
        return False
    try:
        parsed = json.loads(artist.genres)
        artist_genres = {str(item).lower() for item in parsed if item is not None} if isinstance(parsed, list) else set()
    except json.JSONDecodeError:
        return False
    return bool(artist_genres & genres)


def _review_artist(db: Session, review: Review) -> Artist | None:
    if review.target_type == ReviewTarget.album:
        album = db.get(Album, review.target_id)
        if album is None:
            return None
        return db.get(Artist, album.artist_id)
    track = db.get(Track, review.target_id)
    if track is None:
        return None
    album = db.get(Album, track.album_id)
    if album is None:
        return None
    return db.get(Artist, album.artist_id)
