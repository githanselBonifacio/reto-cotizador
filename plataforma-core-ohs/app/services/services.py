# Business Logic Services
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.repository.repositories import (
    SubscriberRepository,
    AgentRepository,
    BusinessLineRepository,
    ZipCodeRepository,
    FolioRepository,
    RiskClassificationRepository,
    GuaranteeRepository,
    TariffRepository,
)
from typing import Optional, Dict, Any, List, Tuple
from datetime import datetime, timezone


class SubscriberService:
    """Business logic for subscriber management."""
    
    def __init__(self, database: AsyncDatabase):
        self.repository = SubscriberRepository(database)
    
    async def get_all_subscribers(self, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get all subscribers."""
        return await self.repository.find_all(skip, limit)
    
    async def get_subscriber(self, subscriber_id: str) -> Optional[Dict[str, Any]]:
        """Get a subscriber by ID."""
        return await self.repository.find_by_id(subscriber_id)
    
    async def create_subscriber(self, data: Dict[str, Any]) -> str:
        """Create a new subscriber."""
        data.setdefault("status", "active")
        data["created_at"] = datetime.now(timezone.utc)
        data["updated_at"] = datetime.now(timezone.utc)
        return await self.repository.create(data)
    
    async def update_subscriber(self, subscriber_id: str, data: Dict[str, Any]) -> bool:
        """Update a subscriber."""
        data["updated_at"] = datetime.now(timezone.utc)
        return await self.repository.update(subscriber_id, data)
    
    async def delete_subscriber(self, subscriber_id: str) -> bool:
        """Delete a subscriber."""
        return await self.repository.delete(subscriber_id)


class AgentService:
    """Business logic for agent management."""
    
    def __init__(self, database: AsyncDatabase):
        self.repository = AgentRepository(database)
    
    async def get_all_agents(self, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get all agents."""
        return await self.repository.find_all(skip, limit)
    
    async def get_agent(self, agent_id: str) -> Optional[Dict[str, Any]]:
        """Get an agent by ID."""
        return await self.repository.find_by_id(agent_id)
    
    async def create_agent(self, data: Dict[str, Any]) -> str:
        """Create a new agent."""
        data.setdefault("status", "active")
        data["created_at"] = datetime.now(timezone.utc)
        data["updated_at"] = datetime.now(timezone.utc)
        return await self.repository.create(data)
    
    async def update_agent(self, agent_id: str, data: Dict[str, Any]) -> bool:
        """Update an agent."""
        data["updated_at"] = datetime.now(timezone.utc)
        return await self.repository.update(agent_id, data)
    
    async def delete_agent(self, agent_id: str) -> bool:
        """Delete an agent."""
        return await self.repository.delete(agent_id)


class BusinessLineService:
    """Business logic for business line management."""
    
    def __init__(self, database: AsyncDatabase):
        self.repository = BusinessLineRepository(database)
    
    async def get_all_business_lines(self, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get all business lines."""
        return await self.repository.find_all(skip, limit)
    
    async def get_business_line(self, business_line_id: str) -> Optional[Dict[str, Any]]:
        """Get a business line by ID."""
        return await self.repository.find_by_id(business_line_id)

    async def create_business_line(self, data: Dict[str, Any]) -> str:
        """Create a new business line."""
        data.setdefault("status", "active")
        data.setdefault("created_at", datetime.now(timezone.utc))
        return await self.repository.create(data)


class ZipCodeService:
    """Business logic for ZIP code management."""
    
    def __init__(self, database: AsyncDatabase):
        self.repository = ZipCodeRepository(database)
    
    async def get_zip_code(self, zip_code: str) -> Optional[Dict[str, Any]]:
        """Get ZIP code information."""
        return await self.repository.find_by_zip_code(zip_code)

    async def create_zip_code(self, data: Dict[str, Any]) -> str:
        """Create a new ZIP code entry."""
        data.setdefault("status", "active")
        data.setdefault("created_at", datetime.now(timezone.utc))
        return await self.repository.create(data)
    
    async def validate_zip_code(self, zip_code: str) -> Dict[str, Any]:
        """Validate if a ZIP code exists and return details."""
        entry = await self.repository.find_by_zip_code(zip_code)
        
        if entry:
            return {
                "is_valid": True,
                "zip_code": entry.get("zip_code"),
                "city": entry.get("city"),
                "state": entry.get("state"),
                "risk_zone": entry.get("risk_zone"),
            }
        
        return {
            "is_valid": False,
            "zip_code": zip_code,
            "city": None,
            "state": None,
            "risk_zone": None,
        }


class FolioService:
    """Business logic for folio (sequence) generation."""
    
    def __init__(self, database: AsyncDatabase):
        self.repository = FolioRepository(database)
    
    async def get_next_folio(self, prefix: Optional[str] = None) -> str:
        """Get next folio number."""
        return await self.repository.get_next_sequence("folios", prefix)


class CatalogService:
    """Business logic for catalog management."""
    
    def __init__(self, database: AsyncDatabase):
        self.risk_repo = RiskClassificationRepository(database)
        self.guarantee_repo = GuaranteeRepository(database)
    
    async def get_risk_classifications(self, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get all risk classifications."""
        return await self.risk_repo.find_all(skip, limit)
    
    async def get_guarantees(self, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get all guarantees."""
        return await self.guarantee_repo.find_all(skip, limit)


class TariffService:
    """Business logic for tariff management."""
    
    def __init__(self, database: AsyncDatabase):
        self.repository = TariffRepository(database)
    
    async def get_all_tariffs(self, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get all tariffs."""
        return await self.repository.find_all(skip, limit)
    
    async def get_tariffs_by_type(self, factor_type: str, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get tariffs by factor type (INCENDIO, CAT, FHM)."""
        return await self.repository.find_by_factor_type(factor_type, skip, limit)
    
    async def get_tariffs_by_business_line(self, business_line_id: str, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """Get tariffs by business line."""
        return await self.repository.find_by_business_line(business_line_id, skip, limit)

