from pydantic import Field
from sqlmodel import SQLModel


class ArtistRead(SQLModel):
    id: int
    spotify_id: str
    name: str
    image_url: str | None = None
    genres: list[str] = Field(default_factory=list)
    avg_rating: float = 0.0


class AlbumRead(SQLModel):
    id: int
    spotify_id: str
    title: str
    artist_name: str
    artist_id: int
    image_url: str | None = None
    release_date: str | None = None
    album_type: str | None = None
    avg_rating: float = 0.0
    review_count: int = 0


class TrackRead(SQLModel):
    id: int
    spotify_id: str
    title: str
    artist_name: str
    album_title: str
    album_id: int
    album_image_url: str | None = None
    duration_ms: int | None = None
    preview_url: str | None = None
    avg_rating: float = 0.0
    review_count: int = 0


class CatalogSearchResponse(SQLModel):
    artists: list[ArtistRead]
    albums: list[AlbumRead]
    tracks: list[TrackRead]
