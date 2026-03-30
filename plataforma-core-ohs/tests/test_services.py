"""Tests for services (business logic)"""

import pytest
from datetime import datetime, timezone
from unittest.mock import AsyncMock, MagicMock
from bson import ObjectId
from app.services.services import (
    SubscriberService,
    AgentService,
    BusinessLineService,
    ZipCodeService,
    FolioService,
    CatalogService,
    TariffService,
)


class TestSubscriberService:
    """Test SubscriberService business logic"""

    @pytest.mark.asyncio
    async def test_get_all_subscribers(self, mock_database, sample_subscriber):
        """Test getting all subscribers"""
        mock_collection = AsyncMock()
        mock_collection.find_all = AsyncMock(
            return_value=([sample_subscriber], 1)
        )
        mock_database.__getitem__.return_value = mock_collection

        service = SubscriberService(mock_database)
        service.repository.find_all = AsyncMock(
            return_value=([sample_subscriber], 1)
        )

        items, total = await service.get_all_subscribers()
        assert total == 1
        assert len(items) == 1

    @pytest.mark.asyncio
    async def test_get_subscriber_by_id(self, mock_database, sample_subscriber):
        """Test getting subscriber by ID"""
        service = SubscriberService(mock_database)
        service.repository.find_by_id = AsyncMock(
            return_value=sample_subscriber
        )

        result = await service.get_subscriber("507f1f77bcf86cd799439011")
        assert result["email"] == "test@example.com"

    @pytest.mark.asyncio
    async def test_create_subscriber(self, mock_database):
        """Test creating subscriber"""
        service = SubscriberService(mock_database)
        service.repository.create = AsyncMock(
            return_value="507f1f77bcf86cd799439011"
        )

        data = {
            "name": "Test Subscriber",
            "email": "test@example.com",
            "phone": "+1-555-0100",
        }
        result = await service.create_subscriber(data)

        assert result == "507f1f77bcf86cd799439011"
        # Verify defaults were set
        assert data["status"] == "active"
        assert "created_at" in data
        assert "updated_at" in data

    @pytest.mark.asyncio
    async def test_update_subscriber(self, mock_database):
        """Test updating subscriber"""
        service = SubscriberService(mock_database)
        service.repository.update = AsyncMock(return_value=True)

        data = {"name": "Updated Name"}
        result = await service.update_subscriber(
            "507f1f77bcf86cd799439011", data
        )

        assert result is True
        assert "updated_at" in data

    @pytest.mark.asyncio
    async def test_delete_subscriber(self, mock_database):
        """Test deleting subscriber"""
        service = SubscriberService(mock_database)
        service.repository.delete = AsyncMock(return_value=True)

        result = await service.delete_subscriber(
            "507f1f77bcf86cd799439011"
        )
        assert result is True


class TestAgentService:
    """Test AgentService business logic"""

    @pytest.mark.asyncio
    async def test_get_all_agents(self, mock_database, sample_agent):
        """Test getting all agents"""
        service = AgentService(mock_database)
        service.repository.find_all = AsyncMock(
            return_value=([sample_agent], 1)
        )

        items, total = await service.get_all_agents()
        assert total == 1
        assert len(items) == 1

    @pytest.mark.asyncio
    async def test_create_agent(self, mock_database):
        """Test creating agent"""
        service = AgentService(mock_database)
        service.repository.create = AsyncMock(
            return_value="507f1f77bcf86cd799439012"
        )

        data = {
            "name": "Test Agent",
            "email": "agent@example.com",
            "agent_code": "AG001",
        }
        result = await service.create_agent(data)

        assert result == "507f1f77bcf86cd799439012"
        assert data["status"] == "active"
        assert "created_at" in data


class TestBusinessLineService:
    """Test BusinessLineService business logic"""

    @pytest.mark.asyncio
    async def test_get_all_business_lines(
        self, mock_database, sample_business_line
    ):
        """Test getting all business lines"""
        service = BusinessLineService(mock_database)
        service.repository.find_all = AsyncMock(
            return_value=([sample_business_line], 1)
        )

        items, total = await service.get_all_business_lines()
        assert total == 1
        assert len(items) == 1

    @pytest.mark.asyncio
    async def test_get_business_line_by_id(
        self, mock_database, sample_business_line
    ):
        """Test getting business line by ID"""
        service = BusinessLineService(mock_database)
        service.repository.find_by_id = AsyncMock(
            return_value=sample_business_line
        )

        result = await service.get_business_line(
            "507f1f77bcf86cd799439013"
        )
        assert result["code"] == "BL001"

    @pytest.mark.asyncio
    async def test_create_business_line(self, mock_database):
        """Test creating business line"""
        service = BusinessLineService(mock_database)
        service.repository.create = AsyncMock(
            return_value="507f1f77bcf86cd799439013"
        )

        data = {
            "code": "BL001",
            "name": "Fire Insurance",
            "description": "Coverage for fire",
        }
        result = await service.create_business_line(data)

        assert result == "507f1f77bcf86cd799439013"
        assert data["status"] == "active"
        assert "created_at" in data


