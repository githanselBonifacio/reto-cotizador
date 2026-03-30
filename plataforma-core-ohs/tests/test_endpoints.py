"""Integration-style unit tests for FastAPI endpoints."""

import sys
from datetime import datetime, timezone
from pathlib import Path
from unittest.mock import AsyncMock, patch

import pytest
from bson import ObjectId
from fastapi.testclient import TestClient
from pymongo.errors import DuplicateKeyError

sys.path.insert(0, str(Path(__file__).parent.parent))
from main import app
from app.core.security import verify_api_key
from app.db.connection import MongoDBClient, get_database


@pytest.fixture
def client_with_overrides():
    """Create a TestClient with auth/database overrides and disabled lifespan DB calls."""

    async def override_verify_api_key():
        return "test-key"

    async def override_get_database():
        yield AsyncMock()

    async def mock_connect():
        return None

    async def mock_disconnect():
        return None

    app.dependency_overrides[verify_api_key] = override_verify_api_key
    app.dependency_overrides[get_database] = override_get_database

    with patch.object(MongoDBClient, "connect", side_effect=mock_connect), patch.object(
        MongoDBClient,
        "disconnect",
        side_effect=mock_disconnect,
    ):
        with TestClient(app) as client:
            yield client

    app.dependency_overrides.clear()


@pytest.fixture
def sample_subscriber_doc():
    now = datetime.now(timezone.utc)
    return {
        "_id": ObjectId("507f1f77bcf86cd799439011"),
        "name": "Test Subscriber",
        "email": "test@example.com",
        "phone": "+1-555-0100",
        "status": "active",
        "created_at": now,
        "updated_at": now,
    }


@pytest.fixture
def sample_agent_doc():
    now = datetime.now(timezone.utc)
    return {
        "_id": ObjectId("507f1f77bcf86cd799439012"),
        "name": "Test Agent",
        "email": "agent@example.com",
        "agent_code": "AG001",
        "subscriber_id": "507f1f77bcf86cd799439011",
        "status": "active",
        "created_at": now,
        "updated_at": now,
    }


@pytest.fixture
def sample_business_line_doc():
    return {
        "_id": ObjectId("507f1f77bcf86cd799439013"),
        "code": "BL001",
        "name": "Fire Insurance",
        "description": "Coverage for fire and related damages",
        "status": "active",
        "created_at": datetime.now(timezone.utc),
    }


@pytest.fixture
def sample_zip_code_doc():
    return {
        "_id": ObjectId("507f1f77bcf86cd799439014"),
        "zip_code": "10001",
        "city": "New York",
        "state": "NY",
        "country": "US",
        "risk_zone": "LOW",
        "status": "active",
        "created_at": datetime.now(timezone.utc),
    }


@pytest.fixture
def sample_guarantee_doc():
    return {
        "_id": ObjectId("507f1f77bcf86cd799439017"),
        "code": "GAR001",
        "name": "Fire Damage",
        "description": "Coverage for fire-related damages",
        "coverage_type": "FIRE",
        "coverage_amount": 1000000.0,
    }


@pytest.fixture
def sample_risk_classification_doc():
    return {
        "_id": ObjectId("507f1f77bcf86cd799439016"),
        "code": "RC001",
        "name": "Low Risk",
        "description": "Low probability of claims",
        "risk_level": "LOW",
    }


@pytest.fixture
def sample_tariff_doc():
    now = datetime.now(timezone.utc)
    return {
        "_id": ObjectId("507f1f77bcf86cd799439015"),
        "code": "TARIFF001",
        "name": "Fire Factor - Standard",
        "factor_type": "INCENDIO",
        "base_rate": 0.015,
        "min_rate": 0.010,
        "max_rate": 0.025,
        "business_line_id": None,
        "effective_date": now,
        "expiration_date": None,
        "status": "active",
    }


class TestHealthEndpoints:
    def test_root_endpoint(self, client_with_overrides):
        response = client_with_overrides.get("/")
        assert response.status_code == 200
        body = response.json()
        assert body["name"] == "Plataforma Core OHS"
        assert body["documentation"] == "/docs"

    def test_health_check_endpoint(self, client_with_overrides):
        response = client_with_overrides.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"


