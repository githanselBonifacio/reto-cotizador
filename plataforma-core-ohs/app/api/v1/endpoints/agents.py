# API v1 Agents Endpoints
from fastapi import APIRouter, Depends, HTTPException, Query, status
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.db.connection import get_database
from app.core.config import settings
from app.core.security import verify_api_key
from app.core.serializers import serialize_mongo_document, serialize_mongo_documents
from app.services.services import AgentService
from app.schemas.schemas import (
    AgentResponse,
    AgentsListResponse,
    AgentCreate,
    AgentUpdate,
)

router = APIRouter(prefix="/agents", tags=["agents"])
AGENT_NOT_FOUND = "Agent not found"


@router.get(
    "",
    response_model=AgentsListResponse,
    summary="List all agents",
    description="Retrieve a list of all agents with pagination support",
)
async def list_agents(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=settings.max_page_size),
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get list of all agents."""
    service = AgentService(database)
    items, total = await service.get_all_agents(skip, limit)
    serialized_items = serialize_mongo_documents(items)
    response_items = [AgentResponse.model_validate(item) for item in serialized_items]
    return AgentsListResponse(total=total, items=response_items)


@router.get(
    "/{agent_id}",
    response_model=AgentResponse,
    summary="Get agent by ID",
)
async def get_agent(
    agent_id: str,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Get a specific agent by ID."""
    service = AgentService(database)
    agent = await service.get_agent(agent_id)
    
    if not agent:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=AGENT_NOT_FOUND,
        )
    
    return AgentResponse.model_validate(serialize_mongo_document(agent))


@router.post(
    "",
    response_model=AgentResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new agent",
)
async def create_agent(
    data: AgentCreate,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Create a new agent."""
    service = AgentService(database)
    agent_id = await service.create_agent(data.model_dump())
    agent = await service.get_agent(agent_id)
    return AgentResponse.model_validate(serialize_mongo_document(agent))


@router.put(
    "/{agent_id}",
    response_model=AgentResponse,
    summary="Update agent",
)
async def update_agent(
    agent_id: str,
    data: AgentUpdate,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Update an existing agent."""
    service = AgentService(database)
    
    existing = await service.get_agent(agent_id)
    if not existing:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=AGENT_NOT_FOUND,
        )
    
    update_data = data.model_dump(exclude_unset=True)
    if update_data:
        await service.update_agent(agent_id, update_data)
    
    agent = await service.get_agent(agent_id)
    return AgentResponse.model_validate(serialize_mongo_document(agent))


@router.delete(
    "/{agent_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete agent",
)
async def delete_agent(
    agent_id: str,
    api_key: str = Depends(verify_api_key),
    database: AsyncDatabase = Depends(get_database),
):
    """Delete an agent."""
    service = AgentService(database)
    
    existing = await service.get_agent(agent_id)
    if not existing:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=AGENT_NOT_FOUND,
        )
    
    await service.delete_agent(agent_id)

