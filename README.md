# Magpie

Members: 
821620 - Melissa Shimada
824387 - Pedro Sakai
821626 - Ronan Pereira

## Requirements

- Android Studio for the Android app
- JDK 11+
- Python 3.11+ for the backend

## Run the Android app

1. Open the `Magpie/` folder in Android Studio.
2. Let Gradle sync finish.
3. Run the `app` module on an emulator or device.

The app uses `BuildConfig.BASE_URL` to talk to the backend.

## Run the backend

1. Open a terminal in `backend/`.
2. Create and activate a virtual environment.
3. Install dependencies:

```bash
pip install -r requirements.txt
```

4. Create your environment file:

```bash
cp .env.example .env
```

5. Edit `.env` with your values.
6. Create or update the database:

```bash
alembic upgrade head
```

7. Start the API:

```bash
uvicorn app.main:app --reload
```

The API will be available at `http://localhost:8000`.
Swagger docs are at `http://localhost:8000/docs`.

## Backend environment variables

The backend expects these values in `.env`:

- `DATABASE_URL`
- `SECRET_KEY`
- `ALGORITHM`
- `ACCESS_TOKEN_EXPIRE_MINUTES`
- `SPOTIFY_CLIENT_ID`
- `SPOTIFY_CLIENT_SECRET`

## Notes

- The Android manifest allows internet access and cleartext traffic for local development.
- If you run the backend on a different host or port, update the Android app's base URL accordingly.
