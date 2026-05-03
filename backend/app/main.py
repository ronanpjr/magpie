from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.database import create_db_and_tables
from app.routers import auth, catalog, feed, reviews, users


def create_app() -> FastAPI:
    app = FastAPI(title="Magpie Backend")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        return JSONResponse(status_code=500, content={"detail": "internal_error"})

    app.include_router(auth.router)
    app.include_router(users.router)
    app.include_router(catalog.router)
    app.include_router(reviews.router)
    app.include_router(feed.router)

    @app.on_event("startup")
    def on_startup() -> None:
        create_db_and_tables()

    return app


app = create_app()
