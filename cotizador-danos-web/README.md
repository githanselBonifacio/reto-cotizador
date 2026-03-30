# Cotizador Daños Web

Aplicación web Angular para cotización de daños con flujo guiado por pasos, integración con servicios de negocio, cálculo de prima, confirmación final y generación de PDF.

## Tabla de contenido

1. [Resumen funcional](#resumen-funcional)
2. [Tecnologías y arquitectura](#tecnologías-y-arquitectura)
3. [Requisitos previos](#requisitos-previos)
4. [Configuración rápida](#configuración-rápida)
5. [Configuración de entorno](#configuración-de-entorno)
6. [Ejecución local](#ejecución-local)
7. [Scripts disponibles](#scripts-disponibles)
8. [Rutas principales](#rutas-principales)
9. [Integración de APIs](#integración-de-apis)
10. [Autenticación por API Key](#autenticación-por-api-key)
11. [Pruebas unitarias y cobertura](#pruebas-unitarias-y-cobertura)
12. [Automatización E2E y mapeo de IDs](#automatización-e2e-y-mapeo-de-ids)
13. [Estructura del proyecto](#estructura-del-proyecto)
14. [Generación de build de producción](#generación-de-build-de-producción)
15. [Troubleshooting](#troubleshooting)
16. [Buenas prácticas para contribuir](#buenas-prácticas-para-contribuir)

## Resumen funcional

El proyecto implementa un flujo de cotización en pasos:

1. Información general del asegurado
2. Administración de ubicaciones
3. Información técnica (coberturas y layout)
4. Términos y condiciones
5. Cálculo y resumen de prima
6. Confirmación final y descarga de PDF

Características destacadas:

- Flujo por rutas con persistencia por folio.
- Catálogos consumidos por API para listas de selección (agentes, giros, coberturas).
- Bloqueo de edición cuando la cotización está en estado CALCULATED.
- Cálculo automático al entrar al paso de cálculo, con opción manual de recalcular.
- Generación de PDF de cotización.
- IDs únicos en campos y botones para facilitar automatización E2E.

## Tecnologías y arquitectura

- Angular 21 (standalone components)
- Angular Material
- Reactive Forms
- Signals y computed para estado local
- RxJS para flujos asíncronos
- jsPDF para documento PDF
- Unit test runner: Angular unit-test builder con Vitest

Arquitectura general:

- src/app/core: modelos, servicios HTTP, interceptor de autenticación
- src/app/features: módulos funcionales (home, quote-creation, etc.)
- src/environments: configuración por ambiente

## Requisitos previos

- Node.js 20+
- npm 10+
- Angular CLI compatible con Angular 21 (opcional si usas npm scripts)
- APIs backend disponibles localmente:
	- Quotes API: http://localhost:8080/v1
	- Core Catalog API: http://localhost:8000/v1

## Configuración rápida

1. Clonar el repositorio.
2. Instalar dependencias.
3. Verificar variables de entorno.
4. Levantar APIs backend.
5. Ejecutar la app.

Comandos:

```bash
npm install
npm start
```

Aplicación local:

- http://localhost:4200

## Configuración de entorno

Archivo de desarrollo: src/environments/environment.ts

```ts
export const environment = {
	production: false,
	apiUrl: 'http://localhost:8080/v1',
	coreApiUrl: 'http://localhost:8000/v1',
	requireApiKey: false,
	apiKey: 'your-api-key-change-in-production'
};
```

Archivo de producción: src/environments/environment.prod.ts

```ts
export const environment = {
	production: true,
	apiUrl: 'http://localhost:8080/v1',
	coreApiUrl: 'http://localhost:8000/v1',
	requireApiKey: true,
	apiKey: 'your-api-key-change-in-production'
};
```

Recomendación de seguridad:

- No publicar API keys reales en repositorios públicos.
- Para despliegue real, inyectar secretos con mecanismo seguro del entorno (pipeline, secret manager, variables protegidas).

## Ejecución local

Modo desarrollo:

```bash
npm start
```

Build local de validación:

```bash
npm run build
```

## Scripts disponibles

- npm start: levanta servidor de desarrollo
- npm run build: genera build de producción
- npm run watch: build en modo watch para desarrollo
- npm run test: ejecuta pruebas unitarias

## Rutas principales

Rutas funcionales:

- /
- /cotizador
- /quotes/:folio/general-info
- /quotes/:folio/locations
- /quotes/:folio/technical-info
- /quotes/:folio/terms-and-conditions
- /quotes/:folio/calculation-summary
- /quotes/:folio/confirmation

Fallback:

- Cualquier ruta no definida redirige a /cotizador.

## Integración de APIs

Servicios relevantes:

- src/app/core/services/quote.service.ts
- src/app/core/services/core-catalog.service.ts

Operaciones principales:

- Inicialización de folio y estado de cotización.
- Actualización de información general.
- Gestión de ubicaciones (alta, edición, eliminación).
- Gestión de cobertura y layout técnico.
- Cálculo de prima.
- Consulta de resumen.
- Catálogos de agentes, giros y coberturas.
- Validación de código postal.

## Autenticación por API Key

Interceptor:

- src/app/core/interceptors/auth.interceptor.ts

Comportamiento:

- Si el request ya trae x-api-key o Authorization, no lo modifica.
- Si requireApiKey es true, usa header Authorization con formato ApiKey <key>.
- Si requireApiKey es false, usa header x-api-key.

## Pruebas unitarias y cobertura

El proyecto usa Angular unit-test builder con Vitest.

Ejecución normal:

```bash
npm run test -- --watch=false
```

Ejecución con cobertura:

```bash
npm run test -- --watch=false --coverage
```

Salida de cobertura:

- coverage/cotizador-danos-web/index.html

Estado actual validado:

- Cobertura global superior al 90% en statements y branches.

## Automatización E2E y mapeo de IDs

Se incorporaron IDs únicos para facilitar automatización (por ejemplo Serenity BDD), incluyendo:

- Stepper y pasos
- Inputs, selects, checkbox
- Botones de navegación y guardado
- Controles dinámicos de layout técnico
- Controles del modal de ubicaciones

Convención recomendada:

- Prefijo qc- para flujo principal de cotización.
- Prefijo loc- para diálogo de ubicaciones.

Esto reduce fragilidad al evitar selectores por texto o estructura CSS.

## Estructura del proyecto

```text
src/
	app/
		core/
			guards/
			interceptors/
			models/
			services/
		features/
			calculation/
			home/
			locations/
			quote-creation/
			summary/
		app.config.ts
		app.routes.ts
	environments/
	index.html
	main.ts
	styles.scss
```

## Generación de build de producción

```bash
npm run build
```

Salida:

- dist/cotizador-danos-web

Notas:

- Existen budgets configurados en angular.json para tamaño de bundle y estilos.
- El proyecto puede mostrar warnings por budgets o dependencias CommonJS de bibliotecas PDF.

## Troubleshooting

1. Error de conexión con APIs

- Verifica que los servicios backend estén arriba en puertos 8080 y 8000.
- Confirma apiUrl y coreApiUrl en environment.

2. Problemas de autenticación

- Verifica requireApiKey y apiKey.
- Revisa en Network qué header espera el backend.

3. Pruebas sin cobertura

- Usa bandera --coverage (no --code-coverage).
- Si falta provider, instala dependencia:

```bash
npm install -D @vitest/coverage-v8
```

4. Warnings de build por CommonJS

- Actualmente no bloquean compilación.
- Revisar dependencias relacionadas con generación de PDF si se desea optimización adicional.

## Buenas prácticas para contribuir

- Mantener componentes pequeños y enfocados.
- Escribir o actualizar pruebas unitarias junto con cambios funcionales.
- Validar flujo principal manualmente y con tests.
- Evitar cambios destructivos en configuración global sin consenso.
- Documentar cualquier cambio de contrato con APIs.

---

Si vas a levantar este proyecto por primera vez, empieza por:

1. npm install
2. Verificar environments
3. Levantar APIs backend
4. npm start
5. npm run test -- --watch=false --coverage
