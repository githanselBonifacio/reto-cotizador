# Subscriber Repository
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase
from app.repository.base import BaseRepository
from typing import Optional, Dict, Any, List, Tuple


class SubscriberRepository(BaseRepository):
    """Repository for subscriber data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "subscribers")
    
    async def find_by_email(self, email: str) -> Optional[Dict[str, Any]]:
        """
        Find a subscriber by email.
        
        Args:
            email: Subscriber email
            
        Returns:
            Optional[Dict]: Subscriber data or None
        """
        return await self.find_one({"email": email})
    
    async def find_active(self, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """
        Find all active subscribers.
        
        Args:
            skip: Number of documents to skip
            limit: Maximum number to return
            
        Returns:
            Tuple of (subscribers list, total count)
        """
        return await self.find({"status": "active"}, skip, limit)


class AgentRepository(BaseRepository):
    """Repository for agent data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "agents")
    
    async def find_by_agent_code(self, agent_code: str) -> Optional[Dict[str, Any]]:
        """
        Find an agent by code.
        
        Args:
            agent_code: Agent code
            
        Returns:
            Optional[Dict]: Agent data or None
        """
        return await self.find_one({"agent_code": agent_code})
    
    async def find_by_subscriber(self, subscriber_id: str, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """
        Find agents by subscriber ID.
        
        Args:
            subscriber_id: Subscriber ID
            skip: Number of documents to skip
            limit: Maximum number to return
            
        Returns:
            Tuple of (agents list, total count)
        """
        return await self.find({"subscriber_id": subscriber_id}, skip, limit)


class BusinessLineRepository(BaseRepository):
    """Repository for business line data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "business_lines")
    
    async def find_by_code(self, code: str) -> Optional[Dict[str, Any]]:
        """
        Find a business line by code.
        
        Args:
            code: Business line code
            
        Returns:
            Optional[Dict]: Business line data or None
        """
        return await self.find_one({"code": code})


class ZipCodeRepository(BaseRepository):
    """Repository for ZIP code data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "zip_codes")
    
    async def find_by_zip_code(self, zip_code: str) -> Optional[Dict[str, Any]]:
        """
        Find a ZIP code entry.
        
        Args:
            zip_code: ZIP code string
            
        Returns:
            Optional[Dict]: ZIP code data or None
        """
        return await self.find_one({"zip_code": zip_code})
    
    async def find_by_city_state(self, city: str, state: str, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """
        Find ZIP codes by city and state.
        
        Args:
            city: City name
            state: State code
            skip: Number of documents to skip
            limit: Maximum number to return
            
        Returns:
            Tuple of (zip codes list, total count)
        """
        return await self.find({"city": city, "state": state}, skip, limit)


class FolioRepository(BaseRepository):
    """Repository for folio (sequence) data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "folios")
    
    async def get_next_sequence(self, collection_name: str, prefix: Optional[str] = None) -> str:
        """
        Get next sequence number for a collection.
        Uses MongoDB atomic increment operation.
        
        Args:
            collection_name: Collection name for the sequence
            prefix: Optional prefix for the folio number
            
        Returns:
            str: Next folio number
        """
        result = await self.collection.find_one_and_update(
            {"collection_name": collection_name},
            {"$inc": {"sequence_value": 1}},
            upsert=True,
            return_document=True
        )
        
        sequence_value = result["sequence_value"]
        if prefix:
            return f"{prefix}{sequence_value:06d}"
        return f"{sequence_value:06d}"


class RiskClassificationRepository(BaseRepository):
    """Repository for risk classification data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "risk_classifications")
    
    async def find_by_code(self, code: str) -> Optional[Dict[str, Any]]:
        """
        Find a risk classification by code.
        
        Args:
            code: Risk classification code
            
        Returns:
            Optional[Dict]: Risk classification data or None
        """
        return await self.find_one({"code": code})


class GuaranteeRepository(BaseRepository):
    """Repository for guarantee data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "guarantees")
    
    async def find_by_code(self, code: str) -> Optional[Dict[str, Any]]:
        """
        Find a guarantee by code.
        
        Args:
            code: Guarantee code
            
        Returns:
            Optional[Dict]: Guarantee data or None
        """
        return await self.find_one({"code": code})


class TariffRepository(BaseRepository):
    """Repository for tariff data access operations."""
    
    def __init__(self, database: AsyncDatabase):
        super().__init__(database, "tariffs")
    
    async def find_by_factor_type(self, factor_type: str, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """
        Find tariffs by factor type (INCENDIO, CAT, FHM).
        
        Args:
            factor_type: Factor type
            skip: Number of documents to skip
            limit: Maximum number to return
            
        Returns:
            Tuple of (tariffs list, total count)
        """
        return await self.find({"factor_type": factor_type, "status": "active"}, skip, limit)
    
    async def find_by_business_line(self, business_line_id: str, skip: int = 0, limit: int = 100) -> Tuple[List[Dict[str, Any]], int]:
        """
        Find tariffs by business line.
        
        Args:
            business_line_id: Business line ID
            skip: Number of documents to skip
            limit: Maximum number to return
            
        Returns:
            Tuple of (tariffs list, total count)
        """
        return await self.find({"business_line_id": business_line_id, "status": "active"}, skip, limit)

