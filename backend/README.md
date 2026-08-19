# Magpie Backend

> REST API for the Magpie music review platform.

![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![SQLModel](https://img.shields.io/badge/SQLModel-336791?style=for-the-badge&logo=sqlite&logoColor=white)

FastAPI service that powers the Magpie Android app: user authentication (JWT), reviews,
likes, follows, comments, a Spotify-backed music catalog with cache-aside, and a
personalized feed.

## Tech stack

| Component | Technology |
|---|---|
| Framework | FastAPI |
| ORM | SQLModel (SQLAlchemy + Pydantic wrapper) |
| Database | SQLite in development, PostgreSQL in production |
| Migrations | Alembic |
| Authentication | JWT via `python-jose`, bcrypt via `passlib` |
| Spotify client | `spotipy` |
| Server | Uvicorn |
| Tests | `pytest` + FastAPI TestClient |
| Deploy | Docker / Docker Compose |

## Prerequisites

- Python 3.11+
- Environment variables from `.env.example`

## Local run

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env        # fill in with your values
alembic upgrade head        # create/update the database schema
uvicorn app.main:app --reload
```

Swagger docs: `http://localhost:8000/docs`

### Docker

```bash
cp .env.example .env
docker compose up --build
```

The container runs migrations, seeds sample data, and exposes the API on port `8000`.

## Environment variables

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | Database connection string | `sqlite:///./magpie.db` |
| `SECRET_KEY` | Secret used to sign JWTs | — |
| `ALGORITHM` | JWT signing algorithm | `HS256` |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | Token lifetime in minutes | `10080` (7 days) |
| `SPOTIFY_CLIENT_ID` | Spotify API client ID | — |
| `SPOTIFY_CLIENT_SECRET` | Spotify API client secret | — |

## Project structure

```
backend/
├── app/
│   ├── main.py                  # FastAPI instance, router registration, CORS
│   ├── config.py                # Environment configuration
│   ├── database.py              # Engine, session, get_db dependency
│   ├── dependencies.py          # get_current_user, get_optional_user
│   ├── models/                  # SQLModel tables (user, review, like, follow, catalog, comments)
│   ├── schemas/                 # Pydantic request/response models
│   ├── routers/                 # auth, users, catalog, reviews, feed, review-comments
│   └── services/                # JWT, Spotify proxy, feed assembly
├── alembic/                     # Database migrations
├── scripts/                     # Catalog + users/reviews seed scripts
├── tests/                       # pytest suite per router
├── Dockerfile
├── docker-compose.yml
└── requirements.txt
```

## API reference

All endpoints return JSON. Authenticated endpoints expect an
`Authorization: Bearer <token>` header. List endpoints accept `?page=1&limit=20` and
return a paginated envelope:

```json
{
  "items": [...],
  "total": 143,
  "page": 1,
  "limit": 20,
  "pages": 8
}
```

### Auth — `/auth`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/auth/register` | Create an account (returns JWT + user) | No |
| POST | `/auth/login` | Authenticate and return a JWT | No |
| POST | `/auth/refresh` | Renew an access token | No |
| POST | `/auth/password-recovery` | Mocked recovery flow (logs, no email) | No |

### Users — `/users`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/users/me` | Current user's profile | Yes |
| PUT | `/users/me` | Update current user's profile | Yes |
| GET | `/users/{id}` | Public profile of another user | Optional |
| GET | `/users/{id}/reviews` | Reviews written by a user | Optional |
| POST | `/users/{id}/follow` | Follow a user | Yes |
| DELETE | `/users/{id}/follow` | Unfollow a user | Yes |
| GET | `/users/{id}/followers` | List a user's followers | Optional |
| GET | `/users/{id}/following` | List users a user follows | Optional |

### Catalog — `/catalog`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/catalog/search` | Search artists, albums and tracks (`q`, `type`, `limit`) | No |
| GET | `/catalog/artists/{id}` | Artist details | No |
| GET | `/catalog/artists/{id}/albums` | Artist discography | No |
| GET | `/catalog/albums/{id}` | Album details | No |
| GET | `/catalog/albums/{id}/tracks` | Album tracks | No |
| GET | `/catalog/tracks/{id}` | Track details | No |

The catalog proxies the Spotify Web API with a cache-aside strategy: local results are
served first, and the Spotify search is triggered only when the local cache is thin.

### Reviews — `/reviews`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/reviews` | Paginated reviews, filterable by `target_type`, `target_id`, `author_id`, `order_by` | Optional |
| POST | `/reviews` | Create a review (rating 0–5, 0.5 increments; one review per work per user) | Yes |
| GET | `/reviews/{id}` | Review detail | Optional |
| PUT | `/reviews/{id}` | Edit a review (author only) | Yes |
| DELETE | `/reviews/{id}` | Delete a review (author only) | Yes |
| POST | `/reviews/{id}/like` | Like a review | Yes |
| DELETE | `/reviews/{id}/like` | Unlike a review | Yes |

### Review comments — `/reviews/{id}/comments`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/reviews/{id}/comments` | List comments on a review | Optional |
| POST | `/reviews/{id}/comments` | Add a comment to a review | Yes |
| POST | `/reviews/{id}/comments/{comment_id}/vote` | Vote on a comment | Yes |

Flat-path compatibility aliases are also available: `GET/POST /review-comments/{id}` and
`POST /review-comments/{id}/vote/{comment_id}`.

### Feed — `/feed`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/feed` | Personalized feed (followed users first, then popular) | Yes |
| GET | `/feed/popular` | Global popular feed (optionally filtered by `genre`) | No |

## Authentication

- Tokens are JWTs with payload `{ "sub": user_id, "exp": timestamp }`.
- Send them as `Authorization: Bearer <token>`.
- `ACCESS_TOKEN_EXPIRE_MINUTES` defaults to 7 days — no mandatory refresh for the academic
  context.
- Passwords are hashed with bcrypt before storage.

## Error handling

| Situation | Status | Detail |
|---|---|---|
| Invalid or expired token | 401 | `invalid_token` |
| Not authenticated | 401 | `not_authenticated` |
| Not the author (no permission) | 403 | `forbidden` |
| Resource not found | 404 | `not_found` |
| Duplicate (review, username, email) | 409 | `already_exists` |
| Field validation | 422 | FastAPI auto-generated |
| Spotify API error | 503 | `catalog_unavailable` (local cache is served) |

Stack traces are never returned in production — a global handler returns
`{"detail": "internal_error"}` on unhandled exceptions.

## Testing

```bash
pytest
```

Each router has a corresponding test file (`test_auth.py`, `test_reviews.py`,
`test_catalog.py`, `test_feed.py`) using an in-memory SQLite database.

## Deployment

The production instance is deployed at `https://magpieapp.duckdns.org` via Docker.
See `Dockerfile` and `docker-compose.yml` for the containerized setup; the container runs
migrations and seeds sample data before starting Uvicorn.