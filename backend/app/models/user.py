from datetime import datetime

from sqlmodel import Field, SQLModel


class User(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    username: str = Field(unique=True, index=True)
    email: str = Field(unique=True, index=True)
    hashed_password: str
    display_name: str
    avatar_url: str | None = None
    bio: str | None = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
