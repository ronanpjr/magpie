from datetime import datetime

from sqlmodel import Field, SQLModel


class Artist(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    spotify_id: str = Field(unique=True, index=True)
    name: str = Field(index=True)
    image_url: str | None = None
    genres: str | None = None
    cached_at: datetime = Field(default_factory=datetime.utcnow)


class Album(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    spotify_id: str = Field(unique=True, index=True)
    title: str = Field(index=True)
    artist_id: int = Field(foreign_key="artist.id")
    image_url: str | None = None
    release_date: str | None = None
    album_type: str | None = None
    cached_at: datetime = Field(default_factory=datetime.utcnow)


class Track(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    spotify_id: str = Field(unique=True, index=True)
    title: str = Field(index=True)
    album_id: int = Field(foreign_key="album.id")
    duration_ms: int | None = None
    track_number: int | None = None
    preview_url: str | None = None
    cached_at: datetime = Field(default_factory=datetime.utcnow)
