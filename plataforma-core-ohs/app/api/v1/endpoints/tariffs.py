# API v1 Tariffs Endpoints
from fastapi import APIRouter, Depends, Query
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.db.connection import get_database
from app.core.config import settings
from app.core.security import verify_api_key
from typing import Literal, Optional

from app.core.serializers import serialize_mongo_documents
from app.services.services import TariffService
from app.schemas.schemas import TariffResponse, TariffsListResponse

router = APIRouter(prefix="/tariffs", tags=["tariffs"])


@router.get(
    "",
    response_model=TariffsListResponse,
    summary="Get all tariffs",
    description="Retrieve all calculation factors (Incendio, CAT, FHM) with pagination",
)
async def get_tariffs(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=settings.max_page_size),
    factor_type: Optional[Literal["INCENDIO", "CAT", "FHM"]] = Query(None, description="Filter by factor type: INCENDIO, CAT, FHM"),
    business_line_id: Optional[str] = Query(None, description="Filter by business line ID"),
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """
    Get tariffs with optional filtering.
    
    Query Parameters:
    - factor_type: Filter by INCENDIO, CAT, or FHM
    - business_line_id: Filter by specific business line
    """
    service = TariffService(database)
    
    if factor_type:
        items, total = await service.get_tariffs_by_type(factor_type, skip, limit)
    elif business_line_id:
        items, total = await service.get_tariffs_by_business_line(business_line_id, skip, limit)
    else:
        items, total = await service.get_all_tariffs(skip, limit)
    
    serialized_items = serialize_mongo_documents(items)
    response_items = [TariffResponse.model_validate(item) for item in serialized_items]
    return TariffsListResponse(total=total, items=response_items)

