# Health Check Endpoints
from fastapi import APIRouter, status

router = APIRouter(tags=["health"])


@router.get(
    "/health",
    status_code=status.HTTP_200_OK,
    summary="Health check endpoint",
)
async def health_check():
    """
    Health check endpoint for monitoring and load balancing.
    Returns application status.
    """
    return {
        "status": "healthy",
        "message": "Plataforma Core OHS is running",
    }


@router.get(
    "/",
    status_code=status.HTTP_200_OK,
    summary="Root endpoint",
)
async def root():
    """Root endpoint with API information."""
    return {
        "name": "Plataforma Core OHS",
        "version": "1.0.0",
        "description": "Core catalog and quotation platform for OHS",
        "documentation": "/docs",
        "endpoints": {
            "health": "/health",
            "api_v1": "/v1",
            "swagger_ui": "/docs",
            "redoc": "/redoc",
        },
    }

