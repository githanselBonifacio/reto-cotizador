# MongoDB Collections & Database Seeding Analysis

**Generated:** March 31, 2026  
**Applications Analyzed:** 
- Spring Boot: `cotizador-danos-back`
- FastAPI: `plataforma-core-ohs`

---

## Table of Contents
1. [FastAPI Collections (Core OHS Platform)](#fastapi-collections)
2. [Spring Boot Collections](#spring-boot-collections)
3. [FastAPI Seeding Script](#fastapi-seeding-script)
4. [Spring Boot Journal Collections](#spring-boot-journal-collections)
5. [Data Dependencies & Relationships](#data-dependencies--relationships)
6. [Seeding Strategy](#seeding-strategy)

---

## FastAPI Collections

**Database Name:** `PLATAFORMA_DANOS` (configurable via `DATABASE_NAME` env var)  
**Connection:** MongoDB Atlas/Local (via `MONGODB_URI` env var)

### 1. **subscribers**
**Purpose:** Insurance subscriber organizations/companies  
**Unique Index:** `email`  
**Seed Count:** 2 records  
**Data Structure:**
```
{
  "_id": ObjectId,
  "name": str,              # Organization name
  "email": str,             # (UNIQUE)
  "phone": str,             # Contact phone
  "status": str,            # "active" / "inactive"
  "created_at": datetime,   # UTC timestamp
  "updated_at": datetime    # UTC timestamp
}
```

**Sample Seed Data:**
- Acme Insurance Corp (contact@acme-ins.com, +1-555-0101)
- Global Risk Management (info@globalrisk.com, +1-555-0102)

---

### 2. **agents**
**Purpose:** Insurance agents/brokers linked to subscribers  
**Unique Index:** `agent_code`  
**Seed Count:** 2 records  
**Data Structure:**
```
{
  "_id": ObjectId,
  "name": str,              # Agent full name
  "email": str,
  "agent_code": str,        # (UNIQUE) e.g., "AG001", "AG002"
  "subscriber_id": str,     # Reference to subscriber._id (as string)
  "status": str,            # "active" / "inactive"
  "created_at": datetime,
  "updated_at": datetime
}
```

**Sample Seed Data:**
- John Carter (AG001) → linked to Acme Insurance Corp
- Laura Mitchell (AG002) → linked to Global Risk Management

---

### 3. **business_lines**
**Purpose:** Insurance product line types (Fire, CAT, etc.)  
**Unique Index:** `code`  
**Seed Count:** 3 records  
**Data Structure:**
```
{
  "_id": ObjectId,
  "code": str,              # (UNIQUE) e.g., "BL001"
  "name": str,              # e.g., "Fire Insurance"
  "description": str,       # Coverage details
  "status": str,            # "active" / "inactive"
  "created_at": datetime
}
```

**Sample Seed Data:**
- BL001: Fire Insurance - "Coverage for fire and related damages"
- BL002: Catastrophe Coverage - "Protection against natural disasters"
- BL003: All Risks Coverage - "Comprehensive coverage for multiple risk types"

---

### 4. **zip_codes**
**Purpose:** Geographic/postal code information with risk zones  
**Unique Index:** `zip_code`  
**Seed Count:** 2 records  
**Data Structure:**
```
{
  "_id": ObjectId,
  "zip_code": str,          # (UNIQUE) e.g., "01000", "64000"
  "city": str,              # City name (e.g., "Mexico City", "Monterrey")
  "state": str,             # State code (e.g., "CDMX", "NL")
  "country": str,           # Country code (e.g., "MX")
  "risk_zone": str,         # Risk classification: "LOW", "MEDIUM", "HIGH"
  "status": str,            # "active" / "inactive"
  "created_at": datetime
}
```

**Sample Seed Data:**
- 01000 (Mexico City) - LOW risk zone
- 64000 (Monterrey) - MEDIUM risk zone

---

### 5. **folios**
**Purpose:** Sequence generator for quote/folio numbers  
**Unique Index:** `collection_name`  
**Seed Count:** 1 record  
**Data Structure:**
```
{
  "_id": ObjectId,
  "collection_name": str,   # (UNIQUE) Generator name (e.g., "folios")
  "sequence_value": int,    # Current sequence counter
  "prefix": str             # Prefix for generated numbers (e.g., "FOL")
}
```

**Sample Seed Data:**
- Folio generator: prefix="FOL", starting sequence=0

---

### 6. **risk_classifications**
**Purpose:** Risk level/probability classifications  
**Unique Index:** `code`  
**Seed Count:** 4 records  
**Data Structure:**
```
{
  "_id": ObjectId,
  "code": str,              # (UNIQUE) e.g., "RC001"
  "name": str,              # Display name
  "description": str,       # Risk description
  "risk_level": str         # "LOW", "MEDIUM", "HIGH", "CRITICAL"
}
```

**Sample Seed Data:**
- RC001: Low Risk - "Low probability of claims"
- RC002: Medium Risk - "Medium probability of claims"
- RC003: High Risk - "High probability of claims"
- RC004: Critical Risk - "Very high probability of claims"

---

### 7. **guarantees**
**Purpose:** Coverage/guarantee types offered  
**Unique Index:** `code`  
**Seed Count:** 3 records  
**Data Structure:**
```
{
  "_id": ObjectId,
  "code": str,              # (UNIQUE) e.g., "GAR001"
  "name": str,              # Guarantee name
  "description": str,       # Coverage details
  "coverage_type": str,     # e.g., "FIRE", "THEFT", "NATURAL_DISASTER"
  "coverage_amount": float  # Max coverage amount in currency
}
```

**Sample Seed Data:**
- GAR001: Fire Damage - $1,000,000 coverage
- GAR002: Theft Coverage - $500,000 coverage
- GAR003: Natural Disaster - $2,000,000 coverage

---

### 8. **tariffs**
**Purpose:** Rate/factor calculations for premium quotes  
**Unique Index:** `code`  
**Seed Count:** 3 records  
**Data Structure:**
```
{
  "_id": ObjectId,
  "code": str,              # (UNIQUE) e.g., "TARIFF001"
  "name": str,              # Display name
  "factor_type": str,       # "INCENDIO", "CAT", "FHM"
  "base_rate": float,       # Base calculation rate
  "min_rate": float,        # Minimum rate cap
  "max_rate": float,        # Maximum rate cap
  "business_line_id": str,  # Optional reference to business_lines
  "effective_date": datetime,
  "expiration_date": datetime,
  "status": str             # "active" / "inactive"
}
```

**Sample Seed Data:**
- TARIFF001: Fire Factor (INCENDIO) - Base: 0.015 (1.5%)
- TARIFF002: CAT Factor (CAT) - Base: 0.005 (0.5%)
- TARIFF003: FHM Factor (FHM) - Base: 0.008 (0.8%)

---

## Spring Boot Collections

**Application:** `cotizador-danos-back`  
**Database Name:** `PLATAFORMA_DANOS` (same MongoDB database as FastAPI)  
**Configuration:** `src/main/resources/application-local.yml`

### **quotes**
**Purpose:** Insurance quotes/cotizaciones generated by users  
**Location:** `src/main/java/com/cotizador/cotizador_danos_back/infrastructure/adapters/nosqlrepository/CotizacionDocument.java`  
**Primary Key:** `numeroFolio` (BSON @Id)  
**Data Structure:**
```
{
  "_id": string,                      # numeroFolio (e.g., "FOL-000001")
  "estadoCotizacion": string,         # Quote status
  "datosAsegurado": DatosAsegurado,   # Insured party data
  "datosConduccion": DatosConduccion, # Conduct/behavior data
  "locations": [Ubicacion],           # List of risk locations
  "configuracionLayout": Map,         # UI/layout configuration
  "opcionesCobertura": [string],      # Selected coverage options
  "primaNeta": BigDecimal,            # Net premium
  "primaComercial": BigDecimal,       # Commercial premium
  "primasPorUbicacion": [PrimaPorUbicacion], # Premium breakdown
  "version": long,                    # Optimistic locking version
  "fechaUltimaActualizacion": LocalDateTime
}
```

**Key Relationships:**
- READS from FastAPI `/v1/subscribers`, `/v1/agents`, `/v1/business-lines`
- READS from FastAPI `/v1/zip-codes/{zipCode}` for location validation
- READS from FastAPI `/v1/folios` to generate quote numbers
- READS from FastAPI `/v1/tariffs` for rate calculations

---

## FastAPI Seeding Script

**Location:** `scripts/seed_database.py`  
**Type:** Asynchronous Python script using Motor (async MongoDB driver)  
**Idempotent:** YES - Uses upsert operations with unique field filters

### Execution Options

**Option 1: Direct Python Execution**
```bash
cd plataforma-core-ohs
python scripts/seed_database.py
```

**Option 2: From Virtual Environment**
```bash
cd plataforma-core-ohs
.venv/Scripts/python scripts/seed_database.py
```

**Option 3: With Conda (if using Conda environment)**
```bash
conda activate <env_name>
python scripts/seed_database.py
```

### Script Workflow

1. **Database Connection**
   - Connects to MongoDB via `settings.mongodb_connection_string`
   - Sends PING command for connection verification

2. **Database Existence Check**
   - Checks if `PLATAFORMA_DANOS` exists
   - Creates automatically with first collection insertion

3. **Collection Initialization**
   - Creates 8 collections if they don't exist:
     - subscribers, agents, business_lines, zip_codes, folios
     - risk_classifications, guarantees, tariffs

4. **Legacy Cleanup**
   - Removes old `_db_init` temporary collection if empty

5. **Index Creation**
   - Creates unique indexes on:
     - `subscribers.email`
     - `agents.agent_code`
     - `business_lines.code`
     - `zip_codes.zip_code`
     - `folios.collection_name`
     - `risk_classifications.code`
     - `guarantees.code`
     - `tariffs.code`

6. **Data Seeding** (Upsert pattern - no duplicates if re-run)
   - Inserts/updates all 8 collection types
   - Uses unique field as upsert filter key
   - Preserves existing `created_at` timestamps on updates

7. **Error Handling**
   - Catches and prints all exceptions
   - Closes client connection in finally block

### Configuration Requirements

**Environment Variables** (in `.env` file):
```bash
MONGODB_URL=mongodb://localhost:27017          # or MongoDB Atlas URL
DATABASE_NAME=PLATAFORMA_DANOS
DEBUG=True
EXPOSE_DOCS=True
CORS_ORIGINS=["*"]
ALLOWED_HOSTS=["*"]
SECRET_KEY=your-secret-key-change-in-production-min-32-chars-long
API_KEY=your-api-key-change-in-production
```

---

## Spring Boot Journal Collections

**Note:** Spring Boot creates automatic journal collections for change tracking:
- `quotes.system.profile`
- Internal MongoDB system collections

**No explicit seeding needed** - Spring Boot only persists user-generated quotes. Catalog data comes from FastAPI APIs.

---

## Data Dependencies & Relationships

### Functional Dependencies

```
Spring Boot Application (cotizador-danos-back)
    ↓
    ├─→ FastAPI /v1/subscribers        → subscribers collection
    ├─→ FastAPI /v1/agents             → agents collection  
    ├─→ FastAPI /v1/business-lines     → business_lines collection
    ├─→ FastAPI /v1/zip-codes          → zip_codes collection
    ├─→ FastAPI /v1/folios             → folios collection (sequence generator)
    ├─→ FastAPI /v1/tariffs            → tariffs collection
    └─→ FastAPI /v1/zip-codes/validate → zip_codes collection query
```

### Document References

**agents → subscribers:**
- Field: `subscriber_id` (stores `subscribers._id` as string)
- Operation: Set during seeding; maintained by application

**tariffs → business_lines:**
- Field: `business_line_id` (optional reference)
- Operation: Can be null or reference `business_lines._id`

---

## Seeding Strategy

### Recommended Seeding Order

1. **Start MongoDB** (local or Atlas)
   ```bash
   # Local: MongoDB must be running on localhost:27017
   # Or via Docker: docker compose up mongodb
   ```

2. **Run FastAPI Seeding**
   ```bash
   cd plataforma-core-ohs
   python scripts/seed_database.py
   ```

3. **Verify Data Creation**
   ```bash
   # Using MongoDB CLI or Compass
   use PLATAFORMA_DANOS
   db.subscribers.find().pretty()
   db.tariffs.find().pretty()
   # etc...
   ```

4. **Start Applications**
   ```bash
   # Terminal 1: FastAPI
   cd plataforma-core-ohs
   python -m uvicorn main:app --reload

   # Terminal 2: Spring Boot
   cd cotizador-danos-back/cotizador-danos-back
   ./gradlew bootRun
   ```

### Re-seeding Behavior

- **Script is idempotent** ✓
- Running `seed_database.py` multiple times is SAFE
- Existing records are updated (not duplicated)
- Only new fields/records are inserted

### Troubleshooting Seeds

| Issue | Cause | Solution |
|-------|-------|----------|
| Connection refused | MongoDB not running | Start MongoDB first |
| Database not created | Normal behavior | First collection auto-creates it |
| Duplicate key error | Indexes not created | Run seed script again |
| Agents have null subscriber_id | Subscribers not seeded first | Ensure correct seeding order |
| Tariffs not returned by API | Rate limiting or Z timeout | Increase timeout in Spring Boot config |

---

## Configuration Files Reference

### FastAPI Configuration
- **Location:** `plataforma-core-ohs/app/core/config.py`
- **Settings Class:** `Settings` (BaseSettings with pydantic-settings)
- **Key Fields:** `mongodb_url`, `database_name`, `api_key`, `secret_key`

### Spring Boot Configuration
- **Default:** `cotizador-danos-back/cotizador-danos-back/src/main/resources/application.yml`
- **Local:** `application-local.yml`
  ```yaml
  spring:
    data:
      mongodb:
        uri: mongodb://localhost:27017/PLATAFORMA_DANOS
  ```
- **Production:** `application-prod.yml` (uses env vars)

---

## Summary Statistics

| Aspect | Count |
|--------|-------|
| **FastAPI Collections** | 8 |
| **Spring Boot Collections** | 1 (quotes) + system journals |
| **Seed Records (FastAPI)** | 14+ total |
| **Unique Indexes Created** | 8 |
| **Seeding Scripts Available** | 1 (seed_database.py) |
| **API Endpoints (FastAPI)** | 7 (v1 endpoints) |
| **External API Calls (Spring Boot)** | 7+ integration points |

