# 🌱 Inicialización Completa del Proyecto - Resumen

## Cambios Realizados (31 de Marzo, 2026)

### ✅ 1. Correcciones realizadas a Docker Compose

**Archivo:** `docker-compose.yml`

**Problema:** Las variables de entorno `CORS_ORIGINS` y `ALLOWED_HOSTS` en la aplicación FastAPI no podían parsearse correctamente porque `pydantic-settings >= 2.6` requiere formato JSON.

**Solución:**
```yaml
# Antes ❌
CORS_ORIGINS: "*"
ALLOWED_HOSTS: "*"

# Después ✅
CORS_ORIGINS: '["*"]'
ALLOWED_HOSTS: '["*"]'
```

### ✅ 2. Corrección del healthcheck en FastAPI

**Archivo:** `plataforma-core-ohs/Dockerfile`

**Problema:** El healthcheck usaba `import requests` que no estaba disponible en el ambiente de healthcheck.

**Solución:**
```dockerfile
# Antes ❌
CMD python -c "import requests; requests.get('http://localhost:8000/health')"

# Después ✅
CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=5)"
```

---

## 🚀 Nuevos Scripts de Seeding Creados

### 1. Script Maestro de Seeding (RECOMENDADO)
**Archivo:** `seed_all_databases.py`

Ejecuta ambos seeds en secuencia con manejo robusto de errores:

```bash
python seed_all_databases.py
```

**Características:**
- ✅ Verifica ambasscripts de seed
- ✅ Ejecuta FastAPI seed primero
- ✅ Luego ejecuta Spring Boot seed
- ✅ Resumen visual del estado
- ✅ Manejo de errores mejorado

---

### 2. Script de Seeding para Spring Boot (NUEVO)
**Archivo:** `cotizador-danos-back/scripts/seed_database.py`

Inicializa la base de datos MongoDB para la aplicación Spring Boot:

**Lo que crea:**
- Base de datos: `PLATAFORMA_DANOS`
- Colección: `cotizaciones` (con índices únicos)
- Datos de ejemplo: 2 cotizaciones de prueba

**Ejemplo de cotización:**
```json
{
  "numeroFolio": "FOL-2026-001",
  "estadoCotizacion": "PENDING",
  "datosAsegurado": {
    "nombre": "Empresa XYZ S.A.",
    "rfc": "EXY880101ABC"
  },
  "primaNeta": 15000.50,
  "primaComercial": 18750.62,
  "opcionesCobertura": ["INCENDIO", "CAT", "FHM"]
}
```

---

### 3. Script Existente para FastAPI
**Archivo:** `plataforma-core-ohs/scripts/seed_database.py`

Ya existía y está completo. Crea 8 colecciones:

```
subscribers (2 registros)
agents (2 registros)  
business_lines (3 registros)
zip_codes (2 registros)
folios (1 registro)
risk_classifications (4 registros)
guarantees (3 registros)
tariffs (3 registros)
```

---

## 📊 Estado de las Bases de Datos

### FastAPI (plataforma-core-ohs)
**Base de datos:** `CATALOGO_DANOS`

```
✓ subscribers → 2 empresas de seguros
✓ agents → 2 agentes
✓ business_lines → 3 líneas de negocio (Fuego, CAT, All Risks)
✓ zip_codes → 2 códigos postales con zonas de riesgo
✓ folios → 1 generador de secuencia
✓ risk_classifications → 4 niveles de riesgo
✓ guarantees → 3 coberturas
✓ tariffs → 3 tarifas de factores
```

### Spring Boot (cotizador-danos-back)
**Base de datos:** `PLATAFORMA_DANOS`

```
✓ cotizaciones → 2 ejemplos de cotizaciones
  - FOL-2026-001: PENDING
  - FOL-2026-002: CALCULADO
```

---

## 🔄 Uso

### Opción 1: Script Maestro (RECOMENDADO)
```bash
python seed_all_databases.py
```

### Opción 2: Seeds Individuales
```bash
# FastAPI
cd plataforma-core-ohs
python scripts/seed_database.py

# Spring Boot
cd cotizador-danos-back
python scripts/seed_database.py
```

### Opción 3: En Docker
```bash
# Ejecutar dentro del contenedor de FastAPI
docker exec reto-core-ohs python scripts/seed_database.py

# Ejecutar desde el host para Spring Boot
cd cotizador-danos-back && python scripts/seed_database.py
```

---

## 📚 Documentación Completa

Ver **`DATABASE_SEEDING_GUIDE.md`** para:
- Detalles completos de cada colección
- Variables de entorno
- Solución de problemas
- Limpieza de datos
- Diagramas de flujo

---

## ✨ Características de los Scripts

- ✅ **Idempotentes**: Pueden ejecutarse múltiples veces sin problemas
- ✅ **Seguros**: Actualizan en lugar de duplicar datos
- ✅ **Validados**: Crean índices únicos automáticamente
- ✅ **Informativos**: Mensajes claros de progreso
- ✅ **Robustos**: Manejo de errores completo

---

## 🐳 Estado Docker Actual

```
✅ reto-mongodb (mongo:7) 
   └─ Puerto 27017 (healthy)
   └─ Volumen: mongo_data

✅ reto-core-ohs (FastAPI)
   └─ Puerto 8000 (healthy)
   └─ Documentación: http://localhost:8000/docs

✅ reto-cotizador-back (Spring Boot)
   └─ Puerto 8080
   └─ Swagger: http://localhost:8080/swagger-ui.html

✅ reto-cotizador-web (Angular)
   └─ Puerto 4200
   └─ UI: http://localhost:4200
```

---

## 📝 Próximos Pasos (Opcional)

1. Ejecutar `python seed_all_databases.py` para inicializar las bases de datos
2. Verificar datos en MongoDB:
   ```bash
   mongosh mongodb://localhost:27017/CATALOGO_DANOS
   db.subscribers.find().pretty()
   ```
3. Acceder a la API de FastAPI: http://localhost:8000/docs
4. Ver cotizaciones en Spring Boot: http://localhost:8080/swagger-ui.html

---

**Última actualización:** Marzo 31, 2026