class TestZipCodeService:
    """Test ZipCodeService business logic"""

    @pytest.mark.asyncio
    async def test_get_zip_code(self, mock_database, sample_zip_code):
        """Test getting ZIP code"""
        service = ZipCodeService(mock_database)
        service.repository.find_by_zip_code = AsyncMock(
            return_value=sample_zip_code
        )

        result = await service.get_zip_code("10001")
        assert result["city"] == "New York"

    @pytest.mark.asyncio
    async def test_validate_zip_code_exists(
        self, mock_database, sample_zip_code
    ):
        """Test validating existing ZIP code"""
        service = ZipCodeService(mock_database)
        service.repository.find_by_zip_code = AsyncMock(
            return_value=sample_zip_code
        )

        result = await service.validate_zip_code("10001")
        assert result["is_valid"] is True
        assert result["city"] == "New York"

    @pytest.mark.asyncio
    async def test_validate_zip_code_not_exists(self, mock_database):
        """Test validating non-existing ZIP code"""
        service = ZipCodeService(mock_database)
        service.repository.find_by_zip_code = AsyncMock(return_value=None)

        result = await service.validate_zip_code("99999")
        assert result["is_valid"] is False
        assert result["city"] is None

    @pytest.mark.asyncio
    async def test_create_zip_code(self, mock_database):
        """Test creating ZIP code"""
        service = ZipCodeService(mock_database)
        service.repository.create = AsyncMock(
            return_value="507f1f77bcf86cd799439014"
        )

        data = {
            "zip_code": "10001",
            "city": "New York",
            "state": "NY",
            "country": "US",
            "risk_zone": "LOW",
        }
        result = await service.create_zip_code(data)

        assert result == "507f1f77bcf86cd799439014"
        assert data["status"] == "active"


class TestFolioService:
    """Test FolioService business logic"""

    @pytest.mark.asyncio
    async def test_get_next_folio(self, mock_database):
        """Test getting next folio"""
        service = FolioService(mock_database)
        service.repository.get_next_sequence = AsyncMock(
            return_value="000001"
        )

        result = await service.get_next_folio()
        assert result == "000001"

    @pytest.mark.asyncio
    async def test_get_next_folio_with_prefix(self, mock_database):
        """Test getting next folio with prefix"""
        service = FolioService(mock_database)
        service.repository.get_next_sequence = AsyncMock(
            return_value="FOL000001"
        )

        result = await service.get_next_folio(prefix="FOL")
        assert result == "FOL000001"


class TestCatalogService:
    """Test CatalogService business logic"""

    @pytest.mark.asyncio
    async def test_get_risk_classifications(
        self, mock_database, sample_risk_classification
    ):
        """Test getting risk classifications"""
        service = CatalogService(mock_database)
        service.risk_repo.find_all = AsyncMock(
            return_value=([sample_risk_classification], 1)
        )

        items, total = await service.get_risk_classifications()
        assert total == 1
        assert items[0]["code"] == "RC001"

    @pytest.mark.asyncio
    async def test_get_guarantees(self, mock_database, sample_guarantee):
        """Test getting guarantees"""
        service = CatalogService(mock_database)
        service.guarantee_repo.find_all = AsyncMock(
            return_value=([sample_guarantee], 1)
        )

        items, total = await service.get_guarantees()
        assert total == 1
        assert items[0]["code"] == "GAR001"


class TestTariffService:
    """Test TariffService business logic"""

    @pytest.mark.asyncio
    async def test_get_all_tariffs(self, mock_database, sample_tariff):
        """Test getting all tariffs"""
        service = TariffService(mock_database)
        service.repository.find_all = AsyncMock(
            return_value=([sample_tariff], 1)
        )

        items, total = await service.get_all_tariffs()
        assert total == 1
        assert items[0]["code"] == "TARIFF001"

    @pytest.mark.asyncio
    async def test_get_tariffs_by_type(self, mock_database, sample_tariff):
        """Test getting tariffs by type"""
        service = TariffService(mock_database)
        service.repository.find_by_factor_type = AsyncMock(
            return_value=([sample_tariff], 1)
        )

        items, total = await service.get_tariffs_by_type("INCENDIO")
        assert total == 1
        assert items[0]["factor_type"] == "INCENDIO"

    @pytest.mark.asyncio
    async def test_get_tariffs_by_business_line(
        self, mock_database, sample_tariff
    ):
        """Test getting tariffs by business line"""
        service = TariffService(mock_database)
        service.repository.find_by_business_line = AsyncMock(
            return_value=([sample_tariff], 1)
        )

        items, total = await service.get_tariffs_by_business_line(
            "507f1f77bcf86cd799439013"
        )
        assert total == 1

