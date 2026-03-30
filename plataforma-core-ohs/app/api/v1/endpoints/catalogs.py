# API v1 Catalogs Endpoints
from fastapi import APIRouter, Depends, Query
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.core.config import settings
from app.db.connection import get_database
from app.core.security import verify_api_key
from app.core.serializers import serialize_mongo_documents
from app.services.services import CatalogService
from app.schemas.schemas import (
    GuaranteeResponse,
    RiskClassificationResponse,
    RiskClassificationsListResponse,
    GuaranteesListResponse,
)

router = APIRouter(prefix="/catalogs", tags=["catalogs"])


@router.get(
    "/risk-classification",
    response_model=RiskClassificationsListResponse,
    summary="Get risk classifications catalog",
    description="Retrieve the risk classification catalog with pagination support",
)
async def get_risk_classifications(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=settings.max_page_size),
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get risk classifications catalog."""
    service = CatalogService(database)
    items, total = await service.get_risk_classifications(skip, limit)
    serialized_items = serialize_mongo_documents(items)
    response_items = [RiskClassificationResponse.model_validate(item) for item in serialized_items]
    return RiskClassificationsListResponse(total=total, items=response_items)


@router.get(
    "/guarantees",
    response_model=GuaranteesListResponse,
    summary="Get guarantees catalog",
    description="Retrieve the guarantees catalog with pagination support",
)
async def get_guarantees(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=settings.max_page_size),
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get guarantees catalog."""
    service = CatalogService(database)
    items, total = await service.get_guarantees(skip, limit)
    serialized_items = serialize_mongo_documents(items)
    response_items = [GuaranteeResponse.model_validate(item) for item in serialized_items]
    return GuaranteesListResponse(total=total, items=response_items)

