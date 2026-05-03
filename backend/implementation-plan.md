# Magpie — Complete Backend Specification
 
> Document intended for an autonomous development agent.
> Read entirely before writing any code.

---

## 1. Overview

REST backend for the Magpie platform, an academic music review app.
Serves an Android client developed by 3 people in parallel.

**Backend responsibilities:**
- User authentication (JWT)
- Reviews, likes, follows CRUD
- Music catalog via proxy with cache (Spotify Web API → local database)
- Personalized user feed
- Serve static and dynamic data with a predictable contract for the mobile team

**What the backend DOES NOT do:**
- Send emails (no real password recovery — returns 200 and logs)
- Push notifications
- Content moderation
- Multi-factor authentication
- Payments or plans

---

## 2. Stack

| Component | Technology |
|---|---|
| Framework | FastAPI |
| ORM | SQLModel (SQLAlchemy + Pydantic wrapper) |
| Database | SQLite in development, PostgreSQL in production |
| Migrations | Alembic |
| Authentication | JWT via `python-jose`, password hashing via `passlib[bcrypt]` |
| Spotify Client | `spotipy` |
| Server | Uvicorn |
| Deploy | Railway (or Render) — free tier |
| Tests | `pytest` + `httpx` (FastAPI TestClient) |

### Dependencies (`requirements.txt`)

```
fastapi
uvicorn[standard]
sqlmodel
alembic
python-jose[cryptography]
passlib[bcrypt]
spotipy
httpx
pytest
python-dotenv
```

---

## 3. Directory Structure

```
magpie-backend/
├── app/
│   ├── main.py                  # FastAPI instance, router registration, CORS
│   ├── config.py                # environment variables via pydantic BaseSettings
│   ├── database.py              # engine, session, get_db dependency
│   ├── models/
│   │   ├── user.py
│   │   ├── review.py
│   │   ├── like.py
│   │   ├── follow.py
│   │   └── catalog.py           # Artist, Album, Track
│   ├── schemas/
│   │   ├── user.py
│   │   ├── review.py
│   │   ├── catalog.py
│   │   └── feed.py
│   ├── routers/
│   │   ├── auth.py
│   │   ├── users.py
│   │   ├── reviews.py
│   │   ├── feed.py
│   │   └── catalog.py
│   ├── services/
│   │   ├── auth_service.py      # JWT logic, hashing
│   │   ├── spotify_service.py   # Spotify client + cache-aside
│   │   └── feed_service.py      # feed assembly logic
│   └── dependencies.py          # get_current_user, etc.
├── alembic/
│   └── versions/
├── tests/
│   ├── test_auth.py
│   ├── test_reviews.py
│   ├── test_catalog.py
│   └── test_feed.py
├── .env.example
├── alembic.ini
├── requirements.txt
└── README.md
```

---

## 4. Environment Variables

`.env` file (do not commit — only `.env.example`):

```env
DATABASE_URL=sqlite:///./magpie.db
SECRET_KEY=replace-with-a-long-random-string
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=10080
SPOTIFY_CLIENT_ID=your_client_id
SPOTIFY_CLIENT_SECRET=your_client_secret
```

`config.py` should use `pydantic_settings.BaseSettings` to load these variables automatically.

---

## 5. Data Models

### 5.1 User

```python
class User(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    username: str = Field(unique=True, index=True)
    email: str = Field(unique=True, index=True)
    hashed_password: str
    display_name: str
    avatar_url: str | None = None
    bio: str | None = None
    created_at: datetime = Field(default_factory=datetime.utcnow)

    reviews: list["Review"] = Relationship(back_populates="author")
    following: list["Follow"] = Relationship(
        back_populates="follower",
        sa_relationship_kwargs={"foreign_keys": "[Follow.follower_id]"}
    )
    followers: list["Follow"] = Relationship(
        back_populates="followed",
        sa_relationship_kwargs={"foreign_keys": "[Follow.followed_id]"}
    )
```

### 5.2 Follow

```python
class Follow(SQLModel, table=True):
    follower_id: int = Field(foreign_key="user.id", primary_key=True)
    followed_id: int = Field(foreign_key="user.id", primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    follower: User = Relationship(
        back_populates="following",
        sa_relationship_kwargs={"foreign_keys": "[Follow.follower_id]"}
    )
    followed: User = Relationship(
        back_populates="followers",
        sa_relationship_kwargs={"foreign_keys": "[Follow.followed_id]"}
    )
```

### 5.3 Artist

```python
class Artist(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    spotify_id: str = Field(unique=True, index=True)
    name: str = Field(index=True)
    image_url: str | None = None
    genres: str | None = None          # Serialized JSON: '["rock", "indie"]'
    cached_at: datetime = Field(default_factory=datetime.utcnow)

    albums: list["Album"] = Relationship(back_populates="artist")
```

