# Production Deployment Guide for Plataforma Core OHS

## 📋 Table of Contents
1. [Pre-deployment Checklist](#pre-deployment-checklist)
2. [Environment Configuration](#environment-configuration)
3. [Database Setup](#database-setup)
4. [Security Considerations](#security-considerations)
5. [Deployment Methods](#deployment-methods)
6. [Monitoring & Logging](#monitoring--logging)
7. [Scaling & Performance](#scaling--performance)

---

## Pre-deployment Checklist

- [ ] All environment variables configured in `.env`
- [ ] MongoDB instance is accessible
- [ ] Secret keys and API keys are strong (minimum 32 characters)
- [ ] CORS settings are properly configured
- [ ] SSL/TLS certificates are in place (if using HTTPS)
- [ ] Database backups are configured
- [ ] Monitoring and logging are set up
- [ ] API documentation is updated
- [ ] Rate limiting is configured
- [ ] Error handling is tested

---

## Environment Configuration

### Production .env Template

```env
# MongoDB Configuration
MONGODB_URL=mongodb+srv://username:password@cluster.mongodb.net/?retryWrites=true&w=majority
DATABASE_NAME=CATALOGO_DANOS

# Security Configuration
SECRET_KEY=generate-with-openssl-rand-base64-32-characters-minimum-very-secure
API_KEY=your-super-secure-api-key-minimum-32-characters-very-complex
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=60

# Application Configuration
APP_NAME=Plataforma Core OHS
APP_VERSION=1.0.0
DEBUG=False

# Server Configuration
WORKERS=4
TIMEOUT=120
```

### Generate Secure Keys

```bash
# Generate SECRET_KEY
python -c "import secrets; print(secrets.token_urlsafe(32))"

# Generate API_KEY
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## Database Setup

### MongoDB Atlas (Cloud)

1. Create MongoDB Atlas account: https://www.mongodb.com/cloud/atlas
2. Create a cluster
3. Create database user with strong password
4. Get connection string
5. Update MONGODB_URL in `.env`

### MongoDB Local

```bash
# Install MongoDB
# On Windows: https://docs.mongodb.com/manual/tutorial/install-mongodb-on-windows/

# Start MongoDB
mongod

# Test connection
mongosh mongodb://localhost:27017
```

### Initialize Collections and Indexes

```bash
# Run seed script
python scripts/seed_database.py
```

---

## Security Considerations

### API Key Management

```python
# Rotate API keys regularly
# Store in secure vault (AWS Secrets Manager, HashiCorp Vault, etc.)
# Never commit keys to version control
# Use different keys for different environments
```

### CORS Configuration

Update `main.py` for production:

```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "https://yourdomain.com",
        "https://www.yourdomain.com",
    ],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["Content-Type", "X-API-Key"],
)
```

### HTTPS/SSL

```bash
# Using Let's Encrypt with Certbot
certbot certonly --standalone -d yourdomain.com

# Configure nginx or use uvicorn with SSL
uvicorn main:app \
  --host 0.0.0.0 \
  --port 443 \
  --ssl-keyfile=/path/to/key.pem \
  --ssl-certfile=/path/to/cert.pem \
  --workers 4
```

### Rate Limiting

Install and configure:

```bash
pip install slowapi
```

Add to `main.py`:

```python
from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter

@app.get("/v1/subscribers")
@limiter.limit("100/minute")
async def list_subscribers(request: Request):
    pass
```

---

## Deployment Methods

### Option 1: Docker & Docker Compose

```bash
# Build Docker image
docker build -t plataforma-core-ohs:1.0.0 .

# Run with Docker Compose
docker-compose -f docker-compose.yml up -d

# Monitor logs
docker-compose logs -f app
```

### Option 2: Linux VM (Ubuntu 20.04+)

```bash
# 1. Install Python and dependencies
sudo apt update
sudo apt install python3.11 python3.11-venv python3.11-dev

# 2. Clone repository
git clone https://github.com/yourrepo/plataforma-core-ohs.git
cd plataforma-core-ohs

# 3. Create virtual environment
python3.11 -m venv venv
source venv/bin/activate

# 4. Install dependencies
pip install -r requirements.txt

# 5. Create systemd service file
sudo nano /etc/systemd/system/plataforma-core-ohs.service
```

Create systemd service:

```ini
[Unit]
Description=Plataforma Core OHS
After=network.target

[Service]
Type=notify
User=www-data
WorkingDirectory=/home/app/plataforma-core-ohs
EnvironmentFile=/home/app/plataforma-core-ohs/.env
ExecStart=/home/app/plataforma-core-ohs/venv/bin/uvicorn main:app \
    --host 0.0.0.0 \
    --port 8000 \
    --workers 4 \
    --timeout 120
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable plataforma-core-ohs
sudo systemctl start plataforma-core-ohs
sudo systemctl status plataforma-core-ohs
```

### Option 3: Kubernetes (K8s)

Create `k8s-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: plataforma-core-ohs
spec:
  replicas: 3
  selector:
    matchLabels:
      app: plataforma-core-ohs
  template:
    metadata:
      labels:
        app: plataforma-core-ohs
    spec:
      containers:
      - name: app
        image: plataforma-core-ohs:1.0.0
        ports:
        - containerPort: 8000
        env:
        - name: MONGODB_URL
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: mongodb-url
        - name: API_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: api-key
        - name: SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: secret-key
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: plataforma-core-ohs-service
spec:
  selector:
    app: plataforma-core-ohs
  type: LoadBalancer
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8000
```

Deploy:

```bash
kubectl create secret generic app-secrets \
  --from-literal=mongodb-url="your-mongodb-url" \
  --from-literal=api-key="your-api-key" \
  --from-literal=secret-key="your-secret-key"

kubectl apply -f k8s-deployment.yaml
```

### Option 4: AWS (Elastic Container Service)

```bash
# Push to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin your-account-id.dkr.ecr.us-east-1.amazonaws.com

docker tag plataforma-core-ohs:1.0.0 \
  your-account-id.dkr.ecr.us-east-1.amazonaws.com/plataforma-core-ohs:1.0.0

docker push your-account-id.dkr.ecr.us-east-1.amazonaws.com/plataforma-core-ohs:1.0.0

# Create ECS task definition and service
# Use AWS Console or CLI
```

---

## Monitoring & Logging

### Application Logging

```python
import logging

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('logs/app.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)
```

### Monitoring Tools

1. **Prometheus + Grafana**
   ```bash
   pip install prometheus-client
   ```

2. **ELK Stack** (Elasticsearch, Logstash, Kibana)
   ```yaml
   # Docker Compose configuration available
   ```

3. **Datadog** (SaaS option)
   ```bash
   pip install datadog
   ```

### Health Check Monitoring

```bash
# Cron job for health checks
*/5 * * * * curl -f http://localhost:8000/health || systemctl restart plataforma-core-ohs
```

---

## Scaling & Performance

### Horizontal Scaling

```bash
# Increase workers
uvicorn main:app --workers 8

# Load balancing with nginx
```

Create `nginx.conf`:

```nginx
upstream app {
    server 127.0.0.1:8001;
    server 127.0.0.1:8002;
    server 127.0.0.1:8003;
}

server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://app;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### Caching

```bash
pip install fastapi-cache2
pip install aioredis
```

### Database Optimization

1. Create appropriate indexes
2. Use connection pooling (Motor handles this)
3. Monitor query performance
4. Archive old data regularly

---

## Backup & Disaster Recovery

### MongoDB Backups

```bash
# Automated daily backup
0 2 * * * mongodump --uri "mongodb://..." --out /backups/$(date +\%Y\%m\%d)

# Restore from backup
mongorestore --uri "mongodb://..." /backups/20240330
```

### Database Replication

Use MongoDB Atlas automatic backups or configure replica sets for self-hosted MongoDB.

---

## Post-Deployment Validation

```bash
# Test API health
curl https://yourdomain.com/health

# Test authentication
curl -H "X-API-Key: your-api-key" https://yourdomain.com/v1/subscribers

# Load testing
pip install locust

# Create locustfile.py and run
locust -f locustfile.py --host=https://yourdomain.com
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check logs
systemctl status plataforma-core-ohs
journalctl -u plataforma-core-ohs -n 50

# Test configuration
python -c "from app.core.config import settings; print(settings)"
```

### Database Connection Issues

```bash
# Test MongoDB connection
mongosh "your-connection-string"

# Check network connectivity
nc -zv mongodb-host 27017
```

### High Memory Usage

```bash
# Reduce workers
# Check for memory leaks
# Implement caching
```

---

## Support & Maintenance

- Regular security updates
- Monthly backup verification
- Quarterly performance review
- Annual disaster recovery drill

---

For more information, refer to the main [README.md](README.md)

