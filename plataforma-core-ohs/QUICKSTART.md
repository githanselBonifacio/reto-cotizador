# 🚀 Quick Start Guide - Plataforma Core OHS

## Prerequisites

- Python 3.9+
- MongoDB (local or cloud)
- pip package manager

## Installation (5 minutes)

### Step 1: Create Virtual Environment
```bash
# Windows
python -m venv venv
venv\Scripts\activate

# macOS/Linux
python3 -m venv venv
source venv/bin/activate
```

### Step 2: Install Dependencies
```bash
pip install -r requirements.txt
```

### Step 3: Configure Environment
Edit `.env` file:
```env
MONGODB_URL=mongodb://localhost:27017
DATABASE_NAME=CATALOGO_DANOS
SECRET_KEY=your-secret-key-min-32-chars
API_KEY=your-api-key-min-32-chars
DEBUG=False
```

### Step 4: Initialize Database (Optional)
```bash
python scripts/seed_database.py
```

### Step 5: Start Application
```bash
uvicorn main:app --reload
```

## Access the Application

- **API Root**: http://localhost:8000
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **Health Check**: http://localhost:8000/health

## Test API Endpoints

### Using cURL
```bash
# List subscribers
curl -H "X-API-Key: your-api-key" http://localhost:8000/v1/subscribers

# Create subscriber
curl -X POST http://localhost:8000/v1/subscribers \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Company",
    "email": "test@company.com",
    "phone": "+1-555-0123"
  }'
```

### Using test_main.http
Open `test_main.http` in VS Code with REST Client extension:
- Install: REST Client extension
- Click "Send Request" on any endpoint

### Using Swagger UI
- Go to http://localhost:8000/docs
- Click on any endpoint
- Click "Try it out"
- Add API Key header
- Execute

## Docker Setup (Alternative)

```bash
# Start with Docker Compose
docker-compose up -d

# Access application
# http://localhost:8000

# Stop containers
docker-compose down
```

## Project Structure Overview

```
app/
├── core/       → Configuration & Security
├── db/         → Database Connection
├── models/     → Domain Models
├── schemas/    → Request/Response DTOs
├── repository/ → Data Access Layer
├── services/   → Business Logic
└── api/        → REST Endpoints
```

## Common Tasks

### List All Endpoints
```bash
curl http://localhost:8000/docs
```

### Create a Subscriber
```bash
curl -X POST http://localhost:8000/v1/subscribers \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "email": "contact@acme.com",
    "phone": "+1-555-0000"
  }'
```

### Validate a ZIP Code
```bash
curl -X POST http://localhost:8000/v1/zip-codes/validate \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"zip_code": "10001"}'
```

### Get Next Folio Number
```bash
curl -H "X-API-Key: your-api-key" \
  http://localhost:8000/v1/folios?prefix=FOL
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| MONGODB_URL | mongodb://localhost:27017 | MongoDB connection string |
| DATABASE_NAME | CATALOGO_DANOS | Database name |
| SECRET_KEY | - | JWT secret (required) |
| API_KEY | - | API key (required) |
| ALGORITHM | HS256 | JWT algorithm |
| ACCESS_TOKEN_EXPIRE_MINUTES | 30 | Token expiration |
| APP_NAME | Plataforma Core OHS | Application name |
| DEBUG | False | Debug mode |

## Troubleshooting

### MongoDB Connection Error
```
Error: connection failed: No address found for mongodb://localhost:27017
```
**Solution**: Ensure MongoDB is running
```bash
# Windows: Start MongoDB
mongod

# macOS: Using Homebrew
brew services start mongodb-community
```

### Import Error
```
ModuleNotFoundError: No module named 'fastapi'
```
**Solution**: Install dependencies
```bash
pip install -r requirements.txt
```

### API Key Not Recognized
```
{"detail":"API Key is required"}
```
**Solution**: Add X-API-Key header with correct value

## Next Steps

1. ✅ Read [README.md](README.md) for full documentation
2. ✅ Review [ARCHITECTURE.md](ARCHITECTURE.md) for design patterns
3. ✅ Check [DEPLOYMENT.md](DEPLOYMENT.md) for production setup
4. ✅ Explore API with Swagger UI at /docs

## Support

For issues or questions:
1. Check the documentation files
2. Review error messages in application logs
3. Verify MongoDB connection
4. Confirm API Key is correct

## Learning Resources

- FastAPI Docs: https://fastapi.tiangolo.com
- MongoDB Async Motor: https://motor.readthedocs.io
- Pydantic: https://docs.pydantic.dev
- Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html

---

**Happy Coding! 🎉**

