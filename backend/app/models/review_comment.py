from __future__ import annotations

from datetime import datetime

from sqlmodel import Field, SQLModel


class ReviewComment(SQLModel, table=True):
    __tablename__ = "review_comment"

    id: int | None = Field(default=None, primary_key=True)
    review_id: int = Field(foreign_key="review.id", index=True)
    author_id: int = Field(foreign_key="user.id")
    body: str
    created_at: datetime = Field(default_factory=datetime.utcnow)


class ReviewCommentVote(SQLModel, table=True):
    __tablename__ = "review_comment_vote"

    user_id: int = Field(foreign_key="user.id", primary_key=True)
    comment_id: int = Field(foreign_key="review_comment.id", primary_key=True)
    value: int = Field(description="1 = up, -1 = down")
    created_at: datetime = Field(default_factory=datetime.utcnow)