class TestSubscriberEndpoints:
    def test_list_subscribers_success(self, client_with_overrides, sample_subscriber_doc):
        with patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.get_all_subscribers",
            new=AsyncMock(return_value=([sample_subscriber_doc], 1)),
        ):
            response = client_with_overrides.get("/v1/subscribers", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        body = response.json()
        assert body["total"] == 1
        assert body["items"][0]["_id"] == "507f1f77bcf86cd799439011"

    def test_get_subscriber_success(self, client_with_overrides, sample_subscriber_doc):
        with patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.get_subscriber",
            new=AsyncMock(return_value=sample_subscriber_doc),
        ):
            response = client_with_overrides.get(
                "/v1/subscribers/507f1f77bcf86cd799439011",
                headers={"X-API-Key": "test-key"},
            )

        assert response.status_code == 200
        assert response.json()["email"] == "test@example.com"

    def test_get_subscriber_not_found(self, client_with_overrides):
        with patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.get_subscriber",
            new=AsyncMock(return_value=None),
        ):
            response = client_with_overrides.get(
                "/v1/subscribers/507f1f77bcf86cd799439099",
                headers={"X-API-Key": "test-key"},
            )

        assert response.status_code == 404
        assert response.json()["detail"] == "Subscriber not found"

    def test_create_subscriber_success(self, client_with_overrides, sample_subscriber_doc):
        with patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.create_subscriber",
            new=AsyncMock(return_value="507f1f77bcf86cd799439011"),
        ), patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.get_subscriber",
            new=AsyncMock(return_value=sample_subscriber_doc),
        ):
            response = client_with_overrides.post(
                "/v1/subscribers",
                headers={"X-API-Key": "test-key"},
                json={"name": "Test Subscriber", "email": "test@example.com", "phone": "+1-555-0100"},
            )

        assert response.status_code == 201
        assert response.json()["status"] == "active"

    def test_create_subscriber_duplicate_email(self, client_with_overrides):
        with patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.create_subscriber",
            new=AsyncMock(side_effect=DuplicateKeyError("duplicate")),
        ):
            response = client_with_overrides.post(
                "/v1/subscribers",
                headers={"X-API-Key": "test-key"},
                json={"name": "Test", "email": "test@example.com", "phone": "+1-555-0100"},
            )

        assert response.status_code == 409
        assert response.json()["detail"] == "A subscriber with this email already exists"

    def test_update_subscriber_success(self, client_with_overrides, sample_subscriber_doc):
        updated_doc = {**sample_subscriber_doc, "name": "Updated Subscriber"}
        with patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.get_subscriber",
            new=AsyncMock(side_effect=[sample_subscriber_doc, updated_doc]),
        ), patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.update_subscriber",
            new=AsyncMock(return_value=True),
        ):
            response = client_with_overrides.put(
                "/v1/subscribers/507f1f77bcf86cd799439011",
                headers={"X-API-Key": "test-key"},
                json={"name": "Updated Subscriber"},
            )

        assert response.status_code == 200
        assert response.json()["name"] == "Updated Subscriber"

    def test_delete_subscriber_success(self, client_with_overrides, sample_subscriber_doc):
        with patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.get_subscriber",
            new=AsyncMock(return_value=sample_subscriber_doc),
        ), patch(
            "app.api.v1.endpoints.subscribers.SubscriberService.delete_subscriber",
            new=AsyncMock(return_value=True),
        ):
            response = client_with_overrides.delete(
                "/v1/subscribers/507f1f77bcf86cd799439011",
                headers={"X-API-Key": "test-key"},
            )

        assert response.status_code == 204


