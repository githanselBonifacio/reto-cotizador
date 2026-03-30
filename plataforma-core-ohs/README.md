# Comprehensive README for Plataforma Core OHS

## 📋 Project Overview

**Plataforma Core OHS** is a production-ready FastAPI application for managing insurance catalogs and quotation data. It follows **Clean Architecture** principles with async/await patterns and MongoDB for data persistence.

### Key Features
- ✅ RESTful API with FastAPI
- ✅ MongoDB Async Driver (Motor)
- ✅ API Key & Bearer Token Security
- ✅ Clean Architecture Implementation
- ✅ PEP 8 Compliant Code
- ✅ Comprehensive Error Handling
- ✅ Automatic API Documentation (Swagger/ReDoc)
- ✅ Database Connection Pooling
- ✅ CORS Support

---

## 🏗️ Project Structure

```
plataforma-core-ohs/
├── app/
│   ├── __init__.py
│   ├── core/                          # Configuration and Security
│   │   ├── __init__.py
│   │   ├── config.py                 # Pydantic settings from .env
│   │   └── security.py               # API Key and Bearer token verification
│   ├── db/                            # Database Connection
│   │   ├── __init__.py
│   │   └── connection.py             # Motor async MongoDB client
│   ├── models/                        # Domain Models
│   │   ├── __init__.py
│   │   └── domain.py                 # Pydantic models for entities
│   ├── schemas/                       # Request/Response DTOs
│   │   ├── __init__.py
│   │   └── schemas.py                # API request/response schemas
│   ├── repository/                    # Data Access Layer
│   │   ├── __init__.py
│   │   ├── base.py                   # BaseRepository with CRUD operations
│   │   └── repositories.py           # Specific repositories
│   ├── services/                      # Business Logic Layer
│   │   ├── __init__.py
│   │   └── services.py               # Business logic services
│   └── api/                           # Presentation Layer
│       ├── __init__.py
│       ├── health.py                 # Health check endpoints
│       └── v1/
│           ├── __init__.py
│           ├── router.py             # V1 API router aggregator
│           └── endpoints/
│               ├── __init__.py
│               ├── subscribers.py    # Subscriber endpoints
│               ├── agents.py         # Agent endpoints
│               ├── business_lines.py # Business line endpoints
│               ├── zip_codes.py      # ZIP code endpoints
│               ├── folios.py         # Folio/sequence endpoints
│               ├── catalogs.py       # Catalog endpoints
│               └── tariffs.py        # Tariff endpoints
├── scripts/
│   ├── __init__.py
│   └── seed_database.py              # Database initialization script
├── main.py                            # Application entry point
├── requirements.txt                   # Python dependencies
├── .env                              # Environment variables (template)
├── .gitignore                        # Git ignore rules
└── README.md                         # This file
```

---

## 🚀 Getting Started

### Prerequisites
- Python 3.9+
- MongoDB 4.0+ (local or cloud instance)
- pip (Python package manager)

### 1. Installation

```bash
# Clone the repository
cd plataforma-core-ohs

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

### 2. Configuration

Create a `.env` file with your settings:

```env
# MongoDB Configuration
MONGODB_URL=mongodb://localhost:27017
DATABASE_NAME=CATALOGO_DANOS

# Security Configuration
SECRET_KEY=your-super-secret-key-at-least-32-characters-long-in-production
API_KEY=your-api-key-change-in-production
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30

# Application Configuration
APP_NAME=Plataforma Core OHS
APP_VERSION=1.0.0
DEBUG=False
```

### 3. Initialize Database

```bash
# Run the seed script to populate sample data
python scripts/seed_database.py
```

### 4. Start the Application

```bash
# Development mode with hot reload
uvicorn main:app --reload

# Production mode
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

The application will be available at `http://localhost:8000`

---

## 📡 API Endpoints

### Health Check
- `GET /` - Root endpoint with API information
- `GET /health` - Health check status

### Subscribers
- `GET /v1/subscribers` - List all subscribers
- `GET /v1/subscribers/{subscriber_id}` - Get subscriber by ID
- `POST /v1/subscribers` - Create new subscriber
- `PUT /v1/subscribers/{subscriber_id}` - Update subscriber
- `DELETE /v1/subscribers/{subscriber_id}` - Delete subscriber

### Agents
- `GET /v1/agents` - List all agents
- `GET /v1/agents/{agent_id}` - Get agent by ID
- `POST /v1/agents` - Create new agent
- `PUT /v1/agents/{agent_id}` - Update agent
- `DELETE /v1/agents/{agent_id}` - Delete agent

### Business Lines
- `GET /v1/business-lines` - List all business lines
- `GET /v1/business-lines/{business_line_id}` - Get business line by ID
- `POST /v1/business-lines` - Create new business line

### ZIP Codes
- `GET /v1/zip-codes/{zip_code}` - Get ZIP code information
- `POST /v1/zip-codes/validate` - Validate ZIP code
- `POST /v1/zip-codes` - Create new ZIP code entry

### Folios
- `GET /v1/folios` - Get next folio number (sequence generator)

### Catalogs
- `GET /v1/catalogs/risk-classification` - Get risk classifications
- `GET /v1/catalogs/guarantees` - Get guarantees

### Tariffs
- `GET /v1/tariffs` - Get all tariffs
- `GET /v1/tariffs?factor_type=INCENDIO` - Get tariffs by factor type
- `GET /v1/tariffs?business_line_id={id}` - Get tariffs by business line

---

## 🔐 Security

