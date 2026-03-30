# Request and Response DTOs (Data Transfer Objects)
from pydantic import BaseModel, ConfigDict, EmailStr, Field
from typing import Optional, List
from datetime import datetime, timezone


# Subscriber DTOs
class SubscriberCreate(BaseModel):
    """Request schema for creating a new subscriber."""
    name: str = Field(..., min_length=1, max_length=255)
    email: EmailStr
    phone: str = Field(..., min_length=10, max_length=20)


class SubscriberUpdate(BaseModel):
    """Request schema for updating a subscriber."""
    name: Optional[str] = Field(None, min_length=1, max_length=255)
    email: Optional[EmailStr] = None
    phone: Optional[str] = Field(None, min_length=10, max_length=20)
    status: Optional[str] = None


class SubscriberResponse(BaseModel):
    """Response schema for a subscriber."""
    id: Optional[str] = Field(None, alias="_id")
    name: str
    email: str
    phone: str
    status: str = Field(default="active")
    created_at: datetime
    updated_at: datetime
    
    model_config = ConfigDict(populate_by_name=True)


class SubscribersListResponse(BaseModel):
    """Response schema for subscribers list."""
    total: int
    items: List[SubscriberResponse]


# Agent DTOs
class AgentCreate(BaseModel):
    """Request schema for creating a new agent."""
    name: str = Field(..., min_length=1, max_length=255)
    email: EmailStr
    agent_code: str = Field(..., min_length=1, max_length=50)
    subscriber_id: Optional[str] = None


class AgentUpdate(BaseModel):
    """Request schema for updating an agent."""
    name: Optional[str] = Field(None, min_length=1, max_length=255)
    email: Optional[EmailStr] = None
    agent_code: Optional[str] = None
    status: Optional[str] = None


class AgentResponse(BaseModel):
    """Response schema for an agent."""
    id: Optional[str] = Field(None, alias="_id")
    name: str
    email: str
    agent_code: str
    subscriber_id: Optional[str]
    status: str = Field(default="active")
    created_at: datetime
    updated_at: datetime
    
    model_config = ConfigDict(populate_by_name=True)


class AgentsListResponse(BaseModel):
    """Response schema for agents list."""
    total: int
    items: List[AgentResponse]


# Business Line DTOs
class BusinessLineCreate(BaseModel):
    """Request schema for creating a business line."""
    code: str = Field(..., min_length=1, max_length=50)
    name: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None


class BusinessLineResponse(BaseModel):
    """Response schema for a business line."""
    id: Optional[str] = Field(None, alias="_id")
    code: str
    name: str
    description: Optional[str]
    status: str = Field(default="active")
    created_at: datetime
    
    model_config = ConfigDict(populate_by_name=True)


class BusinessLinesListResponse(BaseModel):
    """Response schema for business lines list."""
    total: int
    items: List[BusinessLineResponse]


# ZIP Code DTOs
class ZipCodeCreate(BaseModel):
    """Request schema for creating a ZIP code."""
    zip_code: str = Field(..., min_length=5, max_length=10)
    city: str = Field(..., min_length=1, max_length=255)
    state: str = Field(..., min_length=2, max_length=50)
    country: str = "US"
    risk_zone: str = Field(..., min_length=1)


class ZipCodeValidateRequest(BaseModel):
    """Request schema for validating a ZIP code."""
    zip_code: str = Field(..., min_length=5, max_length=10)


class ZipCodeResponse(BaseModel):
    """Response schema for a ZIP code."""
    id: Optional[str] = Field(None, alias="_id")
    zip_code: str
    city: str
    state: str
    country: str
    risk_zone: str
    status: str = Field(default="active")
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

    model_config = ConfigDict(populate_by_name=True)


class ZipCodeValidateResponse(BaseModel):
    """Response schema for ZIP code validation."""
    is_valid: bool
    zip_code: str
    city: Optional[str]
    state: Optional[str]
    risk_zone: Optional[str]


# Folio DTOs
class FolioResponse(BaseModel):
    """Response schema for folio sequence."""
    folio_number: str
    sequence_value: int


# Risk Classification DTOs
class RiskClassificationResponse(BaseModel):
    """Response schema for risk classification."""
    id: Optional[str] = Field(None, alias="_id")
    code: str
    name: str
    description: Optional[str]
    risk_level: str
    
    model_config = ConfigDict(populate_by_name=True)


class RiskClassificationsListResponse(BaseModel):
    """Response schema for risk classifications list."""
    total: int
    items: List[RiskClassificationResponse]


# Guarantee DTOs
class GuaranteeResponse(BaseModel):
    """Response schema for a guarantee."""
    id: Optional[str] = Field(None, alias="_id")
    code: str
    name: str
    description: Optional[str]
    coverage_type: str
    coverage_amount: float
    
    model_config = ConfigDict(populate_by_name=True)


class GuaranteesListResponse(BaseModel):
    """Response schema for guarantees list."""
    total: int
    items: List[GuaranteeResponse]


# Tariff DTOs
class TariffResponse(BaseModel):
    """Response schema for a tariff."""
    id: Optional[str] = Field(None, alias="_id")
    code: str
    name: str
    factor_type: str
    base_rate: float
    min_rate: float
    max_rate: float
    business_line_id: Optional[str]
    effective_date: datetime
    expiration_date: Optional[datetime]
    status: str
    
    model_config = ConfigDict(populate_by_name=True)


class TariffsListResponse(BaseModel):
    """Response schema for tariffs list."""
    total: int
    items: List[TariffResponse]


# Error Response
class ErrorResponse(BaseModel):
    """Standard error response schema."""
    error: str
    message: str
    status_code: int

