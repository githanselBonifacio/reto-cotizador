# API v1 ZIP Codes Endpoints
from fastapi import APIRouter, Depends, HTTPException, status
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.db.connection import get_database
from app.core.security import verify_api_key
from app.core.serializers import serialize_mongo_document
from app.services.services import ZipCodeService
from app.schemas.schemas import (
    ZipCodeResponse,
    ZipCodeValidateRequest,
    ZipCodeValidateResponse,
    ZipCodeCreate,
)

router = APIRouter(prefix="/zip-codes", tags=["zip-codes"])


@router.get(
    "/{zip_code}",
    response_model=ZipCodeResponse,
    summary="Get ZIP code information",
    description="Retrieve detailed information about a specific ZIP code",
)
async def get_zip_code(
    zip_code: str,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get ZIP code information by ZIP code."""
    service = ZipCodeService(database)
    zip_code_info = await service.get_zip_code(zip_code)
    
    if not zip_code_info:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"ZIP code {zip_code} not found",
        )
    
    return ZipCodeResponse.model_validate(serialize_mongo_document(zip_code_info))


@router.post(
    "/validate",
    response_model=ZipCodeValidateResponse,
    summary="Validate ZIP code",
    description="Validate if a ZIP code exists and return its details",
)
async def validate_zip_code(
    request: ZipCodeValidateRequest,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Validate a ZIP code."""
    service = ZipCodeService(database)
    result = await service.validate_zip_code(request.zip_code)
    return result


@router.post(
    "",
    response_model=ZipCodeResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new ZIP code entry",
)
async def create_zip_code(
    data: ZipCodeCreate,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Create a new ZIP code entry."""
    service = ZipCodeService(database)
    await service.create_zip_code(data.model_dump())
    zip_code_info = await service.get_zip_code(data.zip_code)
    return ZipCodeResponse.model_validate(serialize_mongo_document(zip_code_info))

