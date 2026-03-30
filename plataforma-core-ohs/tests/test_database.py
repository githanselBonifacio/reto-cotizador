"""Tests for database connection"""

import pytest
from unittest.mock import AsyncMock, patch
from app.db.connection import MongoDBClient


class TestMongoDBClient:
    """Test MongoDB connection management"""

    @pytest.mark.asyncio
    async def test_connect(self):
        """Test database connection"""
        with patch("motor.motor_asyncio.AsyncIOMotorClient") as mock_client:
            await MongoDBClient.connect()
            assert MongoDBClient.client is not None
            assert MongoDBClient.database is not None

    @pytest.mark.asyncio
    async def test_disconnect(self):
        """Test database disconnection"""
        # Setup
        with patch("motor.motor_asyncio.AsyncIOMotorClient"):
            await MongoDBClient.connect()
            
        # Disconnect
        await MongoDBClient.disconnect()
        assert MongoDBClient.database is None

    def test_get_database_when_connected(self):
        """Test getting database when connected"""
        mock_db = AsyncMock()
        MongoDBClient.database = mock_db

        result = MongoDBClient.get_database()
        assert result == mock_db

    def test_get_database_when_not_connected(self):
        """Test getting database when not connected"""
        MongoDBClient.database = None

        with pytest.raises(RuntimeError) as exc_info:
            MongoDBClient.get_database()
        assert "not connected" in str(exc_info.value)