### 5.4 Album

```python
class Album(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    spotify_id: str = Field(unique=True, index=True)
    title: str = Field(index=True)
    artist_id: int = Field(foreign_key="artist.id")
    image_url: str | None = None
    release_date: str | None = None    # "2023-06-01"
    album_type: str | None = None      # "album", "single", "compilation"
    cached_at: datetime = Field(default_factory=datetime.utcnow)

    artist: Artist = Relationship(back_populates="albums")
    tracks: list["Track"] = Relationship(back_populates="album")
```

### 5.5 Track

```python
class Track(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    spotify_id: str = Field(unique=True, index=True)
    title: str = Field(index=True)
    album_id: int = Field(foreign_key="album.id")
    duration_ms: int | None = None
    track_number: int | None = None
    preview_url: str | None = None     # 30s Spotify preview URL (when available)
    cached_at: datetime = Field(default_factory=datetime.utcnow)

    album: Album = Relationship(back_populates="tracks")
```

### 5.6 Review

```python
class ReviewTarget(str, Enum):
    track = "track"
    album = "album"

class Review(SQLModel, table=True):
    id: int | None = Field(default=None, primary_key=True)
    author_id: int = Field(foreign_key="user.id")
    target_type: ReviewTarget
    target_id: int                      # local ID of Track or Album
    rating: float = Field(ge=0, le=5)  # 0.0 to 5.0, 0.5 increments
    body: str | None = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    author: User = Relationship(back_populates="reviews")
    likes: list["Like"] = Relationship(back_populates="review")
```

Uniqueness constraint: `(author_id, target_type, target_id)` — a user reviews a work only once.

### 5.7 Like

```python
class Like(SQLModel, table=True):
    user_id: int = Field(foreign_key="user.id", primary_key=True)
    review_id: int = Field(foreign_key="review.id", primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    review: Review = Relationship(back_populates="likes")
```

---

## 6. Schemas (Pydantic)

Each entity must have separate schemas to avoid exposing internal data:

### Pattern per entity:

| Schema | Use |
|---|---|
| `EntityCreate` | POST body |
| `EntityUpdate` | PUT body (all fields Optional) |
| `EntityRead` | API response (never exposes hashed_password) |
| `EntityReadWithRelations` | Expanded response when necessary |

### Example — User:

```python
class UserCreate(SQLModel):
    username: str
    email: str
    password: str
    display_name: str

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
    is_following: bool = False    # calculated relative to the authenticated user
```

### ReviewRead must include:

```python
class ReviewRead(SQLModel):
    id: int
    author: UserRead
    target_type: ReviewTarget
    target_id: int
    target_title: str             # denormalized to avoid joins on the frontend
    target_image_url: str | None
    artist_name: str              # ditto
    rating: float
    body: str | None
    like_count: int
    liked_by_me: bool             # calculated relative to the authenticated user
    created_at: datetime
    updated_at: datetime
```

---

## 7. Endpoints

### 7.1 Auth — `/auth`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/auth/register` | Create account | No |
| POST | `/auth/login` | Return JWT | No |
| POST | `/auth/refresh` | Renew token | No (refresh token in body) |
| POST | `/auth/password-recovery` | Log in and return 200 (mock) | No |

**POST /auth/register**
- Body: `UserCreate`
- Validates uniqueness of `username` and `email` — returns 409 if duplicated
- Hashes password with bcrypt
- Returns `UserRead` + `access_token`

**POST /auth/login**
- Body: `{ "username": str, "password": str }`
- Returns `{ "access_token": str, "token_type": "bearer", "user": UserRead }`

---

### 7.2 Users — `/users`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/users/me` | Authenticated user's profile | Yes |
| PUT | `/users/me` | Update profile | Yes |
| GET | `/users/{id}` | Public profile of another user | Optional |
| GET | `/users/{id}/reviews` | User's reviews | Optional |
| POST | `/users/{id}/follow` | Follow user | Yes |
| DELETE | `/users/{id}/follow` | Unfollow | Yes |
| GET | `/users/{id}/followers` | Followers list | Optional |
| GET | `/users/{id}/following` | Following list | Optional |

**Pagination:** all list endpoints accept `?page=1&limit=20`.

---

### 7.3 Catalog — `/catalog`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/catalog/search` | Search artists/albums/tracks | No |
| GET | `/catalog/artists/{id}` | Artist details | No |
| GET | `/catalog/artists/{id}/albums` | Discography | No |
| GET | `/catalog/albums/{id}` | Album details | No |
| GET | `/catalog/albums/{id}/tracks` | Tracks of an album | No |
| GET | `/catalog/tracks/{id}` | Track details | No |

