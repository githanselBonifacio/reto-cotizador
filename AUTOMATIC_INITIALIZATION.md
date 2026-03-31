# 🤖 Sistema Automatizado de Inicialización de Bases de Datos

## 📋 Resumen

El proyecto ahora tiene un **sistema automático de inicialización** que se ejecuta cada vez que inicias los servicios con Docker Compose. No necesitas ejecutar scripts manualmente.

---

## 🎯 Cómo Funciona

```
docker compose up
    ↓
MongoDB inicia (healthcheck)
    ↓
db-init service inicia (espera a que MongoDB esté sano)
    ↓
FastAPI seed (CATALOGO_DANOS)
    ↓
Spring Boot seed (PLATAFORMA_DANOS)
    ↓
db-init service completa (EXIT 0)
    ↓
FastAPI inicia (espera a que db-init esté completado)
    ↓
Spring Boot inicia (espera a que db-init esté completado)
    ↓
Angular inicia
    ↓
✅ Todo listo y sin datos duplicados
```

---

## 🚀 Uso

### Opción 1: Levantar TODO con seeding automático (RECOMENDADO)

```bash
# Desde la raíz del proyecto
docker compose up
```

**Lo que sucede:**
1. ✅ MongoDB se levanta
2. ✅ Servicio db-init se ejecuta (siembra ambas bases de datos)
3. ✅ FastAPI inicia con datos disponibles
4. ✅ Spring Boot inicia con datos disponibles
5. ✅ Angular inicia

**Salida esperada:**
```
reto-db-init       | ════════════════════════════════════════════════════════════════════════════════
reto-db-init       |                MongoDB DATABASE INITIALIZATION
reto-db-init       | ════════════════════════════════════════════════════════════════════════════════
reto-db-init       | Starting automatic database initialization...
reto-db-init       | ✅ MongoDB connection successful
reto-db-init       | ✅ FastAPI (CATALOGO_DANOS) seeding completed successfully
reto-db-init       | ✅ Spring Boot (PLATAFORMA_DANOS) seeding completed successfully
reto-db-init exited with code 0
```

### Opción 2: Modo detached (background)

```bash
docker compose up -d
```

Esto levanta todo en background. Ver logs con:
```bash
docker logs reto-db-init -f
```

### Opción 3: Reconstruir además de inicializar

```bash
docker compose up --build
```

Esto reconstruye las imágenes y luego inicia todo con seeding automático.

---

## 🔄 Segunda Ejecución (Persistencia)

Los datos persisten porque:

1. **MongoDB usa volúmenes persistentes**
   ```yaml
   volumes:
     - mongo_data:/data/db  # Datos persisten entre ejecuciones
   ```

2. **Los scripts son idempotentes**
   - No duplican datos
   - Actualizan si el registro ya existe
   - Crean colecciones solo si no existen

**Cuando ejecutas `docker compose up` la segunda vez:**
```bash
docker compose up
```

✅ Los datos existentes se conservan
✅ El servicio db-init corre nuevamente (es seguro, no puede causar problemas)
✅ Las aplicaciones inician normalmente

---

## 📊 Componentes

### 1. Servicio db-init
- **Imagen:** Construida desde `Dockerfile.init`
- **Base:** `python:3.11-slim`
- **Dependencia:** Espera a que MongoDB esté sano
- **Reinicio:** `no` - Solo corre una vez por sesión de Docker
- **Salida:** EXIT 0 si todo es ok, EXIT 1 si hay error

### 2. Dockerfile.init
```dockerfile
FROM python:3.11-slim

# Instala motor, pymongo, python-dotenv
# Copia los scripts de seed
# Ejecuta initialize_databases.py
```

### 3. initialize_databases.py
- Script orchestrador
- Verifica conectividad a MongoDB con reintentos
- Ejecuta FastAPI seed
- Ejecuta Spring Boot seed
- Muestra resumen visual

### 4. MongoDB Volumes
```yaml
volumes:
  mongo_data:  # Persiste datos entre sesiones
```

---

## 🔍 Ver Logs de Inicialización

```bash
# Ver logs del servicio db-init
docker logs reto-db-init

# Ver logs en vivo
docker logs reto-db-init -f

# Ver todos los logs (incluidas todas las aplicaciones)
docker compose logs -f

# Ver solo FastAPI después de init
docker logs reto-core-ohs -f

# Ver solo Spring Boot después de init
docker logs reto-cotizador-back -f
```

---

## ⚙️ Variables de Entorno en db-init

```yaml
environment:
  MONGODB_URI: mongodb://mongodb:27017
  DATABASE_NAME: CATALOGO_DANOS
  SPRING_DATABASE: PLATAFORMA_DANOS
```

Estas se pasan automáticamente a los scripts de seed.

---

## 🛠️ Validar que Todo Funcionó

### 1. Verificar que los contenedores estén corriendo

```bash
docker compose ps
```

