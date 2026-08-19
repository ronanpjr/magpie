# Magpie

> Rate music, read reviews, and discover what's worth listening to.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![SQLModel](https://img.shields.io/badge/SQLModel-336791?style=for-the-badge&logo=python&logoColor=white)

Magpie is a music review platform built as an academic project. It pairs a native Android
client with a REST API that proxies the Spotify catalog, so users can search for artists,
albums and tracks, rate them from 0 to 5 stars, and share their opinions with the community.

![Magpie](Magpie/app/src/main/magpie-playstore.png)

## Features

- **Music catalog** — search artists, albums and tracks backed by the Spotify Web API
- **Reviews** — rate any album or track from 0 to 5 stars (0.5 increments), with an optional
  text body; edit and delete your own reviews
- **Engagement** — like other users' reviews and comment on them
- **Social graph** — follow other users and browse their profiles, followers and following
- **Personalized feed** — a "For You" feed built from who you follow plus global popular
  reviews
- **Local-first Android experience** — Room caching for offline browsing and a pending-review
  queue that syncs when you're back online
- **Full i18n** — Portuguese (default) and English, zero hardcoded strings
- **JWT authentication** with secure token storage

## Architecture

Magpie is split into two components that talk over a REST contract (the OpenAPI spec):

```
┌────────────────────────────┐         ┌──────────────────────────────┐
│      Android app (Kotlin)  │  HTTPS  │      Backend (FastAPI)        │
│  Jetpack Compose · MVVM    │◄───────►│  SQLModel · JWT · Alembic     │
│  Retrofit · Room · Hilt    │         │  Spotify proxy · cache-aside  │
└────────────────────────────┘         └───────────────┬──────────────┘
                                                       │
                                              ┌────────▼────────┐
                                              │  SQLite / PG     │
                                              │  Spotify Web API │
                                              └─────────────────┘
```

### Android app (`Magpie/`)

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository |
| Networking | Retrofit 2 + OkHttp + Moshi |
| Local DB | Room |
| DI | Hilt |
| Navigation | Navigation Compose (single activity) |
| Image loading | Coil |
| Token storage | EncryptedSharedPreferences |
| State | StateFlow + `collectAsStateWithLifecycle` |

### Backend (`backend/`)

| Component | Technology |
|---|---|
| Framework | FastAPI |
| ORM | SQLModel (SQLAlchemy + Pydantic) |
| Database | SQLite (dev) / PostgreSQL (prod) |
| Migrations | Alembic |
| Auth | JWT (`python-jose`) + bcrypt hashing |
| Spotify | `spotipy` client with cache-aside |
| Tests | `pytest` + FastAPI TestClient |
| Deploy | Docker / Docker Compose |

## Repository layout

```
magpie/
├── Magpie/            # Native Android app (Kotlin, Jetpack Compose)
│   ├── app/
│   │   └── src/
│   │       ├── main/java/com/magpie/magpie/
│   │       │   ├── data/          # Room, Retrofit APIs, repositories
│   │       │   ├── di/            # Hilt modules
│   │       │   ├── navigation/    # Navigation graph and routes
│   │       │   └── ui/            # Theme, components and screens
│   │       ├── main/res/          # Drawables, strings (pt-BR / en)
│   │       └── test/              # Unit + MockWebServer tests
│   └── plan.md                    # Frontend implementation spec
└── backend/           # REST API (FastAPI)
    ├── app/
    │   ├── models/    # SQLModel tables
    │   ├── schemas/   # Pydantic request/response models
    │   ├── routers/   # Auth, users, catalog, reviews, feed
    │   └── services/  # JWT, Spotify proxy, feed assembly
    ├── alembic/       # Database migrations
    ├── scripts/       # Seed scripts
    ├── tests/         # pytest suite
    └── README.md      # API reference and backend docs
```

## Getting started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (for the Android app)
- JDK 11+
- Python 3.11+ (for the backend)

### Run the Android app

1. Open the `Magpie/` folder in Android Studio.
2. Let Gradle sync finish.
3. Run the `app` module on an emulator or a device.

The app talks to the backend through `BuildConfig.BASE_URL`, defined in
`Magpie/app/build.gradle.kts`. It defaults to the production API, so the app works out of
the box. To use a local backend, point `BASE_URL` at your local instance (e.g.
`http://10.0.2.2:8000/` for the Android emulator) and rebuild.

### Run the backend locally

```bash
cd backend

# 1. Create and activate a virtual environment
python -m venv .venv
source .venv/bin/activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Configure environment variables
cp .env.example .env
# edit .env with your values

# 4. Create or update the database
alembic upgrade head

# 5. (Optional) Seed the database with sample data
python -m scripts.seed_users_and_reviews

# 6. Start the API
uvicorn app.main:app --reload
```

The API will be available at `http://localhost:8000`. Interactive Swagger docs are at
`http://localhost:8000/docs`.

### Run the backend with Docker

```bash
cd backend
cp .env.example .env          # edit with your values
docker compose up --build
```

The container runs migrations, seeds sample data, and starts the API on port `8000`.

## Environment variables

The backend reads these values from `.env`:

| Variable | Description |
|---|---|
| `DATABASE_URL` | Database connection string (e.g. `sqlite:///./magpie.db`) |
| `SECRET_KEY` | Secret used to sign JWTs |
| `ALGORITHM` | JWT signing algorithm (default `HS256`) |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | Token lifetime in minutes |
| `SPOTIFY_CLIENT_ID` | Spotify API client ID |
| `SPOTIFY_CLIENT_SECRET` | Spotify API client secret |

## API

The REST API exposes auth, users, catalog, reviews and feed resources. See the
[backend README](backend/README.md) for the full endpoint reference.

A production instance is deployed and browsable at
[https://magpieapp.duckdns.org/docs](https://magpieapp.duckdns.org/docs) — you can run the
app against it without running the backend locally.

## Testing

**Backend**

```bash
cd backend
pytest
```

**Android app**

```bash
cd Magpie
./gradlew test
```

## Production

The backend is deployed and served from `https://magpieapp.duckdns.org`. See
`backend/Dockerfile` and `backend/docker-compose.yml` for the containerized deployment setup.

## Team

- **821620** — Melissa Shimada
- **824387** — Pedro Sakai
- **821626** — Ronan Pereira