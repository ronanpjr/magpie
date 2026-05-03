from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str = "sqlite:///./magpie.db"
    secret_key: str = "replace-with-a-long-random-string"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 10080
    spotify_client_id: str | None = None
    spotify_client_secret: str | None = None

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
