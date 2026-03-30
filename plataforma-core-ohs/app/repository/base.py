# Base Repository Pattern
from typing import List, Optional, Dict, Any, TypeVar, Generic
from motor.motor_asyncio import AsyncIOMotorDatabase as AsyncDatabase, AsyncIOMotorCollection as AsyncCollection
from pydantic import BaseModel
from bson import ObjectId

from app.core.config import settings

T = TypeVar("T", bound=BaseModel)


class BaseRepository(Generic[T]):
    """
    Base repository class providing common CRUD operations
    for MongoDB collections using Motor (async driver).
    """
    
    def __init__(self, database: AsyncDatabase, collection_name: str):
        """
        Initialize repository.
        
        Args:
            database: Motor AsyncDatabase instance
            collection_name: Name of the MongoDB collection
        """
        self.database = database
        self.collection: AsyncCollection = database[collection_name]
        self.collection_name = collection_name

    @staticmethod
    def _sanitize_pagination(skip: int, limit: int) -> tuple[int, int]:
        """Clamp pagination inputs to safe values."""
        safe_skip = max(skip, 0)
        safe_limit = max(1, min(limit, settings.max_page_size))
        return safe_skip, safe_limit
    
    async def create(self, document: Dict[str, Any]) -> str:
        """
        Create a new document.
        
        Args:
            document: Dictionary containing document data
            
        Returns:
            str: ID of the created document
        """
        result = await self.collection.insert_one(document)
        return str(result.inserted_id)
    
    async def find_by_id(self, document_id: str) -> Optional[Dict[str, Any]]:
        """
        Find a document by ID.
        
        Args:
            document_id: Document ID
            
        Returns:
            Optional[Dict]: Document data or None if not found
        """
        try:
            return await self.collection.find_one({"_id": ObjectId(document_id)})
        except Exception:
            return None
    
    async def find_all(self, skip: int = 0, limit: int = 100) -> tuple[List[Dict[str, Any]], int]:
        """
        Find all documents with pagination.
        
        Args:
            skip: Number of documents to skip
            limit: Maximum number of documents to return
            
        Returns:
            Tuple of (documents list, total count)
        """
        safe_skip, safe_limit = self._sanitize_pagination(skip, limit)
        total = await self.collection.count_documents({})
        documents = await self.collection.find({}).skip(safe_skip).limit(safe_limit).to_list(safe_limit)
        return documents, total
    
    async def find(self, query: Dict[str, Any], skip: int = 0, limit: int = 100) -> tuple[List[Dict[str, Any]], int]:
        """
        Find documents matching query with pagination.
        
        Args:
            query: MongoDB query filter
            skip: Number of documents to skip
            limit: Maximum number of documents to return
            
        Returns:
            Tuple of (documents list, total count)
        """
        safe_skip, safe_limit = self._sanitize_pagination(skip, limit)
        total = await self.collection.count_documents(query)
        documents = await self.collection.find(query).skip(safe_skip).limit(safe_limit).to_list(safe_limit)
        return documents, total
    
    async def find_one(self, query: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        Find a single document matching query.
        
        Args:
            query: MongoDB query filter
            
        Returns:
            Optional[Dict]: Document data or None
        """
        return await self.collection.find_one(query)
    
    async def update(self, document_id: str, update_data: Dict[str, Any]) -> bool:
        """
        Update a document by ID.
        
        Args:
            document_id: Document ID
            update_data: Data to update
            
        Returns:
            bool: True if updated, False if not found
        """
        try:
            result = await self.collection.update_one(
                {"_id": ObjectId(document_id)},
                {"$set": update_data}
            )
            return result.modified_count > 0
        except Exception:
            return False
    
    async def delete(self, document_id: str) -> bool:
        """
        Delete a document by ID.
        
        Args:
            document_id: Document ID
            
        Returns:
            bool: True if deleted, False if not found
        """
        try:
            result = await self.collection.delete_one({"_id": ObjectId(document_id)})
            return result.deleted_count > 0
        except Exception:
            return False
    
    async def delete_many(self, query: Dict[str, Any]) -> int:
        """
        Delete multiple documents.
        
        Args:
            query: MongoDB query filter
            
        Returns:
            int: Number of deleted documents
        """
        result = await self.collection.delete_many(query)
        return result.deleted_count

