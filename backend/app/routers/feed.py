from fastapi import APIRouter, Depends, Query
from sqlmodel import Session

from app.database import get_db
from app.dependencies import get_current_user, get_optional_user
from app.models.user import User
from app.schemas.feed import Page
from app.schemas.review import ReviewRead
from app.services.feed_service import assemble_feed
from app.routers.reviews import _review_read


router = APIRouter(prefix="/feed", tags=["feed"])


@router.get("", response_model=Page[ReviewRead])
def personalized_feed(page: int = Query(1, ge=1), limit: int = Query(20, ge=1, le=100), current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> Page[ReviewRead]:
    rows = assemble_feed(db, current_user.id, limit=limit, page=page)
    return Page[ReviewRead](items=[_review_read(db, row, current_user.id) for row in rows], total=len(rows), page=page, limit=limit, pages=max(1, (len(rows) + limit - 1) // limit))


@router.get("/popular", response_model=Page[ReviewRead])
def popular_feed(page: int = Query(1, ge=1), limit: int = Query(20, ge=1, le=100), db: Session = Depends(get_db), viewer: User | None = Depends(get_optional_user)) -> Page[ReviewRead]:
    rows = assemble_feed(db, viewer.id if viewer else None, limit=limit, page=page)
    return Page[ReviewRead](items=[_review_read(db, row, viewer.id if viewer else None) for row in rows], total=len(rows), page=page, limit=limit, pages=max(1, (len(rows) + limit - 1) // limit))
