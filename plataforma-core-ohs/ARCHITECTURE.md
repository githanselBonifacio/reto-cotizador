# Project Configuration & Architecture Documentation

## Architecture Overview

Plataforma Core OHS follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                   API Layer (Presentation)                   │
│  FastAPI Routes, Request Handlers, Response Serialization    │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│              Business Logic Layer (Services)                  │
│  Domain Rules, Calculations, Validation, Orchestration       │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│              Data Access Layer (Repository)                   │
│  MongoDB CRUD Operations, Query Building, Caching            │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                 Database Layer (MongoDB)                      │
│  Storage, Indexing, Query Execution, Transactions            │
└─────────────────────────────────────────────────────────────┘
```

## Module Responsibilities

### 1. **app/core/** - Configuration & Security
   - `config.py`: Environment variables and application settings
   - `security.py`: API Key and Bearer token authentication
   
### 2. **app/db/** - Database Connection
   - `connection.py`: Motor async MongoDB client management
   - Lifecycle management (startup/shutdown)

### 3. **app/models/** - Domain Models
   - `domain.py`: Pydantic models representing database documents
   - Type hints and validation

### 4. **app/schemas/** - DTOs (Data Transfer Objects)
   - `schemas.py`: Request and response validation schemas
   - API contract definitions

### 5. **app/repository/** - Data Access Layer
   - `base.py`: BaseRepository with generic CRUD operations
   - `repositories.py`: Specific repository implementations
   - Query building and MongoDB operations

### 6. **app/services/** - Business Logic Layer
   - `services.py`: Business logic and orchestration
   - Calls repositories, applies business rules
   - Transaction coordination

### 7. **app/api/** - Presentation Layer
   - `health.py`: Health check and root endpoints
   - `v1/router.py`: API router aggregator
   - `v1/endpoints/`: Endpoint implementations per resource

## Data Flow

```
Request
   ↓
API Endpoint (Route Handler)
   ↓
Input Validation (Schema)
   ↓
Service Layer (Business Logic)
   ↓
Repository Layer (Data Access)
   ↓
MongoDB (Database)
   ↓
Response (Schema) → API Response
```

## Key Design Patterns Used

### 1. Repository Pattern
- Abstraction of data access logic
- Easy to test and mock
- Database independence

```python
class BaseRepository:
    async def find_by_id(self, id: str)
    async def create(self, data: dict)
    async def update(self, id: str, data: dict)
    async def delete(self, id: str)
```

### 2. Service Layer Pattern
- Business logic encapsulation
- Reusable across endpoints
- Dependency injection

```python
class SubscriberService:
    def __init__(self, database: AsyncDatabase):
        self.repository = SubscriberRepository(database)
    
    async def create_subscriber(self, data: dict):
        # Business logic here
        return await self.repository.create(data)
```

### 3. Dependency Injection
- FastAPI dependencies for database access
- Testability
- Loose coupling

```python
@router.get("/subscribers")
async def list_subscribers(
    database: AsyncDatabase = Depends(get_database),
):
    pass
```

### 4. DTO (Schema) Separation
- Input schemas for requests
- Output schemas for responses
- Validation at entry points

## Security Implementation

### Authentication Methods

#### API Key Authentication
```python
@router.get("/v1/subscribers")
async def list_subscribers(
    api_key: str = Depends(verify_api_key),
):
    # api_key is verified by middleware
    pass
```

#### Bearer Token Authentication (Optional)
```python
@router.get("/v1/subscribers")
async def list_subscribers(
    token: str = Depends(verify_bearer_token),
):
    pass
```

## Error Handling

### Standard Error Response
```json
{
  "error": "Validation Error",
  "message": "Invalid email format",
  "status_code": 422
}
```

### HTTP Status Codes
- `200`: Success
- `201`: Created
- `204`: No Content
- `400`: Bad Request
- `403`: Forbidden (Authentication Failed)
- `404`: Not Found
- `422`: Validation Error
- `500`: Internal Server Error

## Database Indexes

Indexes are automatically created on startup:

```python
# Unique indexes for high-lookup fields
db.subscribers.create_index("email", unique=True)
db.agents.create_index("agent_code", unique=True)
db.business_lines.create_index("code", unique=True)