Esperado:
```
NAME              STATUS
reto-mongodb      Up (healthy)
reto-db-init      Exited (0)   ← El init solo corra una vez
reto-core-ohs     Up (health: starting)
reto-cotizador-back  Up
reto-cotizador-web   Up
```

### 2. Verificar datos en MongoDB

```bash
# Conectarse a MongoDB
mongosh mongodb://localhost:27017/CATALOGO_DANOS

# Ver documentos
db.subscribers.find()
db.tariffs.find()
db.quotes.find()
```

### 3. Acceder a las APIs

- **FastAPI:** http://localhost:8000/health (debe devolver `{"status": "healthy"}`)
- **FastAPI Docs:** http://localhost:8000/docs
- **Spring Boot:** http://localhost:8080/v3/api-docs (debe devolver OpenAPI spec)
- **Angular:** http://localhost:4200

---

## 🚪 Detener y Limpiar

### Parar todo

```bash
docker compose down
```

Esto:
- ✅ Para todos los contenedores
- ✅ Elimina redes
- ✅ **CONSERVA LOS DATOS** (volumen mongo_data persiste)

### Parar y eliminar TODO (incluida la base de datos)

```bash
docker compose down -v
```

Esto:
- ✅ Para todos los contenedores
- ✅ Elimina la red
- ✅ ❌ ELIMINA los datos (volumen mongo_data se borra)

La próxima vez que ejecutes `docker compose up`, las bases de datos se crearánnuevamente desde cero con el seeding automático.

---

## 🐛 Solución de Problemas

### ❌ El servicio db-init falla

**Síntomas:**
```
reto-db-init exited with code 1
```

**Soluciones:**
```bash
# Ver logs detallados
docker logs reto-db-init

# Reconstruir la imagen
docker compose up --build

# O eliminar la imagen y reconstruir
docker rmi reto-db-init
docker compose up --build
```

### ❌ Los datos no persisten después de `docker compose down`

Usa `docker compose down` sin `-v`:
```bash
docker compose down    # ✅ Datos persisten
docker compose down -v # ❌ Datos se eliminan
```

### ❌ MongoDB no está listo cuando db-init intenta conectar

El servicio db-init reintenta automáticamente hasta 15 veces con 2 segundos de espera entre intentos. Si sigue fallando:

```bash
# Verificar que MongoDB esté corriendo
docker compose logs reto-mongodb

# Reiniciar MongoDB manualmente
docker compose down
docker compose up mongodb -d
docker compose up  # Ahora inicia todo
```

### ❌ Quiero ejecutar el seed manualmente

Aunque no es necesario (se ejecuta automáticamente), puedes:

```bash
# Dentro del contenedor db-init
docker exec reto-db-init python /app/initialize_databases.py

# O desde el host (después de que Docker esté corriendo)
cd reto\ cotizador
python initialize_databases.py
```

---

## 📝 Diagrama de Flujo Completo

```
┌─────────────────────────────────────────────────────────┐
│         docker compose up / docker compose up -d        │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────────┐    ┌──────────────────────┐
│   mongodb:7      │    │ db-init service      │
│ (healthcheck)    │→───→ (Python 3.11)        │
│ :27017           │    │ (corre UNA VEZ)      │
└──────────────────┘    └──────────┬───────────┘
   (healthy)                      │
                    ┌─────────────┴──────────────┐
                    ▼                            ▼
          ┌──────────────────────┐     ┌──────────────────┐
          │ FastAPI seed         │     │ Spring Boot seed │
          │ (motor + motor)       │     │ (motor + motor)  │
          │ CATALOGO_DANOS       │     │ PLATAFORMA_DANOS │
          └──────┬───────────────┘     └─────────┬────────┘
                 │                              │
                 └──────────────┬───────────────┘
                                ▼
                     ┌──────────────────────┐
                     │  db-init EXIT 0      │
                     │ (initialization done)│
                     └──────────┬───────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│ FastAPI          │   │ Spring Boot      │   │ Angular Web      │
│ :8000            │   │ :8080            │   │ :4200            │
│ (with data ✓)    │   │ (with data ✓)    │   │ (ready ✓)        │
└──────────────────┘   └──────────────────┘   └──────────────────┘
```

---

## ✨ Resumen de Ventajas

| Ventaja | Descripción |
|---------|-------------|
| ✅ **Automático** | No necesitas ejecutar scripts manualmente |
| ✅ **Idempotente** | Puedes ejecutar múltiples veces sin problemas |
| ✅ **Persistente** | Los datos se conservan entre sesiones |
| ✅ **Rápido** | Tarda ~5-10 segundos total |
| ✅ **Seguro** | No duplica datos |
| ✅ **Escalable** | Fácil de agregar más seeds |
| ✅ **Observable** | Logs claros de progreso |

---

**¡Ya está todo configurado para automatización! 🚀**

Solo ejecuta `docker compose up` y todo funcionará automáticamente.
