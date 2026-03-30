# VALIDACIÓN DE CONTRATO API - REPORTE COMPLETO

## 📋 RESUMEN EJECUTIVO

Se identificaron **1 problema crítico** en la implementación de CatalogClient que ha sido **CORREGIDO**.

**Estado Final: ✅ TODOS LOS ENDPOINTS CUMPLEN CON CONTRATO**

---

## 🔴 PROBLEMA CRÍTICO (CORREGIDO)

### getTariffs() - Parámetros Incorrectos

#### ❌ ANTES (Incorrecto):
```java
public Mono<TechnicalTariffs> getTariffs(String zipCode, String fireKey) {
    return catalogWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v1/tariffs")
            .queryParam("zip_code", zipCode)      // NO EXISTE EN CONTRATO
            .queryParam("fire_key", fireKey)      // NO EXISTE EN CONTRATO
            .queryParam("zipCode", zipCode)       // DUPLICADO E INCORRECTO
            .queryParam("industry", fireKey)      // NO EXISTE EN CONTRATO
            .build())
        .retrieve()
        .bodyToMono(Map.class)
        .map(this::toTechnicalTariffs);
}
```

#### ✅ DESPUÉS (Correcto):
```java
public Mono<TechnicalTariffs> getTariffs(String zipCode, String fireKey) {
    // CONTRATO: GET /v1/tariffs con query params: skip, limit, factor_type (optional), business_line_id (optional)
    // Nota: zipCode y fireKey son ignorados según el nuevo contrato de API
    // Los tariffs se filtran por factor_type (INCENDIO, CAT, FHM) en la respuesta
    return catalogWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v1/tariffs")
            .queryParam("skip", 0)
            .queryParam("limit", 100)
            .build())
        .retrieve()
        .bodyToMono(Map.class)
        .map(this::toTechnicalTariffs);
}
```

#### 🔍 Análisis del Problema:
- **Parámetros enviados**: `zip_code`, `fire_key`, `zipCode`, `industry`
- **Parámetros válidos (contrato)**: `skip`, `limit`, `factor_type`, `business_line_id`
- **Impacto**: La API de catálogos NO podía procesar estos parámetros
- **Motivo**: Los tariffs están globalizados en el catálogo, no están filtrados por ZIP code
- **Solución**: Usar los parámetros correctos del contrato (skip/limit) y dejar que la API devuelva todos los tariffs

---

## ✅ ENDPOINTS VALIDADOS

| # | Endpoint | Método | Auth | Status | Notas |
|---|----------|--------|------|--------|-------|
| 1 | GET / | GET | No | ✅ OK | Info del servicio |
| 2 | GET /health | GET | No | ✅ OK | Health check |
| 3 | GET /v1/subscribers | GET | X-API-Key | ✅ OK | Lista de suscriptores |
| 4 | GET /v1/agents | GET | X-API-Key | ✅ OK | Lista de agentes |
| 5 | GET /v1/business-lines | GET | X-API-Key | ✅ OK | Líneas de negocio |
| 6 | GET /v1/zip-codes/{zip_code} | GET | X-API-Key | ✅ OK | Info de código postal |
| 7 | POST /v1/zip-codes/validate | POST | X-API-Key | ✅ OK | Validar ZIP code |
| 8 | GET /v1/folios | GET | X-API-Key | ✅ OK | Obtener próximo folio |
| 9 | **GET /v1/tariffs** | **GET** | X-API-Key | **✅ FIXED** | **Parámetros corregidos** |
| 10 | GET /v1/catalogs/risk-classification | GET | X-API-Key | ⚠️ NO USADO | Disponible pero no implementado |
| 11 | GET /v1/catalogs/guarantees | GET | X-API-Key | ⚠️ NO USADO | Disponible pero no implementado |

---

## 🔐 VALIDACIÓN DE SEGURIDAD

### Headers de Autenticación ✅
```java
@Bean
WebClient catalogWebClient(WebClient.Builder builder,
                           @Value("${catalog.base-url:http://localhost:8001}") String baseUrl,
                           @Value("${catalog.api-key:api-key-change-in-production}") String apiKey) {
    return builder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey)      // ✅ CORRECTO
            .defaultHeader("x-api-key", apiKey)                                 // ✅ CORRECTO
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .build();
}
```