### API Key Authentication
All endpoints require an `X-API-Key` header:

```bash
curl -H "X-API-Key: your-api-key" http://localhost:8000/v1/subscribers
```

### Bearer Token Authentication (Optional)
Some endpoints can use Bearer token authentication:

```bash
curl -H "Authorization: Bearer your-token" http://localhost:8000/v1/subscribers
```

---

## 📚 API Documentation

Interactive API documentation is available at:
- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`
- **OpenAPI Schema**: `http://localhost:8000/openapi.json`

---

## 💾 Database Schema

### Collections

#### subscribers
```json
{
  "_id": ObjectId,
  "name": "string",
  "email": "string",
  "phone": "string",
  "status": "active|inactive",
  "created_at": "datetime",
  "updated_at": "datetime"
}
```

#### agents
```json
{
  "_id": ObjectId,
  "name": "string",
  "email": "string",
  "agent_code": "string",
  "subscriber_id": "string|null",
  "status": "active|inactive",
  "created_at": "datetime",
  "updated_at": "datetime"
}
```

#### business_lines
```json
{
  "_id": ObjectId,
  "code": "string",
  "name": "string",
  "description": "string",
  "status": "active|inactive",
  "created_at": "datetime"
}
```

#### zip_codes
```json
{
  "_id": ObjectId,
  "zip_code": "string",
  "city": "string",
  "state": "string",
  "country": "string",
  "risk_zone": "string",
  "status": "active|inactive",
  "created_at": "datetime"
}
```

#### folios
```json
{
  "_id": ObjectId,
  "collection_name": "string",
  "sequence_value": "number",
  "prefix": "string|null"
}
```

#### risk_classifications
```json
{
  "_id": ObjectId,
  "code": "string",
  "name": "string",
  "description": "string",
  "risk_level": "LOW|MEDIUM|HIGH|CRITICAL"
}
```

#### guarantees
```json
{
  "_id": ObjectId,
  "code": "string",
  "name": "string",
  "description": "string",
  "coverage_type": "string",
  "coverage_amount": "number"
}
```

#### tariffs
```json
{
  "_id": ObjectId,
  "code": "string",
  "name": "string",
  "factor_type": "INCENDIO|CAT|FHM",
  "base_rate": "number",
  "min_rate": "number",
  "max_rate": "number",
  "business_line_id": "string|null",
  "effective_date": "datetime",
  "expiration_date": "datetime|null",
  "status": "active|inactive"
}
```

---

## 🧪 Testing

Example requests using curl:

```bash
# Get all subscribers
curl -H "X-API-Key: your-api-key" http://localhost:8000/v1/subscribers

# Create a subscriber
curl -X POST http://localhost:8000/v1/subscribers \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Company",
    "email": "test@company.com",
    "phone": "+1-555-0123"
  }'

# Validate a ZIP code
curl -X POST http://localhost:8000/v1/zip-codes/validate \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"zip_code": "10001"}'

# Get tariffs by factor type
curl -H "X-API-Key: your-api-key" \
  "http://localhost:8000/v1/tariffs?factor_type=INCENDIO"
```

---

## 🛠️ Development

### Code Style
The project follows PEP 8 standards. Use a linter to check code quality:

```bash
# Install linting tools
pip install flake8 black pylint

# Format code with black
black app/ main.py

# Check with flake8
flake8 app/ main.py
```

### Adding New Endpoints

1. Create a new endpoint file in `app/api/v1/endpoints/`
2. Define the router and endpoints
3. Include the router in `app/api/v1/router.py`
4. Add corresponding service in `app/services/services.py`
5. Add repository in `app/repository/repositories.py`
6. Add schemas in `app/schemas/schemas.py`

---

## 📝 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MONGODB_URL` | MongoDB connection string | mongodb://localhost:27017 |
| `DATABASE_NAME` | MongoDB database name | CATALOGO_DANOS |
| `SECRET_KEY` | JWT secret key | - (required) |
| `API_KEY` | API authentication key | - (required) |
| `ALGORITHM` | JWT algorithm | HS256 |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | Token expiration time | 30 |
| `APP_NAME` | Application name | Plataforma Core OHS |
| `APP_VERSION` | Application version | 1.0.0 |
| `DEBUG` | Debug mode | False |

---

## 🐛 Troubleshooting

### MongoDB Connection Issues
- Ensure MongoDB is running: `mongod`
- Check connection string in `.env`
- Verify firewall/network settings

### Import Errors
- Ensure virtual environment is activated
- Run `pip install -r requirements.txt` again
- Check Python version (must be 3.9+)

### API Key Authentication
- Include `X-API-Key` header in all requests
- Verify API key matches configuration in `.env`

---

## 📦 Dependencies

- **fastapi** (0.104.1) - Web framework
- **uvicorn** (0.24.0) - ASGI server
- **motor** (3.3.2) - Async MongoDB driver
- **pydantic** (2.5.0) - Data validation
- **pydantic-settings** (2.1.0) - Settings management
- **python-dotenv** (1.0.0) - Environment variables
- **pymongo** (4.6.0) - MongoDB client
- **python-multipart** (0.0.6) - Multipart form data support

---

## 🔄 Deployment

### Docker (Optional)

Create a `Dockerfile`:

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

Build and run:

```bash
docker build -t plataforma-core-ohs .
docker run -p 8000:8000 --env-file .env plataforma-core-ohs
```

---

## 📄 License

This project is proprietary and confidential.

---

## 👥 Support

For issues or questions, contact the development team.

