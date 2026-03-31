# Guía de Initialización de Bases de Datos MongoDB

## Descripción General

Este proyecto utiliza MongoDB para almacenar datos en dos aplicaciones:
1. **plataforma-core-ohs** (FastAPI) - Base de datos de catálogos y configuraciones
2. **cotizador-danos-back** (Spring Boot) - Base de datos de cotizaciones

Este documento explica cómo inicializar y sembrar (seed) ambas bases de datos.

---

## Estructura de Datos

### 📊 FastAPI (plataforma-core-ohs) - Base de datos: `CATALOGO_DANOS`

**8 Colecciones:**

| Colección | Registros | Propósito | Índice Único |
|---|---|---|---|
| `subscribers` | 2 | Empresas de seguros | email |
| `agents` | 2 | Agentes/Brokers | agent_code |
| `business_lines` | 3 | Líneas de negocio | code |
| `zip_codes` | 2 | Códigos postales y zonas de riesgo | zip_code |
| `folios` | 1 | Generador de secuencia para folios | collection_name |
| `risk_classifications` | 4 | Clasificaciones de riesgo | code |
| `guarantees` | 3 | Coberturas/Garantías | code |
| `tariffs` | 3 | Tarifas y factores | code |

**Datos de Ejemplo:**
- Subscribers: Acme Insurance Corp, Global Risk Management
- Agents: John Carter (AG001), Laura Mitchell (AG002)
- Business Lines: Fire Insurance, Catastrophe Coverage, All Risks
- Risk Zones: Mexico City (LOW), Monterrey (MEDIUM)
- Tariffs: INCENDIO (1.5%), CAT (0.5%), FHM (0.8%)

### 📊 Spring Boot (cotizador-danos-back) - Base de datos: `PLATAFORMA_DANOS`

**1 Colección:**

| Colección | Propósito | Índices |
|---|---|---|
| `cotizaciones` | Cotizaciones/Quotes generadas por usuarios | numeroFolio (único), estadoCotizacion, fechaUltimaActualizacion |

**Datos de Ejemplo:**
- Cotización 1: FOL-2026-001 (PENDING) - Empresa XYZ S.A., Almacén Principal
- Cotización 2: FOL-2026-002 (CALCULADO) - Retail Solutions Ltd., Tienda Metropolitana

---

## Métodos de Inicialización

### ✅ Opción 1: Script Maestro (RECOMENDADO)

Ejecuta ambos seeds en secuencia con un único comando:

```bash
# Desde la raíz del proyecto
python seed_all_databases.py
```

**Ventajas:**
- ✓ One-command solution
- ✓ Mejor manejo de errores
- ✓ Resumen visual del estado de ambas bases de datos

**Requisitos:**
```bash
pip install motor python-dotenv pymongo
```

---

### ✅ Opción 2: Seeds Individuales

Si prefieres ejecutar cada seed por separado:

#### FastAPI (plataforma-core-ohs)

```bash
cd plataforma-core-ohs
python scripts/seed_database.py
```

#### Spring Boot (cotizador-danos-back)

```bash
cd cotizador-danos-back
python scripts/seed_database.py
```

---

### ✅ Opción 3: MongoDB Shell (Alternativa Manual)

Si prefieres crear collections manualmente:

```bash
# Conectarse a MongoDB
mongosh mongodb://localhost:27017

# Para FastAPI
use CATALOGO_DANOS
db.createCollection("subscribers")
db.createCollection("agents")
db.createCollection("business_lines")
db.createCollection("zip_codes")
db.createCollection("folios")
db.createCollection("risk_classifications")
db.createCollection("guarantees")
db.createCollection("tariffs")

# Para Spring Boot
use PLATAFORMA_DANOS
db.createCollection("cotizaciones")
```

---

## Uso en Docker Compose

Si estás usando Docker Compose, puedes ejecutar el seed después de que los contenedores estén corriendo:

```bash
# 1. Levantar los servicios
docker compose up -d

# 2. Esperar a que MongoDB esté listo (~ 30-40 segundos)
# 3. Ejecutar el seed maestro
python seed_all_databases.py
```

Alternativamente, ejecuta seeds individuales dentro de los contenedores:

```bash
# Seed FastAPI en el contenedor
docker exec reto-core-ohs python scripts/seed_database.py

# Seed Spring Boot desde el host
cd cotizador-danos-back && python scripts/seed_database.py
```

