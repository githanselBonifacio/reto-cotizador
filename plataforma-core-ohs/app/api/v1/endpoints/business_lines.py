# API v1 Business Lines Endpoints
from fastapi import APIRouter, Depends, HTTPException, Query, status
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.db.connection import get_database
from app.core.config import settings
from app.core.security import verify_api_key
from app.core.serializers import serialize_mongo_document, serialize_mongo_documents
from app.services.services import BusinessLineService
from app.schemas.schemas import (
    BusinessLineResponse,
    BusinessLinesListResponse,
    BusinessLineCreate,
)

router = APIRouter(prefix="/business-lines", tags=["business-lines"])
BUSINESS_LINE_NOT_FOUND = "Business line not found"


@router.get(
    "",
    response_model=BusinessLinesListResponse,
    summary="List all business lines",
    description="Retrieve a list of all business lines with pagination support",
)
async def list_business_lines(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=settings.max_page_size),
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get list of all business lines."""
    service = BusinessLineService(database)
    items, total = await service.get_all_business_lines(skip, limit)
    serialized_items = serialize_mongo_documents(items)
    response_items = [BusinessLineResponse.model_validate(item) for item in serialized_items]
    return BusinessLinesListResponse(total=total, items=response_items)


@router.get(
    "/{business_line_id}",
    response_model=BusinessLineResponse,
    summary="Get business line by ID",
)
async def get_business_line(
    business_line_id: str,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get a specific business line by ID."""
    service = BusinessLineService(database)
    business_line = await service.get_business_line(business_line_id)
    
    if not business_line:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=BUSINESS_LINE_NOT_FOUND,
        )
    
    return BusinessLineResponse.model_validate(serialize_mongo_document(business_line))


@router.post(
    "",
    response_model=BusinessLineResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new business line",
)
async def create_business_line(
    data: BusinessLineCreate,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Create a new business line."""
    service = BusinessLineService(database)
    business_line_id = await service.create_business_line(data.model_dump())
    business_line = await service.get_business_line(business_line_id)
    return BusinessLineResponse.model_validate(serialize_mongo_document(business_line))