**Estado:** ✅ Configurado correctamente
- Se envían ambos headers (compatibilidad)
- API Key se lee de `application.properties`
- Se aplica a todos los requests de /v1/*

---

## 📊 VALIDACIÓN DE RESPUESTAS

### Endpoint: GET /v1/tariffs (CORREGIDO)

**Contrato esperado:**
```json
{
  "total": 15,
  "items": [
    {
      "_id": "tar_001",
      "code": "TAR-INCENDIO-2024",
      "name": "Tarifa Incendio 2024",
      "factor_type": "INCENDIO",
      "base_rate": 0.015,
      "min_rate": 0.010,
      "max_rate": 0.025,
      "business_line_id": "bl_001",
      "effective_date": "2024-01-15T00:00:00Z",
      "expiration_date": "2024-12-31T23:59:59Z",
      "status": "active"
    }
  ]
}
```

**Parsing en código:** ✅ CORRECTO
- Se extrae lista de items[]
- Se itera y filtra por factor_type (INCENDIO, CAT, FHM)
- Se aplica clamping de rates (min/max)
- Se valida fecha efectiva (effective_date ≤ hoy ≤ expiration_date)

---

## 🔧 CAMBIOS REALIZADOS

### Archivo: CatalogClient.java

**Cambio 1: Línea 65-78**
```diff
- .queryParam("zip_code", zipCode)
- .queryParam("fire_key", fireKey)
- .queryParam("zipCode", zipCode)
- .queryParam("industry", fireKey)
+ .queryParam("skip", 0)
+ .queryParam("limit", 100)
```

**Cambio 2: Agregado comentario explicativo**
```java
// CONTRATO: GET /v1/tariffs con query params: skip, limit, factor_type (optional), business_line_id (optional)
// Nota: zipCode y fireKey son ignorados según el nuevo contrato de API
// Los tariffs se filtran por factor_type (INCENDIO, CAT, FHM) en la respuesta
```

---

## 📝 MAPA DE USO DE ENDPOINTS

### En CotizacionUseCaseImpl.java:
```
calculateLocation()
  └─> catalogClient.getTariffs(codigoPostal, claveIncendio)  [LÍNEA 236]
      └─> GET /v1/tariffs (con skip=0, limit=100)
          └─> Filtra por factor_type en toTechnicalTariffs()
              ├─ INCENDIO rate
              ├─ CAT rate
              └─ FHM rate
```

---

## ⚠️ CONSIDERACIONES Y RECOMENDACIONES

### 1. **Parámetro business_line_id no utilizado**
- **Contrato permite:** Filtrar tariffs por `business_line_id`
- **Código actual:** No explota este filtro
- **Recomendación:** En futuro, si necesitas filtrar tariffs específicos para una línea de negocio, agregar este parámetro

### 2. **Endpoints NO utilizados**
Los siguientes están en el contrato pero no se usan:
- POST /v1/zip-codes (crear ZIP code)
- GET /v1/catalogs/risk-classification
- GET /v1/catalogs/guarantees
- POST /v1/subscribers, PUT /v1/subscribers, etc. (CRUD completo)

**Recomendación:** Implementar cuando se necesite consultar clasificaciones de riesgo o garantías disponibles

### 3. **Límite de paginación**
- **Límite actual:** 100 items
- **Límite máximo (contrato):** Configurable en API
- **Recomendación:** Si hay >100 tariffs, implementar paginación adaptativa

### 4. **Manejo de errores**
- **404 Not Found:** Cuando ZIP no existe
- **403 Forbidden:** Si API key es inválida
- **Recomendación:** Agregar tratamiento específico de errores HTTP en cliente

---

## ✅ CHECKLIST DE VALIDACIÓN

- [x] Headers X-API-Key configurados correctamente
- [x] GET /v1/subscribers implementado
- [x] GET /v1/agents implementado
- [x] GET /v1/business-lines implementado
- [x] GET /v1/zip-codes/{zip_code} implementado
- [x] POST /v1/zip-codes/validate implementado (con campo correcto: zip_code)
- [x] GET /v1/folios implementado
- [x] **GET /v1/tariffs implementado con parámetros CORRECTOS**
- [ ] GET /v1/catalogs/risk-classification (opcional)
- [ ] GET /v1/catalogs/guarantees (opcional)

---

## 🚀 ESTADO FINAL

**✅ VALIDACIÓN COMPLETADA**

Todos los endpoints están usando los parámetros y formatos correctos según el contrato de API de Plataforma Core OHS.

El código está listo para comunicarse correctamente con la API de catálogos.

**Fecha de validación:** 2026-03-30
**Versión API contrato:** 1.0.0

