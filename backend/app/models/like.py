from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING

from sqlmodel import Field, Relationship, SQLModel

if TYPE_CHECKING:
    from app.models.review import Review


class Like(SQLModel, table=True):
    user_id: int = Field(foreign_key="user.id", primary_key=True)
    review_id: int = Field(foreign_key="review.id", primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)
