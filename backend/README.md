# Magpie Backend

REST API for the Magpie academic music review app.

## Prerequisites

- Python 3.11+
- Environment variables from `.env.example`

## Local run

```bash
pip install -r requirements.txt
cp .env.example .env
# fill in `.env` with your values
alembic upgrade head
uvicorn app.main:app --reload
```

Swagger: `http://localhost:8000/docs`

## Endpoint overview

- `POST /auth/register` — create account
- `POST /auth/login` — authenticate and return JWT
- `POST /auth/refresh` — renew token
- `POST /auth/password-recovery` — mocked recovery flow
- `GET /users/me` — current user profile
- `PUT /users/me` — update current user profile
- `GET /users/{id}` — public profile
- `GET /users/{id}/reviews` — reviews for a user
- `POST /users/{id}/follow` — follow a user
- `DELETE /users/{id}/follow` — unfollow a user
- `GET /users/{id}/followers` — list followers
- `GET /users/{id}/following` — list following
- `GET /catalog/search` — search catalog
- `GET /catalog/artists/{id}` — artist detail
- `GET /catalog/artists/{id}/albums` — artist discography
- `GET /catalog/albums/{id}` — album detail
- `GET /catalog/albums/{id}/tracks` — album tracks
- `GET /catalog/tracks/{id}` — track detail
- `GET /reviews` — list reviews
- `POST /reviews` — create review
- `GET /reviews/{id}` — review detail
- `GET /reviews/{id}/comments` — list comments
- `POST /reviews/{id}/comments` — add comment
- `POST /reviews/{id}/comments/{comment_id}/vote` — vote on comment
- `GET /review-comments/{id}` — list comments (flat path)
- `POST /review-comments/{id}` — add comment (flat path)
- `POST /review-comments/{id}/vote/{comment_id}` — vote (flat path)
- `PUT /reviews/{id}` — edit review
- `DELETE /reviews/{id}` — delete review
- `POST /reviews/{id}/like` — like review
- `DELETE /reviews/{id}/like` — unlike review
- `GET /feed` — personalized feed
- `GET /feed/popular` — global popular feed
