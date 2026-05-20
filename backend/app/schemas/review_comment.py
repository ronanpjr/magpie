from datetime import datetime

from app.schemas.user import UserRead
from sqlmodel import SQLModel


class ReviewCommentCreate(SQLModel):
    body: str


class ReviewCommentVotePayload(SQLModel):
    direction: str


class ReviewCommentRead(SQLModel):
    id: int
    author: UserRead
    body: str
    created_at: datetime
    up_count: int = 0
    down_count: int = 0
    my_vote: str | None = None
