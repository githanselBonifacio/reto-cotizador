"""Tests for models/domain"""

import pytest
from datetime import datetime, timezone
from bson import ObjectId
from app.models.domain import (
    PyObjectId,
    Subscriber,
    Agent,
    BusinessLine,
    ZipCode,
    Folio,
    RiskClassification,
    Guarantee,
    Tariff,
)


class TestPyObjectId:
    """Test custom ObjectId type"""

    def test_object_id_serialization(self):
        """Test ObjectId serialization"""
        obj_id = ObjectId()
        py_obj_id = PyObjectId(obj_id)
        assert str(py_obj_id) == str(obj_id)

    def test_object_id_string_init(self):
        """Test ObjectId initialization from string"""
        obj_id_str = "507f1f77bcf86cd799439011"
        py_obj_id = PyObjectId(obj_id_str)
        assert str(py_obj_id) == obj_id_str


class TestSubscriberModel:
    """Test Subscriber domain model"""

    def test_subscriber_creation(self):
        """Test creating subscriber"""
        subscriber = Subscriber(
            id=ObjectId(),
            name="Test",
            email="test@example.com",
            phone="+1-555-0100",
            status="active",
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
        assert subscriber.name == "Test"
        assert subscriber.status == "active"


class TestAgentModel:
    """Test Agent domain model"""

    def test_agent_creation(self):
        """Test creating agent"""
        agent = Agent(
            id=ObjectId(),
            name="Test",
            email="test@example.com",
            agent_code="AG001",
            subscriber_id=str(ObjectId()),
            status="active",
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
        assert agent.agent_code == "AG001"


class TestBusinessLineModel:
    """Test BusinessLine domain model"""

    def test_business_line_creation(self):
        """Test creating business line"""
        bl = BusinessLine(
            id=ObjectId(),
            code="BL001",
            name="Fire Insurance",
            description="Coverage for fire",
            status="active",
            created_at=datetime.now(timezone.utc),
        )
        assert bl.code == "BL001"


class TestZipCodeModel:
    """Test ZipCode domain model"""

    def test_zip_code_creation(self):
        """Test creating ZIP code"""
        zc = ZipCode(
            id=ObjectId(),
            zip_code="10001",
            city="New York",
            state="NY",
            country="US",
            risk_zone="LOW",
            status="active",
            created_at=datetime.now(timezone.utc),
        )
        assert zc.zip_code == "10001"


class TestFolioModel:
    """Test Folio domain model"""

    def test_folio_creation(self):
        """Test creating folio"""
        folio = Folio(
            id=ObjectId(),
            collection_name="folios",
            sequence_value=1,
            prefix="FOL",
        )
        assert folio.sequence_value == 1


class TestRiskClassificationModel:
    """Test RiskClassification domain model"""

    def test_risk_classification_creation(self):
        """Test creating risk classification"""
        rc = RiskClassification(
            id=ObjectId(),
            code="RC001",
            name="Low Risk",
            description="Low probability",
            risk_level="LOW",
        )
        assert rc.risk_level == "LOW"


class TestGuaranteeModel:
    """Test Guarantee domain model"""

    def test_guarantee_creation(self):
        """Test creating guarantee"""
        guarantee = Guarantee(
            id=ObjectId(),
            code="GAR001",
            name="Fire Damage",
            description="Coverage for fire",
            coverage_type="FIRE",
            coverage_amount=1000000,
        )
        assert guarantee.coverage_amount == 1000000


class TestTariffModel:
    """Test Tariff domain model"""

    def test_tariff_creation(self):
        """Test creating tariff"""
        tariff = Tariff(
            id=ObjectId(),
            code="TARIFF001",
            name="Fire Factor",
            factor_type="INCENDIO",
            base_rate=0.015,
            min_rate=0.010,
            max_rate=0.025,
            business_line_id=None,
            effective_date=datetime.now(timezone.utc),
            expiration_date=None,
            status="active",
        )
        assert tariff.factor_type == "INCENDIO"
        assert tariff.base_rate == 0.015

