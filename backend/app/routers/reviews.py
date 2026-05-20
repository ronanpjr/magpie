from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlmodel import Session, func, select

from app.database import get_db
from app.dependencies import get_current_user, get_optional_user
from app.models.catalog import Album, Artist, Track
from app.models.like import Like
from app.models.review import Review, ReviewTarget
from app.models.review_comment import ReviewComment, ReviewCommentVote
from app.models.user import User
from app.schemas.feed import Page
from app.schemas.review import ReviewCreate, ReviewRead, ReviewUpdate
from app.schemas.review_comment import ReviewCommentCreate, ReviewCommentRead, ReviewCommentVotePayload
from app.schemas.user import UserRead


router = APIRouter(prefix="/reviews", tags=["reviews"])


def _user_read(user: User) -> UserRead:
    return UserRead(id=user.id or 0, username=user.username, display_name=user.display_name, avatar_url=user.avatar_url, bio=user.bio, created_at=user.created_at)


def _review_title(db: Session, review: Review) -> tuple[str, str | None, str]:
    if review.target_type == ReviewTarget.album:
        album = db.get(Album, review.target_id)
        if album:
            artist = db.get(Artist, album.artist_id)
            return album.title, album.image_url, artist.name if artist else ""
    track = db.get(Track, review.target_id)
    if track:
        album = db.get(Album, track.album_id)
        artist = db.get(Artist, album.artist_id) if album else None
        return track.title, album.image_url if album else None, artist.name if artist else ""
    return "", None, ""


def _review_read(db: Session, review: Review, viewer_id: int | None = None) -> ReviewRead:
    title, image_url, artist_name = _review_title(db, review)
    like_count = db.exec(select(func.count()).select_from(Like).where(Like.review_id == review.id)).one()
    liked_by_me = viewer_id is not None and db.get(Like, (viewer_id, review.id or 0)) is not None
    author = db.get(User, review.author_id)
    return ReviewRead(
        id=review.id or 0,
        author=_user_read(author) if author else UserRead(id=0, username="", display_name="", avatar_url=None, bio=None, created_at=review.created_at),
        target_type=review.target_type,
        target_id=review.target_id,
        target_title=title,
        target_image_url=image_url,
        artist_name=artist_name,
        rating=review.rating,
        body=review.body,
        like_count=int(like_count or 0),
        liked_by_me=liked_by_me,
        created_at=review.created_at,
        updated_at=review.updated_at,
    )


