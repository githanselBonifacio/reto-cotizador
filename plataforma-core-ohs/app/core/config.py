# App Configuration Module
from typing import Any

from pydantic import Field, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Application settings management using pydantic-settings.
    Loads configuration from .env file and environment variables.
    """
    
    # Application
    app_name: str = Field(default="Plataforma Core OHS", alias="APP_NAME")
    app_version: str = Field(default="1.0.0", alias="APP_VERSION")
    debug: bool = Field(default=False, alias="DEBUG")
    expose_docs: bool | None = Field(default=None, alias="EXPOSE_DOCS")
    cors_origins: list[str] = Field(default_factory=lambda: ["*"], alias="CORS_ORIGINS")
    allowed_hosts: list[str] = Field(default_factory=lambda: ["*"], alias="ALLOWED_HOSTS")
    max_page_size: int = Field(default=100, alias="MAX_PAGE_SIZE")
    
    # MongoDB Configuration
    mongodb_url: str = Field(..., alias="MONGODB_URL")
    database_name: str = Field(..., alias="DATABASE_NAME")
    
    # Security Configuration
    secret_key: str = Field(..., alias="SECRET_KEY")
    api_key: str = Field(..., alias="API_KEY")
    algorithm: str = Field(default="HS256", alias="ALGORITHM")
    access_token_expire_minutes: int = Field(default=30, alias="ACCESS_TOKEN_EXPIRE_MINUTES")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    @field_validator("cors_origins", "allowed_hosts", mode="before")
    @classmethod
    def parse_list_settings(cls, value: Any) -> list[str]:
        """Allow comma-separated strings or JSON-style lists in env settings."""
        if value is None or value == "":
            return ["*"]

        if isinstance(value, list):
            return [str(item).strip() for item in value if str(item).strip()]

        if isinstance(value, str):
            raw_value = value.strip()
            if raw_value.startswith("[") and raw_value.endswith("]"):
                raw_value = raw_value[1:-1]
            items = [item.strip().strip('"\'') for item in raw_value.split(",")]
            return [item for item in items if item]

        return [str(value)]

    @model_validator(mode="after")
    def apply_secure_defaults(self) -> "Settings":
        """Apply environment-aware secure defaults without breaking local development."""
        if self.expose_docs is None:
            self.expose_docs = self.debug

        if not self.debug:
            if self.secret_key in {
                "your-secret-key-change-in-production-min-32-chars-long",
                "your-secret-key-change-in-production-min-32-chars",
                "your-secret-key-change-in-production",
            } or len(self.secret_key) < 32:
                raise ValueError("SECRET_KEY must be strong and at least 32 characters in non-debug environments")

            if self.api_key == "your-api-key-change-in-production" or len(self.api_key) < 24:
                raise ValueError("API_KEY must be strong and at least 24 characters in non-debug environments")

            if self.cors_origins == ["*"]:
                raise ValueError("CORS_ORIGINS cannot be '*' in non-debug environments")

            if self.allowed_hosts == ["*"]:
                raise ValueError("ALLOWED_HOSTS cannot be '*' in non-debug environments")

        return self
    
    @property
    def mongodb_connection_string(self) -> str:
        """Return the complete MongoDB connection string."""
        return self.mongodb_url


# Singleton instance
settings = Settings()  # type: ignore

