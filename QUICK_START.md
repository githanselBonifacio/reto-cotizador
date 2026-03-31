# 🚀 GUÍA RÁPIDA DE INICIO

## ⚡ Los 3 Pasos Más Importantes

### 1️⃣ Verifica que todo está bien
```bash
python verify_setup.py
```

Debe mostrar:
```
✅ ALL CHECKS PASSED - READY TO USE AUTO-INITIALIZATION
```

### 2️⃣ Levanta TODO con inicialización automática
```bash
docker compose up
```

Espera a ver:
```
reto-db-init exited with code 0
```

Esto significa que:
- ✅ MongoDB está listo
- ✅ FastAPI base de datos seeded (CATALOGO_DANOS)
- ✅ Spring Boot base de datos seeded (PLATAFORMA_DANOS)

### 3️⃣ Accede a las aplicaciones
- **FastAPI**: http://localhost:8000/docs
- **Spring Boot**: http://localhost:8080/swagger-ui.html  
- **Angular**: http://localhost:4200

---

## 🎯 ¿Cuál es la diferencia con antes?

| Aspecto | Antes | Ahora |
|--------|-------|-------|
| **Paso 1** | `docker compose up` | `docker compose up` |
| **Paso 2** | `python seed_all_databases.py` | **(¡No necesario!)** |
| **Total** | 2 comandos manuales | 1 comando automático |
| **Datos** | Posiblemente no listos | ✅ Automáticamente listos |

---

## 📊 ¿Qué se siembra automáticamente?

### FastAPI (CATALOGO_DANOS)
✅ Subscribers (2 registros)  
✅ Agents (2 registros)  
✅ Business Lines (3 registros)  
✅ ZIP Codes (2 registros)  
✅ Folios (1 registro)  
✅ Risk Classifications (4 registros)  
✅ Guarantees (3 registros)  
✅ Tariffs (3 registros)  

### Spring Boot (PLATAFORMA_DANOS)
✅ Cotizaciones/Quotes (2 ejemplos)

---

## 🔄 Segunda vez que ejecutas `docker compose up`

Tus datos **permanecen** porque:
- MongoDB usa volúmenes persistentes (`mongo_data`)
- El servicio db-init corre de nuevo (es seguro, no duplica)

```bash
docker compose up
# Los datos de la sesión anterior están ahí ✅
```

---

## 🛑 Para detener

**Sin eliminar datos:**
```bash
docker compose down
# Datos persisten - ejecuta docker compose up otra vez y estarán ahí
```

**Eliminando TODO (incluida BD):**
```bash
docker compose down -v
# Próxima ejecución de docker compose up volverá a sembrar desde cero
```

---

## 🐛 Si algo no funciona

### Ver logs del seeding
```bash
docker logs reto-db-init
```

### Ver todos los logs
```bash
docker compose logs -f
```

### Verificar que MongoDB tiene datos
```bash
mongosh mongodb://localhost:27017/CATALOGO_DANOS
db.subscribers.find()
```

### Reconstruir imágenes
```bash
docker compose up --build
```

---

## 📚 Documentación Completa

- **AUTOMATIC_INITIALIZATION.md** - Cómo funciona el sistema
- **AUTOMATION_CHANGES.md** - Qué se cambió y por qué
- **DATABASE_SEEDING_GUIDE.md** - Detalles de las colecciones

---

## ✨ Lo Más Importante

```
docker compose up

...espera ~2 minutos...

✅ TODO FUNCIONA SIN HACER NADA MÁS
```

**¡Eso es todo! 🎉**

No necesitas ejecutar scripts de seeding manualmente. Docker Compose se encarga de todo automáticamente.
