from __future__ import annotations

import random
import json
from dataclasses import dataclass

from sqlmodel import Session, select

from app.database import create_db_and_tables, engine
from app.models.catalog import Album, Artist, Track
from app.models.review import Review, ReviewTarget
from app.models.user import User
from app.services.auth_service import get_password_hash


@dataclass(frozen=True)
class AlbumSeed:
    artist_name: str
    album_title: str
    genres: list[str]
    release_date: str
    track_titles: list[str]


ALBUMS: list[AlbumSeed] = [
    AlbumSeed("Pink Floyd", "The Dark Side of the Moon", ["progressive rock", "psychedelic rock"], "1973-03-01", ["Speak to Me", "Time", "Money"]),
    AlbumSeed("The Beatles", "Abbey Road", ["rock", "pop"], "1969-09-26", ["Come Together", "Something", "Here Comes the Sun"]),
    AlbumSeed("Michael Jackson", "Thriller", ["pop", "r&b"], "1982-11-30", ["Thriller", "Beat It", "Billie Jean"]),
    AlbumSeed("Radiohead", "OK Computer", ["alternative rock", "art rock"], "1997-06-16", ["Airbag", "Paranoid Android", "Karma Police"]),
    AlbumSeed("Nirvana", "Nevermind", ["grunge", "alternative rock"], "1991-09-24", ["Smells Like Teen Spirit", "In Bloom", "Come as You Are"]),
]


def seed() -> None:
    create_db_and_tables()
    with Session(engine) as session:
        artists: dict[str, Artist] = {}
        for index, album_seed in enumerate(ALBUMS, start=1):
            artist = session.exec(select(Artist).where(Artist.name == album_seed.artist_name)).first()
            if artist is None:
                artist = Artist(
                    spotify_id=f"seed-artist-{index}",
                    name=album_seed.artist_name,
                    image_url=None,
                    genres=json.dumps(album_seed.genres),
                )
                session.add(artist)
                session.flush()
            artists[album_seed.artist_name] = artist

            album = session.exec(select(Album).where(Album.title == album_seed.album_title)).first()
            if album is None:
                album = Album(
                    spotify_id=f"seed-album-{index}",
                    title=album_seed.album_title,
                    artist_id=artist.id or 0,
                    release_date=album_seed.release_date,
                    album_type="album",
                )
                session.add(album)
                session.flush()

            existing_tracks = session.exec(select(Track).where(Track.album_id == album.id)).all()
            if not existing_tracks:
                for track_index, title in enumerate(album_seed.track_titles, start=1):
                    session.add(
                        Track(
                            spotify_id=f"seed-track-{index}-{track_index}",
                            title=title,
                            album_id=album.id or 0,
                            track_number=track_index,
                            duration_ms=random.randint(180_000, 320_000),
                        )
                    )

        users: list[User] = []
        for idx in range(1, 6):
            username = f"listener{idx}"
            user = session.exec(select(User).where(User.username == username)).first()
            if user is None:
                user = User(
                    username=username,
                    email=f"listener{idx}@magpie.dev",
                    hashed_password=get_password_hash("password123"),
                    display_name=f"Listener {idx}",
                    bio="Seeded user for local development",
                )
                session.add(user)
                session.flush()
            users.append(user)

        album_rows = session.exec(select(Album)).all()
        for user in users:
            for album in random.sample(album_rows, k=min(2, len(album_rows))):
                existing = session.exec(
                    select(Review).where(
                        Review.author_id == user.id,
                        Review.target_type == ReviewTarget.album,
                        Review.target_id == album.id,
                    )
                ).first()
                if existing is None:
                    session.add(
                        Review(
                            author_id=user.id or 0,
                            target_type=ReviewTarget.album,
                            target_id=album.id or 0,
                            rating=random.choice([3.5, 4.0, 4.5, 5.0]),
                            body=f"A seeded review of {album.title}.",
                        )
                    )

        session.commit()


if __name__ == "__main__":
    seed()