---

## Variables de Entorno

Puedes configurar las variables de entorno para personalizar la conexión a MongoDB:

```bash
# En .env o al ejecutar el script
export MONGODB_URI="mongodb://localhost:27017"
export DATABASE_NAME="CATALOGO_DANOS"  # Para FastAPI
export SPRING_PROFILES_ACTIVE="local"
```

### Docker Compose usa:
```yaml
MONGODB_URL: mongodb://mongodb:27017
DATABASE_NAME: CATALOGO_DANOS  # FastAPI
MONGODB_URI: mongodb://mongodb:27017/PLATAFORMA_DANOS  # Spring Boot
```

---

## Características de los Scripts

### Idempotencia
Todos los scripts son **idempotentes**, lo que significa que:
- ✓ Pueden ejecutarse múltiples veces sin errores
- ✓ Actualizan datos existentes en lugar de duplicarlos
- ✓ Crean colecciones solo si no existen

### Garantías de Datos
- ✓ Se crean índices únicos automáticamente
- ✓ Se valida la conexión a MongoDB
- ✓ Se muestran mensajes de progreso claros

### Seguridad
- ✓ Los timestamps se almacenan en UTC
- ✓ Se valida la integridad de los datos
- ✓ Se manejan errores de conexión gracefully

---

## Solución de Problemas

### ❌ Error: "Unable to connect to MongoDB"

**Causa:** MongoDB no está corriendo o la URI es incorrecta.

**Solución:**
```bash
# Verificar que MongoDB está corriendo
docker compose ps

# Si no está corriendo, iniciarlo
docker compose up -d mongodb

# Verificar conectividad
mongosh mongodb://localhost:27017 --eval "db.adminCommand('ping')"
```

### ❌ Error: "Module not found: motor"

**Causa:** Falta instalar dependencias.

**Solución:**
```bash
pip install -r requirements.txt
# O manualmente:
pip install motor python-dotenv pymongo
```

### ❌ Error: "Database already exists" (en la segunda ejecución)

**Esto es NORMAL.** El script detecta que la base de datos ya existe y continúa sin error.

### ❌ Los datos no aparecen en la aplicación

**Verificar:**
1. Que el seed finalizó sin errores (buscar "✅ completed successfully")
2. Que la aplicación está usando la misma URI de MongoDB
3. Conectarse con mongosh y verificar los datos:
```bash
mongosh mongodb://localhost:27017/CATALOGO_DANOS
db.subscribers.find().pretty()
```

---

## Flujo de Datos

```
┌─────────────────────────────────────────────────────────────────┐
│                    seed_all_databases.py                         │
│              (Script maestro - ÚniCO punto de entrada)           │
└──────────────────────┬──────────────────────════────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
┌──────────────────────┐    ┌──────────────────────┐
│ FastAPI Seed Script  │    │ Spring Boot Seed     │
│ (secuencial)         │    │ Script (secuencial)  │
└──────────┬───────────┘    └──────────┬───────────┘
           │                          │
           ▼                          ▼
┌──────────────────────┐    ┌──────────────────────┐
│ MongoDB Database     │    │ MongoDB Database     │
│ CATALOGO_DANOS       │    │ PLATAFORMA_DANOS     │
│ (8 colecciones)      │    │ (1 colección)        │
└──────────────────────┘    └──────────────────────┘
```

---

## Limpieza (Opcional)

Si necesitas limpiar los datos y empezar de nuevo:

```bash
# Eliminar todas las bases de datos (CUIDADO)
mongosh mongodb://localhost:27017 --eval "db.dropDatabase()"

# Eliminar solo colecciones específicas
mongosh mongodb://localhost:27017/CATALOGO_DANOS --eval "db.subscribers.deleteMany({})"
mongosh mongodb://localhost:27017/PLATAFORMA_DANOS --eval "db.cotizaciones.deleteMany({})"

# Luego ejecutar los seeds nuevamente
python seed_all_databases.py
```

---

## Referencias

- [MongoDB Documentation](https://docs.mongodb.com/)
- [Motor (Async MongoDB Driver)](https://motor.readthedocs.io/)
- [PyMongo](https://pymongo.readthedocs.io/)

---

**Última actualización:** Marzo 31, 2026
