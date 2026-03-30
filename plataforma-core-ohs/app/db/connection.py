# MongoDB Database Connection Module
import motor.motor_asyncio
from typing import AsyncGenerator, Optional
from app.core.config import settings


class MongoDBClient:
    """
    MongoDB Async Client Manager using Motor driver.
    Handles database connection and lifecycle management.
    """
    
    client: Optional[motor.motor_asyncio.AsyncIOMotorClient] = None
    database: Optional[motor.motor_asyncio.AsyncIOMotorDatabase] = None
    
    @classmethod
    async def connect(cls) -> None:
        """
        Create MongoDB connection pool.
        Called on application startup.
        """
        cls.client = motor.motor_asyncio.AsyncIOMotorClient(
            settings.mongodb_connection_string,
            serverSelectionTimeoutMS=5000,
            connectTimeoutMS=10000,
        )
        cls.database = cls.client[settings.database_name]
        print(f"✓ Connected to MongoDB: {settings.database_name}")
    
    @classmethod
    async def disconnect(cls) -> None:
        """
        Close MongoDB connection.
        Called on application shutdown.
        """
        if cls.client is not None:
            cls.client.close()
            cls.database = None
            print("✓ Disconnected from MongoDB")
    
    @classmethod
    def get_database(cls) -> motor.motor_asyncio.AsyncIOMotorDatabase:
        """
        Get the current database instance.
        
        Returns:
            AsyncDatabase: Motor async database instance
            
        Raises:
            RuntimeError: If database is not connected
        """
        if cls.database is None:
            raise RuntimeError("Database is not connected. Call connect() first.")
        return cls.database


# Dependency for FastAPI
async def get_database() -> AsyncGenerator:
    """
    FastAPI dependency to get database connection.
    
    Yields:
        AsyncDatabase: Motor async database instance
    """
    database = MongoDBClient.get_database()
    yield database

