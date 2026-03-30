# Security Module
import secrets
from typing import Optional

from fastapi import HTTPException, status, Depends
from fastapi.security import APIKeyHeader, HTTPBearer, HTTPAuthorizationCredentials

from app.core.config import settings

# API Key Security
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)

# HTTP Bearer Security
http_bearer = HTTPBearer(auto_error=False)


async def verify_api_key(api_key: Optional[str] = Depends(api_key_header)) -> str:
    """
    Verify API Key from header.
    
    Args:
        api_key: API Key from X-API-Key header
        
    Returns:
        str: The verified API key
        
    Raises:
        HTTPException: If API key is invalid or missing
    """
    if api_key is None or not api_key.strip():
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="API Key is required"
        )

    normalized_api_key = api_key.strip()
    if not secrets.compare_digest(normalized_api_key, settings.api_key):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid API Key"
        )

    return normalized_api_key


async def verify_bearer_token(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(http_bearer),
) -> str:
    """
    Verify Bearer token from Authorization header.
    This is useful for OAuth2 Password Bearer authentication.
    
    Args:
        credentials: Bearer token credentials
        
    Returns:
        str: The verified token
        
    Raises:
        HTTPException: If token is invalid or missing
    """
    if not credentials or not credentials.credentials.strip():
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Bearer token is required"
        )

    # In production, validate the token against a database or JWT
    # For now, we do a simple check
    token = credentials.credentials.strip()
    if not secrets.compare_digest(token, settings.secret_key):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid bearer token"
        )

    return token

