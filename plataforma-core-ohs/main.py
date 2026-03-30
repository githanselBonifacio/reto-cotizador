"""
Plataforma Core OHS - Main Application Entry Point

This is a production-ready FastAPI application following Clean Architecture principles.
The application provides REST APIs for managing catalogs and quotation data.

Architecture Layers:
- app/core/: Configuration and security
- app/db/: Database connection management
- app/models/: Domain models
- app/schemas/: Request/Response DTOs
- app/repository/: Data access layer
- app/services/: Business logic layer
- app/api/: Presentation layer (REST endpoints)
"""

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.db.connection import MongoDBClient
from app.api import health
from app.api.v1.router import router as v1_router

logger = logging.getLogger(__name__)


# Lifespan events handler
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan context manager.
    Handles startup and shutdown events.
    """
    # Startup
    logger.info("Starting Plataforma Core OHS")
    try:
        await MongoDBClient.connect()
        logger.info("Application started successfully")
    except Exception:
        logger.exception("Failed to start application")
        raise
    
    yield
    
    # Shutdown
    logger.info("Shutting down Plataforma Core OHS")
    try:
        await MongoDBClient.disconnect()
        logger.info("Application shutdown completed")
    except Exception:
        logger.exception("Error during shutdown")


# Create FastAPI application
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Core catalog and quotation platform for Occupational Health and Safety (OHS)",
    docs_url="/docs" if settings.expose_docs else None,
    redoc_url="/redoc" if settings.expose_docs else None,
    openapi_url="/openapi.json" if settings.expose_docs else None,
    lifespan=lifespan,
)

app.add_middleware(
    TrustedHostMiddleware,
    allowed_hosts=settings.allowed_hosts,
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=settings.cors_origins != ["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def add_security_headers(request: Request, call_next):
    """Add basic security headers to every response."""
    response = await call_next(request)
    response.headers.setdefault("X-Content-Type-Options", "nosniff")
    response.headers.setdefault("X-Frame-Options", "DENY")
    response.headers.setdefault("Referrer-Policy", "no-referrer")
    response.headers.setdefault("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
    response.headers.setdefault("Cache-Control", "no-store")
    if request.url.scheme == "https":
        response.headers.setdefault("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
    return response

# Include routers
app.include_router(health.router)
app.include_router(v1_router)

# Global exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Handle all unhandled exceptions."""
    logger.exception("Unhandled exception while processing %s %s", request.method, request.url.path)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal Server Error",
            "message": str(exc) if settings.debug else "An unexpected error occurred",
        },
    )


if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "main:app",
        host="127.0.0.1",
        port=8000,
        reload=settings.debug,
        log_level="info",
    )
