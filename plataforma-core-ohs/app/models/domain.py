# Domain Models Module
from datetime import datetime, timezone
from typing import Optional

from bson import ObjectId
from pydantic import BaseModel, ConfigDict, Field, GetCoreSchemaHandler
from pydantic_core import core_schema


class PyObjectId(ObjectId):
    """
    Custom ObjectId type for MongoDB documents.
    Allows seamless serialization to JSON.
    """
    
    @classmethod
    def __get_pydantic_core_schema__(
        cls,
        source_type,
        handler: GetCoreSchemaHandler,
    ) -> core_schema.CoreSchema:
        return core_schema.no_info_after_validator_function(
            cls.validate,
            core_schema.union_schema(
                [
                    core_schema.is_instance_schema(ObjectId),
                    core_schema.str_schema(),
                ]
            ),
            serialization=core_schema.to_string_ser_schema(),
        )
    
    @classmethod
    def validate(cls, v, info=None):
        if isinstance(v, ObjectId):
            return v
        if not ObjectId.is_valid(v):
            raise ValueError(f"Invalid ObjectId: {v}")
        return ObjectId(v)
    
    def __repr__(self):
        return f"ObjectId('{self}')"


class Subscriber(BaseModel):
    """
    Subscriber domain model.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    name: str
    email: str
    phone: str
    status: str = "active"
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    updated_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)


class Agent(BaseModel):
    """
    Agent domain model.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    name: str
    email: str
    agent_code: str
    subscriber_id: Optional[str] = None
    status: str = "active"
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    updated_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)


class BusinessLine(BaseModel):
    """
    Business Line domain model.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    code: str
    name: str
    description: Optional[str] = None
    status: str = "active"
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)


class ZipCode(BaseModel):
    """
    ZIP Code domain model.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    zip_code: str
    city: str
    state: str
    country: str = "US"
    risk_zone: str
    status: str = "active"
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)


class Folio(BaseModel):
    """
    Folio (Sequence) domain model for generating unique folio numbers.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    collection_name: str
    sequence_value: int = 1
    prefix: Optional[str] = None
    
    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)


class RiskClassification(BaseModel):
    """
    Risk Classification domain model.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    code: str
    name: str
    description: Optional[str] = None
    risk_level: str  # LOW, MEDIUM, HIGH, CRITICAL
    
    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)


class Guarantee(BaseModel):
    """
    Guarantee domain model.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    code: str
    name: str
    description: Optional[str] = None
    coverage_type: str
    coverage_amount: float
    
    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)


class Tariff(BaseModel):
    """
    Tariff domain model for calculation factors.
    """
    id: Optional[PyObjectId] = Field(None, alias="_id")
    code: str
    name: str
    factor_type: str  # INCENDIO, CAT, FHM
    base_rate: float
    min_rate: float
    max_rate: float
    business_line_id: Optional[str] = None
    effective_date: datetime
    expiration_date: Optional[datetime] = None
    status: str = "active"
    
    model_config = ConfigDict(populate_by_name=True, arbitrary_types_allowed=True)

