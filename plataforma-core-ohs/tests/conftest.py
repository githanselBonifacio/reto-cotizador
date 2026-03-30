"""
Test Configuration and Fixtures
Global fixtures and test setup for the test suite
"""

import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock, patch
from datetime import datetime, timezone
from motor.motor_asyncio import AsyncIOMotorDatabase


@pytest.fixture
def event_loop():
    """Create an event loop for async tests."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def mock_database():
    """Create a mock MongoDB database."""
    db = AsyncMock(spec=AsyncIOMotorDatabase)
    return db


@pytest.fixture
def sample_subscriber():
    """Sample subscriber document."""
    return {
        "_id": "507f1f77bcf86cd799439011",
        "name": "Test Subscriber",
        "email": "test@example.com",
        "phone": "+1-555-0100",
        "status": "active",
        "created_at": datetime.now(timezone.utc),
        "updated_at": datetime.now(timezone.utc),
    }


@pytest.fixture
def sample_agent():
    """Sample agent document."""
    return {
        "_id": "507f1f77bcf86cd799439012",
        "name": "Test Agent",
        "email": "agent@example.com",
        "agent_code": "AG001",
        "subscriber_id": "507f1f77bcf86cd799439011",
        "status": "active",
        "created_at": datetime.now(timezone.utc),
        "updated_at": datetime.now(timezone.utc),
    }


@pytest.fixture
def sample_business_line():
    """Sample business line document."""
    return {
        "_id": "507f1f77bcf86cd799439013",
        "code": "BL001",
        "name": "Fire Insurance",
        "description": "Coverage for fire and related damages",
        "status": "active",
        "created_at": datetime.now(timezone.utc),
    }


@pytest.fixture
def sample_zip_code():
    """Sample ZIP code document."""
    return {
        "_id": "507f1f77bcf86cd799439014",
        "zip_code": "10001",
        "city": "New York",
        "state": "NY",
        "country": "US",
        "risk_zone": "LOW",
        "status": "active",
        "created_at": datetime.now(timezone.utc),
    }


@pytest.fixture
def sample_tariff():
    """Sample tariff document."""
    return {
        "_id": "507f1f77bcf86cd799439015",
        "code": "TARIFF001",
        "name": "Fire Factor - Standard",
        "factor_type": "INCENDIO",
        "base_rate": 0.015,
        "min_rate": 0.010,
        "max_rate": 0.025,
        "business_line_id": None,
        "effective_date": datetime.now(timezone.utc),
        "expiration_date": None,
        "status": "active",
    }


@pytest.fixture
def sample_risk_classification():
    """Sample risk classification document."""
    return {
        "_id": "507f1f77bcf86cd799439016",
        "code": "RC001",
        "name": "Low Risk",
        "description": "Low probability of claims",
        "risk_level": "LOW",
    }


@pytest.fixture
def sample_guarantee():
    """Sample guarantee document."""
    return {
        "_id": "507f1f77bcf86cd799439017",
        "code": "GAR001",
        "name": "Fire Damage",
        "description": "Coverage for fire-related damages",
        "coverage_type": "FIRE",
        "coverage_amount": 1000000,
    }

