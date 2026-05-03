import logging

from fastapi import APIRouter, Depends, HTTPException, status
from sqlmodel import Session, select

from app.database import get_db
from app.models.user import User
from app.schemas.user import (
    AuthResponse,
    PasswordRecoveryRequest,
    TokenRefreshRequest,
    TokenResponse,
    UserCreate,
    UserLogin,
    UserRead,
)
from app.services.auth_service import create_access_token, create_refresh_token, decode_token, get_password_hash, verify_password


logger = logging.getLogger(__name__)


router = APIRouter(prefix="/auth", tags=["auth"])


def _to_user_read(user: User) -> UserRead:
    return UserRead(
        id=user.id or 0,
        username=user.username,
        display_name=user.display_name,
        avatar_url=user.avatar_url,
        bio=user.bio,
        created_at=user.created_at,
    )


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
def register(payload: UserCreate, db: Session = Depends(get_db)) -> AuthResponse:
    existing = db.exec(select(User).where((User.username == payload.username) | (User.email == payload.email))).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="already_exists")
    user = User(
        username=payload.username,
        email=payload.email,
        hashed_password=get_password_hash(payload.password),
        display_name=payload.display_name,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    token = create_access_token(str(user.id))
    refresh_token = create_refresh_token(str(user.id))
    return AuthResponse(access_token=token, refresh_token=refresh_token, user=_to_user_read(user))


@router.post("/login", response_model=AuthResponse)
def login(payload: UserLogin, db: Session = Depends(get_db)) -> AuthResponse:
    user = db.exec(select(User).where(User.username == payload.username)).first()
    if user is None or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid_token")
    token = create_access_token(str(user.id))
    refresh_token = create_refresh_token(str(user.id))
    return AuthResponse(access_token=token, refresh_token=refresh_token, user=_to_user_read(user))


@router.post("/refresh", response_model=TokenResponse)
def refresh(payload: TokenRefreshRequest, db: Session = Depends(get_db)) -> TokenResponse:
    try:
        user_id = int(decode_token(payload.refresh_token, expected_token_type="refresh"))
    except (TypeError, ValueError):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid_token")
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid_token")
    return TokenResponse(access_token=create_access_token(str(user.id)), refresh_token=create_refresh_token(str(user.id)))


@router.post("/password-recovery")
def password_recovery(payload: PasswordRecoveryRequest) -> dict[str, str]:
    logger.info("password recovery requested for %s", payload.email_or_username)
    return {"detail": "ok"}