**GET /catalog/search**

Query params:
- `q: str` (mandatory, minimum 2 chars)
- `type: "artist" | "album" | "track" | "all"` (default: `"all"`)
- `limit: int` (default: 10, max: 50)

Logic (cache-aside):
1. Search local database with `ilike` on `name`/`title`
2. If results < 3, trigger Spotify search, persist, and include in the response
3. Return unified results

Response:
```json
{
  "artists": [...],
  "albums": [...],
  "tracks": [...]
}
```

**Fields for each returned artist:**
`id`, `spotify_id`, `name`, `image_url`, `genres` (list), `avg_rating` (calculated from local reviews)

**Fields for each returned album:**
`id`, `spotify_id`, `title`, `artist_name`, `artist_id`, `image_url`, `release_date`, `album_type`, `avg_rating`, `review_count`

**Fields for each returned track:**
`id`, `spotify_id`, `title`, `artist_name`, `album_title`, `album_id`, `album_image_url`, `duration_ms`, `preview_url`, `avg_rating`, `review_count`

---

### 7.4 Reviews — `/reviews`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/reviews` | Paginated list with filters | Optional |
| POST | `/reviews` | Create review | Yes |
| GET | `/reviews/{id}` | Review detail | Optional |
| PUT | `/reviews/{id}` | Edit review (author only) | Yes |
| DELETE | `/reviews/{id}` | Delete review (author only) | Yes |
| POST | `/reviews/{id}/like` | Like | Yes |
| DELETE | `/reviews/{id}/like` | Unlike | Yes |

**GET /reviews — query params:**
- `target_type: "track" | "album"` (optional)
- `target_id: int` (optional) — reviews of a specific work
- `author_id: int` (optional) — reviews by a specific user
- `page`, `limit`
- `order_by: "recent" | "top"` (default: `"recent"`)

**POST /reviews — body:**
```json
{
  "target_type": "album",
  "target_id": 42,
  "rating": 4.5,
  "body": "Essential work of Brazilian indie rock."
}
```

Validations:
- `target_id` must exist in the database (track or album according to `target_type`)
- Uniqueness: author cannot have an existing review for the same `(target_type, target_id)` — returns 409
- `rating` must be a multiple of 0.5

---

### 7.5 Feed — `/feed`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/feed` | User's personalized feed | Yes |
| GET | `/feed/popular` | Global feed of popular reviews | No |

**GET /feed**

Returns paginated list of `ReviewRead`.

Ordering logic (by priority):
1. Reviews from users the authenticated user follows (last 48h first)
2. Popular reviews of works from the authenticated user's history (by genre)
3. Global popular reviews

`page` and `limit` applied to the unified set.

**GET /feed/popular**
- Query: `?genre=rock&limit=20`
- Does not require authentication — used for logged-out users and the home screen

---

## 8. Authentication and Authorization

### JWT

- `ACCESS_TOKEN_EXPIRE_MINUTES`: 7 days (10080 min) — suitable for academic context, no mandatory refresh
- Payload: `{ "sub": user_id, "exp": timestamp }`
- Expected header: `Authorization: Bearer <token>`

### Dependency `get_current_user`

```python
async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
) -> User:
    # decodes JWT, fetches user from database, returns or raises 401
```

### Dependency `get_optional_user`

Same logic but returns `None` instead of raising 401 — used in endpoints that behave differently for authenticated vs anonymous users (e.g., `liked_by_me` in ReviewRead).

---

## 9. Spotify Service

```python
# services/spotify_service.py

import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

CACHE_TTL_DAYS = 7  # refetch Spotify data after 7 days

def get_spotify_client() -> spotipy.Spotify:
    auth = SpotifyClientCredentials(
        client_id=settings.SPOTIFY_CLIENT_ID,
        client_secret=settings.SPOTIFY_CLIENT_SECRET
    )
    return spotipy.Spotify(auth_manager=auth)

def search_and_cache(q: str, db: Session) -> dict:
    """
    1. Local search (ilike)
    2. If few results, search Spotify
    3. Persist new artists/albums/tracks
    4. Return unified result
    """

def fetch_artist_albums(spotify_id: str, db: Session) -> list[Album]:
    """
    Fetches an artist's albums on Spotify and persists them.
    Called when an artist exists in the database but without cached albums.
    """

def fetch_album_tracks(spotify_id: str, db: Session) -> list[Track]:
    """
    Fetches tracks of an album on Spotify and persists them.
    """
```

**Fields to extract from Spotify and persist:**

