"""Tests for repositories"""

import pytest
from unittest.mock import AsyncMock, MagicMock
from bson import ObjectId
from app.repository.base import BaseRepository


class TestBaseRepository:
    """Test BaseRepository CRUD operations"""

    @pytest.mark.asyncio
    async def test_create_document(self, mock_database):
        """Test creating a document"""
        mock_collection = AsyncMock()
        mock_collection.insert_one = AsyncMock()
        mock_collection.insert_one.return_value.inserted_id = ObjectId()
        mock_database.__getitem__.return_value = mock_collection

        repo = BaseRepository(mock_database, "test_collection")
        result = await repo.create({"name": "Test"})

        assert isinstance(result, str)
        mock_collection.insert_one.assert_called_once()

    @pytest.mark.asyncio
    async def test_find_by_id_valid(self, mock_database):
        """Test finding document by ID"""
        mock_collection = AsyncMock()
        test_doc = {"_id": ObjectId("507f1f77bcf86cd799439011"), "name": "Test"}
        mock_collection.find_one = AsyncMock(return_value=test_doc)
        mock_database.__getitem__.return_value = mock_collection

        repo = BaseRepository(mock_database, "test_collection")
        result = await repo.find_by_id("507f1f77bcf86cd799439011")

        assert result["name"] == "Test"
        mock_collection.find_one.assert_called_once()

    @pytest.mark.asyncio
    async def test_find_by_id_invalid(self, mock_database):
        """Test finding document with invalid ID"""
        mock_collection = AsyncMock()
        mock_collection.find_one = AsyncMock(side_effect=Exception("Invalid ID"))
        mock_database.__getitem__.return_value = mock_collection

        repo = BaseRepository(mock_database, "test_collection")
        result = await repo.find_by_id("invalid-id")

        assert result is None

    @pytest.mark.asyncio
    async def test_find_all(self, mock_database):
        """Test finding all documents"""
        test_docs = [
            {"_id": ObjectId(), "name": "Test 1"},
            {"_id": ObjectId(), "name": "Test 2"},
        ]

        mock_collection = MagicMock()
        mock_collection.count_documents = AsyncMock(return_value=2)

        # Mock the cursor chain
        mock_cursor = MagicMock()
        mock_cursor.skip.return_value = mock_cursor
        mock_cursor.limit.return_value = mock_cursor
        mock_cursor.to_list = AsyncMock(return_value=test_docs)

        mock_collection.find.return_value = mock_cursor
        mock_database.__getitem__.return_value = mock_collection

        repo = BaseRepository(mock_database, "test_collection")
        results, total = await repo.find_all(skip=0, limit=100)

        assert len(results) == 2
        assert total == 2

    @pytest.mark.asyncio
    async def test_update_document(self, mock_database):
        """Test updating a document"""
        mock_collection = AsyncMock()
        mock_collection.update_one = AsyncMock()
        mock_collection.update_one.return_value.modified_count = 1
        mock_database.__getitem__.return_value = mock_collection

        repo = BaseRepository(mock_database, "test_collection")
        result = await repo.update(
            "507f1f77bcf86cd799439011", {"name": "Updated"}
        )

        assert result is True
        mock_collection.update_one.assert_called_once()

    @pytest.mark.asyncio
    async def test_delete_document(self, mock_database):
        """Test deleting a document"""
        mock_collection = AsyncMock()
        mock_collection.delete_one = AsyncMock()
        mock_collection.delete_one.return_value.deleted_count = 1
        mock_database.__getitem__.return_value = mock_collection

        repo = BaseRepository(mock_database, "test_collection")
        result = await repo.delete("507f1f77bcf86cd799439011")

        assert result is True
        mock_collection.delete_one.assert_called_once()

    @pytest.mark.asyncio
    async def test_find_with_query(self, mock_database):
        """Test finding documents with query"""
        test_docs = [{"_id": ObjectId(), "status": "active"}]

        mock_collection = MagicMock()
        mock_collection.count_documents = AsyncMock(return_value=1)

        mock_cursor = MagicMock()
        mock_cursor.skip.return_value = mock_cursor
        mock_cursor.limit.return_value = mock_cursor
        mock_cursor.to_list = AsyncMock(return_value=test_docs)

        mock_collection.find.return_value = mock_cursor
        mock_database.__getitem__.return_value = mock_collection

        repo = BaseRepository(mock_database, "test_collection")
        results, total = await repo.find({"status": "active"})

        assert len(results) == 1
        assert total == 1
        mock_collection.find.assert_called_with({"status": "active"})

