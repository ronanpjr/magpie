from __future__ import annotations

from datetime import datetime
from enum import Enum

from sqlalchemy import UniqueConstraint
from sqlmodel import Field, SQLModel


class ReviewTarget(str, Enum):
    track = "track"
    album = "album"


class Review(SQLModel, table=True):
    __table_args__ = (UniqueConstraint("author_id", "target_type", "target_id"),)

    id: int | None = Field(default=None, primary_key=True)
    author_id: int = Field(foreign_key="user.id")
    target_type: ReviewTarget
    target_id: int
    rating: float = Field(ge=0, le=5)
    body: str | None = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
