# API v1 Subscribers Endpoints
from fastapi import APIRouter, Depends, HTTPException, Query, status
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from pymongo.errors import DuplicateKeyError

from app.db.connection import get_database
from app.core.security import verify_api_key
from app.core.config import settings
from app.core.serializers import serialize_mongo_document, serialize_mongo_documents
from app.services.services import SubscriberService
from app.schemas.schemas import (
    SubscriberResponse,
    SubscribersListResponse,
    SubscriberCreate,
    SubscriberUpdate,
)

router = APIRouter(prefix="/subscribers", tags=["subscribers"])
SUBSCRIBER_NOT_FOUND = "Subscriber not found"


@router.get(
    "",
    response_model=SubscribersListResponse,
    summary="List all subscribers",
    description="Retrieve a list of all subscribers with pagination support",
)
async def list_subscribers(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=settings.max_page_size),
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get list of all subscribers."""
    service = SubscriberService(database)
    items, total = await service.get_all_subscribers(skip, limit)
    serialized_items = serialize_mongo_documents(items)
    response_items = [SubscriberResponse.model_validate(item) for item in serialized_items]
    return SubscribersListResponse(total=total, items=response_items)


@router.get(
    "/{subscriber_id}",
    response_model=SubscriberResponse,
    summary="Get subscriber by ID",
)
async def get_subscriber(
    subscriber_id: str,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get a specific subscriber by ID."""
    service = SubscriberService(database)
    subscriber = await service.get_subscriber(subscriber_id)
    
    if not subscriber:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=SUBSCRIBER_NOT_FOUND,
        )
    
    return SubscriberResponse.model_validate(serialize_mongo_document(subscriber))


@router.post(
    "",
    response_model=SubscriberResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new subscriber",
)
async def create_subscriber(
    data: SubscriberCreate,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Create a new subscriber."""
    service = SubscriberService(database)
    try:
        subscriber_id = await service.create_subscriber(data.model_dump())
    except DuplicateKeyError as error:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="A subscriber with this email already exists",
        ) from error

    subscriber = await service.get_subscriber(subscriber_id)
    return SubscriberResponse.model_validate(serialize_mongo_document(subscriber))


@router.put(
    "/{subscriber_id}",
    response_model=SubscriberResponse,
    summary="Update subscriber",
)
async def update_subscriber(
    subscriber_id: str,
    data: SubscriberUpdate,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Update an existing subscriber."""
    service = SubscriberService(database)
    
    existing = await service.get_subscriber(subscriber_id)
    if not existing:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=SUBSCRIBER_NOT_FOUND,
        )
    
    update_data = data.model_dump(exclude_unset=True)
    if update_data:
        await service.update_subscriber(subscriber_id, update_data)
    
    subscriber = await service.get_subscriber(subscriber_id)
    return SubscriberResponse.model_validate(serialize_mongo_document(subscriber))


@router.delete(
    "/{subscriber_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete subscriber",
)
async def delete_subscriber(
    subscriber_id: str,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Delete a subscriber."""
    service = SubscriberService(database)
    
    existing = await service.get_subscriber(subscriber_id)
    if not existing:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=SUBSCRIBER_NOT_FOUND,
        )
    
    await service.delete_subscriber(subscriber_id)