# Regular indexes for frequent queries
db.folios.create_index("collection_name", unique=True)
```

## Performance Considerations

### Query Optimization
1. **Pagination**: Use `skip` and `limit` parameters
2. **Indexing**: Create indexes on frequently queried fields
3. **Projection**: Return only needed fields
4. **Connection Pooling**: Motor handles this automatically

### Caching Strategy
- Use Redis for frequently accessed data
- Cache tariff and catalog data
- Implement cache invalidation

### Load Balancing
- Multiple worker processes
- Nginx reverse proxy
- Horizontal scaling with Kubernetes

## Testing Strategy

### Unit Tests
Test individual services and repositories in isolation.

### Integration Tests
Test database operations with real MongoDB.

### API Tests
Test endpoints with actual HTTP requests.

Example test file structure:
```
tests/
├── __init__.py
├── conftest.py              # Shared fixtures
├── unit/
│   ├── test_services.py
│   └── test_repositories.py
├── integration/
│   ├── test_subscribers_api.py
│   └── test_agents_api.py
└── fixtures/
    └── sample_data.py
```

## Monitoring & Logging

### Structured Logging
```python
logger.info(f"Subscriber created: {subscriber_id}")
logger.error(f"Database connection failed: {error}")
logger.debug(f"Query executed in {duration}ms")
```

### Health Endpoints
- `/health` - Application health status
- `/docs` - Swagger UI
- `/redoc` - ReDoc documentation

### Metrics to Monitor
- Request count
- Response time
- Error rate
- Database connection pool status
- Memory usage
- CPU usage

## Deployment Architecture

### Development
```
Local Machine
    ↓
MongoDB (Local or Docker)
    ↓
FastAPI (uvicorn with reload)
```

### Production
```
Load Balancer (Nginx/AWS ALB)
    ↓
Multiple FastAPI Instances (Kubernetes/ECS)
    ↓
MongoDB Atlas or Self-hosted MongoDB
    ↓
Persistent Storage (Backups)
```

## Configuration Management

### Environment-Specific Configuration

```python
# Development
DEBUG=True
MONGODB_URL=mongodb://localhost:27017

# Production
DEBUG=False
MONGODB_URL=mongodb+srv://prod-cluster.mongodb.net/

# Staging
DEBUG=False
MONGODB_URL=mongodb+srv://staging-cluster.mongodb.net/
```

### Secrets Management

Use environment variables or external secret management:
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault
- Kubernetes Secrets

## API Versioning Strategy

Current API version: `v1`

Future versions:
- `/v2` - New endpoints or breaking changes
- `/v1` - Deprecated but maintained for backward compatibility

## Database Migration Strategy

For schema changes:
1. Create migration script
2. Test in staging environment
3. Execute during maintenance window
4. Verify data integrity

## Disaster Recovery Plan

1. **Backup Strategy**: Daily automated backups
2. **Recovery Point Objective (RPO)**: 24 hours
3. **Recovery Time Objective (RTO)**: 4 hours
4. **Backup Location**: Different region (cross-region)
5. **Regular Testing**: Quarterly restore tests

## Compliance & Security

- ✅ Data encryption in transit (HTTPS/TLS)
- ✅ Data encryption at rest (MongoDB encryption)
- ✅ API authentication (API Key)
- ✅ Input validation (Pydantic schemas)
- ✅ SQL injection prevention (MongoDB parameterized queries)
- ✅ Rate limiting (Configurable per deployment)
- ✅ CORS configuration (Configurable per environment)
- ✅ Audit logging (Request/response logging)

## Standards & Best Practices

- **Language**: English (variables, comments, docstrings)
- **Code Style**: PEP 8
- **Type Hints**: Full type annotations
- **Async/Await**: Used throughout
- **Documentation**: Comprehensive docstrings
- **Testing**: Unit and integration tests
- **Linting**: Flake8, Black, Pylint
- **API Docs**: Auto-generated by FastAPI

## Future Enhancements

- [ ] WebSocket support for real-time updates
- [ ] GraphQL endpoint
- [ ] Message queue integration (RabbitMQ/Kafka)
- [ ] Microservices architecture
- [ ] Event sourcing
- [ ] Multi-tenancy support
- [ ] Advanced caching strategies
- [ ] Machine learning integration
- [ ] Advanced analytics dashboard

## Related Documentation

- [README.md](README.md) - Project overview and getting started
- [DEPLOYMENT.md](DEPLOYMENT.md) - Production deployment guide
- [API Documentation](http://localhost:8000/docs) - Interactive API docs