@router.get("", response_model=Page[ReviewRead])
def list_reviews(
    target_type: ReviewTarget | None = None,
    target_id: int | None = None,
    author_id: int | None = None,
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    order_by: str = Query("recent"),
    db: Session = Depends(get_db),
    viewer: User | None = Depends(get_optional_user),
) -> Page[ReviewRead]:
    query = select(Review)
    if target_type is not None:
        query = query.where(Review.target_type == target_type)
    if target_id is not None:
        query = query.where(Review.target_id == target_id)
    if author_id is not None:
        query = query.where(Review.author_id == author_id)
    if order_by == "top":
        query = query.order_by(Review.updated_at.desc())
    else:
        query = query.order_by(Review.created_at.desc())
    rows = db.exec(query).all()
    total = len(rows)
    sliced = rows[(page - 1) * limit : page * limit]
    items = [_review_read(db, row, viewer.id if viewer else None) for row in sliced]
    return Page[ReviewRead](items=items, total=total, page=page, limit=limit, pages=max(1, (total + limit - 1) // limit))


@router.post("", response_model=ReviewRead, status_code=status.HTTP_201_CREATED)
def create_review(payload: ReviewCreate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> ReviewRead:
    if payload.target_type == ReviewTarget.album and db.get(Album, payload.target_id) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    if payload.target_type == ReviewTarget.track and db.get(Track, payload.target_id) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    existing = db.exec(select(Review).where(Review.author_id == current_user.id, Review.target_type == payload.target_type, Review.target_id == payload.target_id)).first()
    if existing is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="already_exists")
    review = Review(author_id=current_user.id or 0, target_type=payload.target_type, target_id=payload.target_id, rating=payload.rating, body=payload.body)
    db.add(review)
    db.commit()
    db.refresh(review)
    return _review_read(db, review, current_user.id)


def _comment_read(db: Session, comment: ReviewComment, viewer_id: int | None = None) -> ReviewCommentRead:
    author = db.get(User, comment.author_id)
    if author is None:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="invariant_broken")
    up_count = db.exec(
        select(func.count())
        .select_from(ReviewCommentVote)
        .where(ReviewCommentVote.comment_id == comment.id, ReviewCommentVote.value == 1)
    ).one()
    down_count = db.exec(
        select(func.count())
        .select_from(ReviewCommentVote)
        .where(ReviewCommentVote.comment_id == comment.id, ReviewCommentVote.value == -1)
    ).one()
    my_vote: str | None = None
    cid = comment.id or 0
    if viewer_id is not None and cid:
        vote_row = db.get(ReviewCommentVote, (viewer_id, cid))
        if vote_row is not None:
            my_vote = "up" if vote_row.value == 1 else "down"
    return ReviewCommentRead(
        id=cid,
        author=_user_read(author),
        body=comment.body,
        created_at=comment.created_at,
        up_count=int(up_count or 0),
        down_count=int(down_count or 0),
        my_vote=my_vote,
    )


def list_review_comments_result(
    db: Session, review_id: int, page: int, limit: int, viewer_id: int | None = None
) -> Page[ReviewCommentRead]:
    if db.get(Review, review_id) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    query = (
        select(ReviewComment)
        .where(ReviewComment.review_id == review_id)
        .order_by(ReviewComment.created_at.desc())
    )
    rows = db.exec(query).all()
    total = len(rows)
    sliced = rows[(page - 1) * limit : page * limit]
    items = [_comment_read(db, row, viewer_id) for row in sliced]
    return Page[ReviewCommentRead](
        items=items,
        total=total,
        page=page,
        limit=limit,
        pages=max(1, (total + limit - 1) // limit),
    )


def create_review_comment_result(db: Session, review_id: int, author_id: int, raw_body: str) -> ReviewCommentRead:
    if db.get(Review, review_id) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    body = raw_body.strip()
    if not body:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="empty_body")
    comment = ReviewComment(review_id=review_id, author_id=author_id, body=body)
    db.add(comment)
    db.commit()
    db.refresh(comment)
    return _comment_read(db, comment, author_id)


def vote_review_comment_result(db: Session, review_id: int, comment_id: int, user_id: int, direction: str) -> dict[str, str]:
    comment = db.get(ReviewComment, comment_id)
    if comment is None or comment.review_id != review_id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    val = 1 if direction == "up" else -1
    uid = user_id
    existing = db.get(ReviewCommentVote, (uid, comment_id))
    if existing is None:
        db.add(ReviewCommentVote(user_id=uid, comment_id=comment_id, value=val))
    else:
        existing.value = val
        db.add(existing)
    db.commit()
    return {"detail": "ok"}


@router.get("/{review_id}/comments", response_model=Page[ReviewCommentRead])
def list_review_comments(
    review_id: int,
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    viewer: User | None = Depends(get_optional_user),
) -> Page[ReviewCommentRead]:
    return list_review_comments_result(db, review_id, page, limit, viewer.id if viewer else None)


@router.post("/{review_id}/comments", response_model=ReviewCommentRead, status_code=status.HTTP_201_CREATED)
def create_review_comment(
    review_id: int,
    payload: ReviewCommentCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ReviewCommentRead:
    return create_review_comment_result(db, review_id, current_user.id or 0, payload.body)


@router.post("/{review_id}/comments/{comment_id}/vote", response_model=dict[str, str])
def vote_review_comment(
    review_id: int,
    comment_id: int,
    payload: ReviewCommentVotePayload,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    return vote_review_comment_result(db, review_id, comment_id, current_user.id or 0, payload.direction)


@router.get("/{review_id}", response_model=ReviewRead)
def read_review(review_id: int, db: Session = Depends(get_db), viewer: User | None = Depends(get_optional_user)) -> ReviewRead:
    review = db.get(Review, review_id)
    if review is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    return _review_read(db, review, viewer.id if viewer else None)


@router.put("/{review_id}", response_model=ReviewRead)
def update_review(review_id: int, payload: ReviewUpdate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> ReviewRead:
    review = db.get(Review, review_id)
    if review is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    if review.author_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="forbidden")
    for key, value in payload.model_dump(exclude_unset=True).items():
        setattr(review, key, value)
    db.add(review)
    db.commit()
    db.refresh(review)
    return _review_read(db, review, current_user.id)


@router.delete("/{review_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_review(review_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> None:
    review = db.get(Review, review_id)
    if review is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    if review.author_id != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="forbidden")
    db.delete(review)
    db.commit()


@router.post("/{review_id}/like", response_model=dict[str, str])
def like_review(review_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, str]:
    review = db.get(Review, review_id)
    if review is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not_found")
    if db.get(Like, (current_user.id, review_id)) is None:
        db.add(Like(user_id=current_user.id or 0, review_id=review_id))
        db.commit()
    return {"detail": "ok"}


@router.delete("/{review_id}/like", response_model=dict[str, str])
def unlike_review(review_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, str]:
    like = db.get(Like, (current_user.id, review_id))
    if like is not None:
        db.delete(like)
        db.commit()
    return {"detail": "ok"}