class TestAgentEndpoints:
    def test_list_agents_success(self, client_with_overrides, sample_agent_doc):
        with patch(
            "app.api.v1.endpoints.agents.AgentService.get_all_agents",
            new=AsyncMock(return_value=([sample_agent_doc], 1)),
        ):
            response = client_with_overrides.get("/v1/agents", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["items"][0]["agent_code"] == "AG001"

    def test_get_agent_not_found(self, client_with_overrides):
        with patch(
            "app.api.v1.endpoints.agents.AgentService.get_agent",
            new=AsyncMock(return_value=None),
        ):
            response = client_with_overrides.get("/v1/agents/unknown", headers={"X-API-Key": "test-key"})

        assert response.status_code == 404

    def test_create_agent_success(self, client_with_overrides, sample_agent_doc):
        with patch(
            "app.api.v1.endpoints.agents.AgentService.create_agent",
            new=AsyncMock(return_value="507f1f77bcf86cd799439012"),
        ), patch(
            "app.api.v1.endpoints.agents.AgentService.get_agent",
            new=AsyncMock(return_value=sample_agent_doc),
        ):
            response = client_with_overrides.post(
                "/v1/agents",
                headers={"X-API-Key": "test-key"},
                json={"name": "Test Agent", "email": "agent@example.com", "agent_code": "AG001", "subscriber_id": "507f1f77bcf86cd799439011"},
            )

        assert response.status_code == 201
        assert response.json()["_id"] == "507f1f77bcf86cd799439012"

    def test_update_agent_success(self, client_with_overrides, sample_agent_doc):
        updated_doc = {**sample_agent_doc, "name": "Updated Agent"}
        with patch(
            "app.api.v1.endpoints.agents.AgentService.get_agent",
            new=AsyncMock(side_effect=[sample_agent_doc, updated_doc]),
        ), patch(
            "app.api.v1.endpoints.agents.AgentService.update_agent",
            new=AsyncMock(return_value=True),
        ):
            response = client_with_overrides.put(
                "/v1/agents/507f1f77bcf86cd799439012",
                headers={"X-API-Key": "test-key"},
                json={"name": "Updated Agent"},
            )

        assert response.status_code == 200
        assert response.json()["name"] == "Updated Agent"

    def test_delete_agent_success(self, client_with_overrides, sample_agent_doc):
        with patch(
            "app.api.v1.endpoints.agents.AgentService.get_agent",
            new=AsyncMock(return_value=sample_agent_doc),
        ), patch(
            "app.api.v1.endpoints.agents.AgentService.delete_agent",
            new=AsyncMock(return_value=True),
        ):
            response = client_with_overrides.delete(
                "/v1/agents/507f1f77bcf86cd799439012",
                headers={"X-API-Key": "test-key"},
            )

        assert response.status_code == 204


class TestBusinessLineEndpoints:
    def test_list_business_lines_success(self, client_with_overrides, sample_business_line_doc):
        with patch(
            "app.api.v1.endpoints.business_lines.BusinessLineService.get_all_business_lines",
            new=AsyncMock(return_value=([sample_business_line_doc], 1)),
        ):
            response = client_with_overrides.get("/v1/business-lines", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["items"][0]["code"] == "BL001"

    def test_get_business_line_not_found(self, client_with_overrides):
        with patch(
            "app.api.v1.endpoints.business_lines.BusinessLineService.get_business_line",
            new=AsyncMock(return_value=None),
        ):
            response = client_with_overrides.get("/v1/business-lines/unknown", headers={"X-API-Key": "test-key"})

        assert response.status_code == 404

    def test_create_business_line_success(self, client_with_overrides, sample_business_line_doc):
        with patch(
            "app.api.v1.endpoints.business_lines.BusinessLineService.get_business_line",
            new=AsyncMock(return_value=sample_business_line_doc),
        ), patch(
            "app.services.services.BusinessLineRepository.create",
            new=AsyncMock(return_value="507f1f77bcf86cd799439013"),
        ):
            response = client_with_overrides.post(
                "/v1/business-lines",
                headers={"X-API-Key": "test-key"},
                json={"code": "BL001", "name": "Fire Insurance", "description": "Coverage for fire"},
            )

        assert response.status_code == 201


class TestZipCodeEndpoints:
    def test_get_zip_code_success(self, client_with_overrides, sample_zip_code_doc):
        with patch(
            "app.api.v1.endpoints.zip_codes.ZipCodeService.get_zip_code",
            new=AsyncMock(return_value=sample_zip_code_doc),
        ):
            response = client_with_overrides.get("/v1/zip-codes/10001", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["zip_code"] == "10001"

    def test_get_zip_code_not_found(self, client_with_overrides):
        with patch(
            "app.api.v1.endpoints.zip_codes.ZipCodeService.get_zip_code",
            new=AsyncMock(return_value=None),
        ):
            response = client_with_overrides.get("/v1/zip-codes/99999", headers={"X-API-Key": "test-key"})

        assert response.status_code == 404

    def test_validate_zip_code_success(self, client_with_overrides):
        with patch(
            "app.api.v1.endpoints.zip_codes.ZipCodeService.validate_zip_code",
            new=AsyncMock(return_value={"is_valid": True, "zip_code": "10001", "city": "New York", "state": "NY", "risk_zone": "LOW"}),
        ):
            response = client_with_overrides.post(
                "/v1/zip-codes/validate",
                headers={"X-API-Key": "test-key"},
                json={"zip_code": "10001"},
            )

        assert response.status_code == 200
        assert response.json()["is_valid"] is True

    def test_create_zip_code_success(self, client_with_overrides, sample_zip_code_doc):
        with patch(
            "app.services.services.ZipCodeRepository.create",
            new=AsyncMock(return_value="507f1f77bcf86cd799439014"),
        ), patch(
            "app.api.v1.endpoints.zip_codes.ZipCodeService.get_zip_code",
            new=AsyncMock(return_value=sample_zip_code_doc),
        ):
            response = client_with_overrides.post(
                "/v1/zip-codes",
                headers={"X-API-Key": "test-key"},
                json={"zip_code": "10001", "city": "New York", "state": "NY", "country": "US", "risk_zone": "LOW"},
            )

        assert response.status_code == 201


class TestFolioEndpoints:
    def test_get_next_folio_success(self, client_with_overrides):
        with patch(
            "app.api.v1.endpoints.folios.FolioService.get_next_folio",
            new=AsyncMock(return_value="FOL000123"),
        ):
            response = client_with_overrides.get("/v1/folios?prefix=FOL", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["folio_number"] == "FOL000123"
        assert response.json()["sequence_value"] == 123


class TestCatalogEndpoints:
    def test_get_risk_classifications_success(self, client_with_overrides, sample_risk_classification_doc):
        with patch(
            "app.api.v1.endpoints.catalogs.CatalogService.get_risk_classifications",
            new=AsyncMock(return_value=([sample_risk_classification_doc], 1)),
        ):
            response = client_with_overrides.get("/v1/catalogs/risk-classification", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["items"][0]["code"] == "RC001"

    def test_get_guarantees_success(self, client_with_overrides, sample_guarantee_doc):
        with patch(
            "app.api.v1.endpoints.catalogs.CatalogService.get_guarantees",
            new=AsyncMock(return_value=([sample_guarantee_doc], 1)),
        ):
            response = client_with_overrides.get("/v1/catalogs/guarantees", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["items"][0]["code"] == "GAR001"


class TestTariffsEndpoints:
    def test_get_all_tariffs_success(self, client_with_overrides, sample_tariff_doc):
        with patch(
            "app.api.v1.endpoints.tariffs.TariffService.get_all_tariffs",
            new=AsyncMock(return_value=([sample_tariff_doc], 1)),
        ):
            response = client_with_overrides.get("/v1/tariffs", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["items"][0]["code"] == "TARIFF001"

    def test_get_tariffs_by_type_success(self, client_with_overrides, sample_tariff_doc):
        with patch(
            "app.api.v1.endpoints.tariffs.TariffService.get_tariffs_by_type",
            new=AsyncMock(return_value=([sample_tariff_doc], 1)),
        ):
            response = client_with_overrides.get("/v1/tariffs?factor_type=INCENDIO", headers={"X-API-Key": "test-key"})

        assert response.status_code == 200
        assert response.json()["items"][0]["factor_type"] == "INCENDIO"

    def test_get_tariffs_by_business_line_success(self, client_with_overrides, sample_tariff_doc):
        with patch(
            "app.api.v1.endpoints.tariffs.TariffService.get_tariffs_by_business_line",
            new=AsyncMock(return_value=([sample_tariff_doc], 1)),
        ):
            response = client_with_overrides.get(
                "/v1/tariffs?business_line_id=507f1f77bcf86cd799439013",
                headers={"X-API-Key": "test-key"},
            )

        assert response.status_code == 200
        assert response.json()["total"] == 1


class TestSecurityCoverage:
    def test_missing_api_key_returns_403(self):
        with patch.object(MongoDBClient, "connect", new=AsyncMock(return_value=None)), patch.object(
            MongoDBClient,
            "disconnect",
            new=AsyncMock(return_value=None),
        ):
            with TestClient(app) as client:
                response = client.get("/v1/subscribers")

        assert response.status_code == 403
        assert response.json()["detail"] == "API Key is required"
