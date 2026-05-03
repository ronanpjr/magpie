from datetime import datetime

from pydantic import field_validator

from app.models.review import ReviewTarget
from app.schemas.user import UserRead
from sqlmodel import SQLModel


class ReviewCreate(SQLModel):
    target_type: ReviewTarget
    target_id: int
    rating: float
    body: str | None = None

    @field_validator("rating")
    @classmethod
    def validate_half_star_increment(cls, value: float) -> float:
        if round(value * 2) != value * 2:
            raise ValueError("rating_must_be_half_star_increment")
        return value


class ReviewUpdate(SQLModel):
    rating: float | None = None
    body: str | None = None

    @field_validator("rating")
    @classmethod
    def validate_half_star_increment(cls, value: float | None) -> float | None:
        if value is not None and round(value * 2) != value * 2:
            raise ValueError("rating_must_be_half_star_increment")
        return value


class ReviewRead(SQLModel):
    id: int
    author: UserRead
    target_type: ReviewTarget
    target_id: int
    target_title: str
    target_image_url: str | None = None
    artist_name: str
    rating: float
    body: str | None = None
    like_count: int = 0
    liked_by_me: bool = False
    created_at: datetime
    updated_at: datetime
