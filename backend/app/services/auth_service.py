from datetime import datetime, timedelta, timezone
from typing import Any

from jose import JWTError, jwt
from passlib.context import CryptContext

from app.config import settings


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def get_password_hash(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)


def _create_token(subject: str, token_type: str, expires_delta: timedelta) -> str:
    expire = datetime.now(timezone.utc) + expires_delta
    payload: dict[str, Any] = {"sub": subject, "exp": expire, "token_type": token_type}
    return jwt.encode(payload, settings.secret_key, algorithm=settings.algorithm)


def create_access_token(subject: str, expires_minutes: int | None = None) -> str:
    expire_delta = timedelta(minutes=expires_minutes or settings.access_token_expire_minutes)
    return _create_token(subject, "access", expire_delta)


def create_refresh_token(subject: str, expires_days: int = 7) -> str:
    return _create_token(subject, "refresh", timedelta(days=expires_days))


def decode_token(token: str, expected_token_type: str | None = "access") -> str:
    try:
        payload = jwt.decode(token, settings.secret_key, algorithms=[settings.algorithm])
        subject = payload.get("sub")
        if subject is None:
            raise JWTError("missing subject")
        token_type = payload.get("token_type")
        if expected_token_type is not None and token_type != expected_token_type:
            raise JWTError("invalid token type")
        return str(subject)
    except JWTError as exc:
        raise ValueError("invalid_token") from exc
