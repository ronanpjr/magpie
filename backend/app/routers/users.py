from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlmodel import Session, func, select

from app.database import get_db
from app.dependencies import get_current_user, get_optional_user
from app.models.follow import Follow
from app.models.review import Review
from app.models.user import User
from app.schemas.feed import Page
from app.schemas.review import ReviewRead
from app.schemas.user import UserRead, UserUpdate
from app.routers.reviews import _review_read


router = APIRouter(prefix="/users", tags=["users"])


def _user_stats(db: Session, user_id: int) -> tuple[int, int]:
    follower_count = db.exec(select(func.count()).select_from(Follow).where(Follow.followed_id == user_id)).one()
    following_count = db.exec(select(func.count()).select_from(Follow).where(Follow.follower_id == user_id)).one()
    return int(follower_count), int(following_count)


def _to_user_read(db: Session, user: User, viewer_id: int | None = None) -> UserRead:
    follower_count, following_count = _user_stats(db, user.id or 0)
    is_following = False
    if viewer_id is not None and viewer_id != user.id:
        is_following = db.get(Follow, (viewer_id, user.id or 0)) is not None
    return UserRead(
        id=user.id or 0,
        username=user.username,
        display_name=user.display_name,
        avatar_url=user.avatar_url,
        bio=user.bio,
        created_at=user.created_at,
        follower_count=follower_count,
        following_count=following_count,
        is_following=is_following,
    )


@router.get("/me", response_model=UserRead)
def read_me(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> UserRead:
    return _to_user_read(db, current_user, current_user.id)


@router.put("/me", response_model=UserRead)
def update_me(payload: UserUpdate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> UserRead:
    data = payload.model_dump(exclude_unset=True)
    for key, value in data.items():
        setattr(current_user, key, value)
    db.add(current_user)
    db.commit()
    db.refresh(current_user)
    return _to_user_read(db, current_user, current_user.id)


@router.get("/search", response_model=Page[UserRead])
def search_users(
    q: str = Query(..., min_length=1),
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Page[UserRead]:
    MAX_RESULTS = 100
    # Search query is case-insensitive, matches both username and display_name
    search_term = f"%{q}%"
    query = select(User).where(
        (User.username.ilike(search_term)) | (User.display_name.ilike(search_term))
    )
    rows = db.exec(query).all()
    total = min(len(rows), MAX_RESULTS)
    # Limit results to MAX_RESULTS and then apply pagination
    limited_rows = rows[:MAX_RESULTS]
    sliced = limited_rows[(page - 1) * limit : page * limit]
    items = [_to_user_read(db, user, current_user.id) for user in sliced]
    return Page[UserRead](
        items=items,
        total=total,
        page=page,
        limit=limit,
        pages=max(1, (total + limit - 1) // limit),
    )


@router.get("/{user_id}", response_model=UserRead)
def read_user(user_id: int, viewer: User | None = Depends(get_optional_user), db: Session = Depends(get_db)) -> UserRead:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    return _to_user_read(db, user, viewer.id if viewer else None)


@router.post("/{user_id}/follow", status_code=status.HTTP_200_OK)
def follow_user(user_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, str]:
    if user_id == current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="forbidden")
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    existing = db.get(Follow, (current_user.id, user_id))
    if existing is None:
        db.add(Follow(follower_id=current_user.id or 0, followed_id=user_id))
        db.commit()
    return {"detail": "ok"}


@router.delete("/{user_id}/follow", status_code=status.HTTP_200_OK)
def unfollow_user(user_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, str]:
    follow = db.get(Follow, (current_user.id, user_id))
    if follow is not None:
        db.delete(follow)
        db.commit()
    return {"detail": "ok"}


@router.get("/{user_id}/reviews", response_model=Page[ReviewRead])
def user_reviews(user_id: int, page: int = Query(1, ge=1), limit: int = Query(20, ge=1, le=100), db: Session = Depends(get_db), viewer: User | None = Depends(get_optional_user)) -> Page[ReviewRead]:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    query = select(Review).where(Review.author_id == user_id).order_by(Review.created_at.desc())
    rows = db.exec(query).all()
    total = len(rows)
    sliced = rows[(page - 1) * limit : page * limit]
    items = [_review_read(db, review, viewer.id if viewer else None) for review in sliced]
    return Page[ReviewRead](items=items, total=total, page=page, limit=limit, pages=max(1, (total + limit - 1) // limit))


@router.get("/{user_id}/followers", response_model=Page[UserRead])
def followers(user_id: int, page: int = Query(1, ge=1), limit: int = Query(20, ge=1, le=100), db: Session = Depends(get_db), viewer: User | None = Depends(get_optional_user)) -> Page[UserRead]:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    rows = db.exec(select(Follow).where(Follow.followed_id == user_id)).all()
    total = len(rows)
    items = [_to_user_read(db, db.get(User, row.follower_id), viewer.id if viewer else None) for row in rows[(page - 1) * limit : page * limit] if db.get(User, row.follower_id)]
    return Page[UserRead](items=items, total=total, page=page, limit=limit, pages=max(1, (total + limit - 1) // limit))


@router.get("/{user_id}/following", response_model=Page[UserRead])
def following(user_id: int, page: int = Query(1, ge=1), limit: int = Query(20, ge=1, le=100), db: Session = Depends(get_db), viewer: User | None = Depends(get_optional_user)) -> Page[UserRead]:
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    rows = db.exec(select(Follow).where(Follow.follower_id == user_id)).all()
    total = len(rows)
    items = [_to_user_read(db, db.get(User, row.followed_id), viewer.id if viewer else None) for row in rows[(page - 1) * limit : page * limit] if db.get(User, row.followed_id)]
    return Page[UserRead](items=items, total=total, page=page, limit=limit, pages=max(1, (total + limit - 1) // limit))