From `artist`:
- `id` → `spotify_id`
- `name`
- `images[0].url` → `image_url`
- `genres` → serialize as JSON string

From `album`:
- `id` → `spotify_id`
- `name` → `title`
- `artists[0].id` → resolve to local `artist_id`
- `images[0].url` → `image_url`
- `release_date`
- `album_type`

From `track`:
- `id` → `spotify_id`
- `name` → `title`
- `album.id` → resolve to local `album_id`
- `duration_ms`
- `track_number`
- `preview_url`

**Ignore:** `popularity`, `followers`, `available_markets`, `external_ids` — removed or irrelevant fields.

---

## 10. Error Handling

Use FastAPI's `HTTPException` with the following patterns:

| Situation | Status | Detail |
|---|---|---|
| Invalid or expired token | 401 | `"invalid_token"` |
| Unauthenticated | 401 | `"not_authenticated"` |
| No permission (not the author) | 403 | `"forbidden"` |
| Resource not found | 404 | `"not_found"` |
| Duplicate (review, username) | 409 | `"already_exists"` |
| Field validation | 422 | automatic by FastAPI |
| Spotify API error | 503 | `"catalog_unavailable"` — return local cache data even if outdated |

**Never return stack traces in production.** Use a global exception handler:

```python
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    return JSONResponse(status_code=500, content={"detail": "internal_error"})
```

---

## 11. CORS

Configure to accept any origin in development. The Android client uses the emulator (`10.0.2.2`) or a physical device on the same network.

```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

---

## 12. Pagination

Uniform pattern across all list endpoints:

**Request:** `?page=1&limit=20`

**Response:**
```json
{
  "items": [...],
  "total": 143,
  "page": 1,
  "limit": 20,
  "pages": 8
}
```

Implement as generic schema `Page[T]` with `Generic[T]`.

---

## 13. Tests

Each router must have a corresponding test file. Use FastAPI's `TestClient` with an in-memory SQLite database.

### Minimum expected coverage:

**test_auth.py**
- Register with valid data → 201
- Register with duplicate username → 409
- Correct login → 200 with token
- Login with wrong password → 401
- Access protected route without token → 401

**test_reviews.py**
- Create review with authenticated user → 201
- Create duplicate review → 409
- Edit another user's review → 403
- Delete own review → 204
- Like and unlike → 200

**test_catalog.py**
- Search returns local cache results
- Search with cache miss triggers Spotify mock (monkeypatch in `spotify_service`)
- `GET /catalog/artists/{id}` with non-existent ID → 404

**test_feed.py**
- Authenticated feed prioritizes reviews from followed users
- Popular feed returns without authentication

**Base fixture:**
```python
@pytest.fixture
def client():
    engine = create_engine("sqlite:///:memory:")
    SQLModel.metadata.create_all(engine)
    with TestClient(app) as c:
        yield c

@pytest.fixture
def auth_client(client):
    client.post("/auth/register", json={...})
    resp = client.post("/auth/login", json={...})
    token = resp.json()["access_token"]
    client.headers["Authorization"] = f"Bearer {token}"
    return client
```

---

## 14. Migrations (Alembic)

- `alembic init alembic` at the root
- `env.py` must import all models before the `target_metadata`
- First migration: create all tables
- Never edit already applied migrations — create a new migration for each schema change

---

## 15. Minimum README

The README must contain:

1. Prerequisites (Python 3.11+, environment variables)
2. How to run locally:
   ```bash
   pip install -r requirements.txt
   cp .env.example .env
   # fill .env with Spotify credentials
   alembic upgrade head
   uvicorn app.main:app --reload
   ```
3. Swagger URL: `http://localhost:8000/docs`
4. Production URL (Railway)
5. Table of all endpoints with method, path, and description (copy section 7)

---

## 16. Recommended Implementation Order

Implement in this order to unblock the Android team as quickly as possible:

1. **Project setup** — directory structure, config, database.py, Alembic
2. **Models** — all models at once, run first migration
3. **Auth** — register + login working with JWT
4. **Users** — `/users/me`, `/users/{id}`, follow/unfollow
5. **Basic Catalog** — Artist/Album/Track models, manual seed with 10 artists so frontend is not blocked
6. **Reviews** — full CRUD + likes
7. **Feed** — `/feed` and `/feed/popular`
8. **Spotify service** — replace manual seed with real cache-aside
9. **Tests** — cover all critical cases
10. **Deploy** — Railway + production environment variables

---

## 17. Delivery Contract for the Mobile Team

Upon finishing each step, communicate to the team:

- Production base URL
- Any schema changes compared to this document
- Fields that are still mocks/placeholders

The Swagger at `/docs` is the source of truth for the contracts — the Android team must consult it before implementing any integration