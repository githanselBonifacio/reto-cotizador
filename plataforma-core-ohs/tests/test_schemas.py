"""Tests for schemas (DTOs)"""

import pytest
from datetime import datetime, timezone
from pydantic import ValidationError
from app.schemas.schemas import (
    SubscriberCreate,
    SubscriberResponse,
    SubscriberUpdate,
    AgentCreate,
    AgentResponse,
    BusinessLineCreate,
    BusinessLineResponse,
    ZipCodeCreate,
    ZipCodeResponse,
    TariffResponse,
    RiskClassificationResponse,
    GuaranteeResponse,
)


class TestSubscriberSchemas:
    """Test subscriber request and response schemas"""

    def test_subscriber_create_valid(self):
        """Test valid SubscriberCreate schema"""
        data = {
            "name": "Test Subscriber",
            "email": "test@example.com",
            "phone": "+1-555-0100",
        }
        subscriber = SubscriberCreate(**data)
        assert subscriber.name == "Test Subscriber"
        assert subscriber.email == "test@example.com"
        assert subscriber.phone == "+1-555-0100"

    def test_subscriber_create_invalid_email(self):
        """Test SubscriberCreate with invalid email"""
        data = {
            "name": "Test",
            "email": "invalid-email",
            "phone": "+1-555-0100",
        }
        with pytest.raises(ValidationError):
            SubscriberCreate(**data)

    def test_subscriber_create_invalid_phone_length(self):
        """Test SubscriberCreate with invalid phone length"""
        data = {
            "name": "Test",
            "email": "test@example.com",
            "phone": "123",  # Too short
        }
        with pytest.raises(ValidationError):
            SubscriberCreate(**data)

    def test_subscriber_response_valid(self):
        """Test valid SubscriberResponse schema"""
        data = {
            "_id": "507f1f77bcf86cd799439011",
            "name": "Test",
            "email": "test@example.com",
            "phone": "+1-555-0100",
            "status": "active",
            "created_at": datetime.now(timezone.utc),
            "updated_at": datetime.now(timezone.utc),
        }
        response = SubscriberResponse(**data)
        assert response.id == "507f1f77bcf86cd799439011"
        assert response.status == "active"

    def test_subscriber_response_default_status(self):
        """Test SubscriberResponse with missing status (uses default)"""
        data = {
            "_id": "507f1f77bcf86cd799439011",
            "name": "Test",
            "email": "test@example.com",
            "phone": "+1-555-0100",
            "created_at": datetime.now(timezone.utc),
            "updated_at": datetime.now(timezone.utc),
        }
        response = SubscriberResponse(**data)
        assert response.status == "active"  # Default value

    def test_subscriber_update_all_optional(self):
        """Test SubscriberUpdate with all optional fields"""
        data = {}
        update = SubscriberUpdate(**data)
        assert update.name is None
        assert update.email is None
        assert update.phone is None
        assert update.status is None


class TestAgentSchemas:
    """Test agent request and response schemas"""

    def test_agent_create_valid(self):
        """Test valid AgentCreate schema"""
        data = {
            "name": "Test Agent",
            "email": "agent@example.com",
            "agent_code": "AG001",
        }
        agent = AgentCreate(**data)
        assert agent.name == "Test Agent"
        assert agent.agent_code == "AG001"

    def test_agent_response_default_status(self):
        """Test AgentResponse with missing status"""
        data = {
            "_id": "507f1f77bcf86cd799439012",
            "name": "Test Agent",
            "email": "agent@example.com",
            "agent_code": "AG001",
            "subscriber_id": "507f1f77bcf86cd799439011",
            "created_at": datetime.now(timezone.utc),
            "updated_at": datetime.now(timezone.utc),
        }
        response = AgentResponse(**data)
        assert response.status == "active"  # Default value


class TestBusinessLineSchemas:
    """Test business line schemas"""

    def test_business_line_create_valid(self):
        """Test valid BusinessLineCreate schema"""
        data = {
            "code": "BL001",
            "name": "Fire Insurance",
            "description": "Coverage for fire",
        }
        bl = BusinessLineCreate(**data)
        assert bl.code == "BL001"
        assert bl.name == "Fire Insurance"

    def test_business_line_response_default_status(self):
        """Test BusinessLineResponse with missing status"""
        data = {
            "_id": "507f1f77bcf86cd799439013",
            "code": "BL001",
            "name": "Fire Insurance",
            "description": "Coverage for fire",
            "created_at": datetime.now(timezone.utc),
        }
        response = BusinessLineResponse(**data)
        assert response.status == "active"  # Default value


class TestZipCodeSchemas:
    """Test ZIP code schemas"""

    def test_zip_code_create_valid(self):
        """Test valid ZipCodeCreate schema"""
        data = {
            "zip_code": "10001",
            "city": "New York",
            "state": "NY",
            "country": "US",
            "risk_zone": "LOW",
        }
        zc = ZipCodeCreate(**data)
        assert zc.zip_code == "10001"
        assert zc.city == "New York"

    def test_zip_code_response_defaults(self):
        """Test ZipCodeResponse with defaults"""
        data = {
            "_id": "507f1f77bcf86cd799439014",
            "zip_code": "10001",
            "city": "New York",
            "state": "NY",
            "country": "US",
            "risk_zone": "LOW",
        }
        response = ZipCodeResponse(**data)
        assert response.status == "active"
        assert response.created_at is not None


class TestTariffSchemas:
    """Test tariff schemas"""

    def test_tariff_response_valid(self):
        """Test valid TariffResponse schema"""
        data = {
            "_id": "507f1f77bcf86cd799439015",
            "code": "TARIFF001",
            "name": "Fire Factor",
            "factor_type": "INCENDIO",
            "base_rate": 0.015,
            "min_rate": 0.010,
            "max_rate": 0.025,
            "business_line_id": None,
            "effective_date": datetime.now(timezone.utc),
            "expiration_date": None,
            "status": "active",
        }
        response = TariffResponse(**data)
        assert response.code == "TARIFF001"
        assert response.factor_type == "INCENDIO"


class TestCatalogSchemas:
    """Test catalog schemas"""

    def test_risk_classification_response_valid(self):
        """Test valid RiskClassificationResponse"""
        data = {
            "_id": "507f1f77bcf86cd799439016",
            "code": "RC001",
            "name": "Low Risk",
            "description": "Low probability",
            "risk_level": "LOW",
        }
        response = RiskClassificationResponse(**data)
        assert response.code == "RC001"
        assert response.risk_level == "LOW"

    def test_guarantee_response_valid(self):
        """Test valid GuaranteeResponse"""
        data = {
            "_id": "507f1f77bcf86cd799439017",
            "code": "GAR001",
            "name": "Fire Damage",
            "description": "Coverage for fire",
            "coverage_type": "FIRE",
            "coverage_amount": 1000000,
        }
        response = GuaranteeResponse(**data)
        assert response.code == "GAR001"
        assert response.coverage_amount == 1000000

