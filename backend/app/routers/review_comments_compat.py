from fastapi import APIRouter, Depends, Query, status
from sqlmodel import Session

from app.database import get_db
from app.dependencies import get_current_user, get_optional_user
from app.models.user import User
from app.schemas.feed import Page
from app.schemas.review_comment import ReviewCommentCreate, ReviewCommentRead, ReviewCommentVotePayload
from app.routers.reviews import (
    create_review_comment_result,
    list_review_comments_result,
    vote_review_comment_result,
)

router = APIRouter(prefix="/review-comments", tags=["review-comments"])


@router.get("/{review_id}", response_model=Page[ReviewCommentRead])
def list_comments_flat(
    review_id: int,
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    viewer: User | None = Depends(get_optional_user),
) -> Page[ReviewCommentRead]:
    return list_review_comments_result(db, review_id, page, limit, viewer.id if viewer else None)


@router.post("/{review_id}", response_model=ReviewCommentRead, status_code=status.HTTP_201_CREATED)
def create_comment_flat(
    review_id: int,
    payload: ReviewCommentCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ReviewCommentRead:
    return create_review_comment_result(db, review_id, current_user.id or 0, payload.body)


@router.post("/{review_id}/vote/{comment_id}", response_model=dict[str, str])
def vote_comment_flat(
    review_id: int,
    comment_id: int,
    payload: ReviewCommentVotePayload,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    return vote_review_comment_result(db, review_id, comment_id, current_user.id or 0, payload.direction)
