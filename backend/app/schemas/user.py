from datetime import datetime

from sqlmodel import SQLModel


class UserCreate(SQLModel):
    username: str
    email: str
    password: str
    display_name: str


class UserLogin(SQLModel):
    username: str
    password: str


class UserUpdate(SQLModel):
    display_name: str | None = None
    avatar_url: str | None = None
    bio: str | None = None


class UserRead(SQLModel):
    id: int
    username: str
    display_name: str
    avatar_url: str | None
    bio: str | None
    created_at: datetime
    follower_count: int = 0
    following_count: int = 0
    is_following: bool = False


class TokenResponse(SQLModel):
    access_token: str
    token_type: str = "bearer"
    refresh_token: str | None = None


class AuthResponse(SQLModel):
    access_token: str
    token_type: str = "bearer"
    refresh_token: str | None = None
    user: UserRead


class TokenRefreshRequest(SQLModel):
    refresh_token: str


class PasswordRecoveryRequest(SQLModel):
    email_or_username: str
