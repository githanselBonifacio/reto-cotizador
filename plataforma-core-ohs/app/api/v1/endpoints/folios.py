# API v1 Folios Endpoints
from fastapi import APIRouter, Depends, HTTPException, status
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.db.connection import get_database
from app.core.security import verify_api_key
from app.services.services import FolioService
from app.schemas.schemas import FolioResponse

router = APIRouter(prefix="/folios", tags=["folios"])


@router.get(
    "",
    response_model=FolioResponse,
    summary="Get next folio number",
    description="Generate and retrieve the next folio number for document sequencing",
)
async def get_next_folio(
    prefix: str = "FOL",
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get next folio number for Java backend."""
    service = FolioService(database)
    folio_number = await service.get_next_folio(prefix)
    
    # Extract the numeric part for the response
    import re
    match = re.search(r'\d+', folio_number)
    sequence_value = int(match.group()) if match else 0
    
    return FolioResponse(
        folio_number=folio_number,
        sequence_value=sequence_value,
    )

