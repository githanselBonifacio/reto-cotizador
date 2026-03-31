"""MongoDB database initialization and idempotent seed script for cotizador-danos-back."""

import asyncio
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from decimal import Decimal

from dotenv import load_dotenv
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase

# Load environment from parent directory's .env if exists
ENV_PATH = Path(__file__).resolve().parent.parent / ".env"
if ENV_PATH.exists():
    load_dotenv(ENV_PATH)

# Try to load from docker environment
mongodb_uri = (
    Path(__file__).resolve().parent.parent.parent / ".env"
)

if mongodb_uri.exists():
    load_dotenv(mongodb_uri)


async def ensure_database_exists(client: AsyncIOMotorClient, database_name: str) -> AsyncIOMotorDatabase:
    """Ensure the target database exists and return its instance."""
    database_names = await client.list_database_names()
    database = client[database_name]

    if database_name in database_names:
        print(f"✓ Database already exists: {database_name}")
        return database

    print(f"ℹ Database does not exist yet: {database_name}")
    print("ℹ Database will be created automatically when the first collection is created")
    return database


async def ensure_collections_exist(database: AsyncIOMotorDatabase, collection_names: list[str]) -> None:
    """Create collections only when they do not exist yet."""
    existing_collections = set(await database.list_collection_names())

    for collection_name in collection_names:
        if collection_name in existing_collections:
            print(f"✓ Collection already exists: {collection_name}")
            continue

        await database.create_collection(collection_name)
        print(f"✓ Collection created: {collection_name}")


async def ensure_indexes(database: AsyncIOMotorDatabase) -> None:
    """Create required indexes for all collections."""
    existing_indexes = await database.quotes.index_information()
    if "numeroFolio_1" in existing_indexes:
        await database.quotes.drop_index("numeroFolio_1")
        print("✓ Removed legacy conflicting index: numeroFolio_1")

    await database.quotes.create_index("estadoCotizacion")
    await database.quotes.create_index("fechaUltimaActualizacion", expireAfterSeconds=2592000)  # 30 days
    print("✓ Indexes ensured for quotes collection")


async def upsert_many(
    database: AsyncIOMotorDatabase,
    collection_name: str,
    documents: list[dict[str, Any]],
    unique_field: str,
) -> None:
    """Insert or update seed data without duplicating records."""
    collection = database[collection_name]
    upserted_count = 0

    for document in documents:
        filter_query = {unique_field: document[unique_field]}
        update_fields = dict(document)
        update_document = {
            "$set": update_fields,
        }
        result = await collection.update_one(filter_query, update_document, upsert=True)
        if result.upserted_id or result.modified_count:
            upserted_count += 1

    print(f"✓ Seed ensured for {collection_name}: {len(documents)} records processed, {upserted_count} inserted/updated")


async def seed_database() -> None:
    """Create database, collections, indexes, and seed MongoDB data for Spring Boot app."""
    mongodb_uri = "mongodb://mongodb:27017"
    database_name = "PLATAFORMA_DANOS"

    # Try to get from environment
    import os
    mongodb_uri = os.getenv("MONGODB_URI", mongodb_uri)
    database_name = os.getenv("DATABASE_NAME", database_name)

    client = AsyncIOMotorClient(mongodb_uri)

    try:
        await client.admin.command("ping")
        print(f"✓ Connected to MongoDB: {mongodb_uri}")
        print(f"🌱 Starting database validation and seeding for: {database_name}")

        database = await ensure_database_exists(client, database_name)

        collection_names = [
            "quotes",
        ]
        await ensure_collections_exist(database, collection_names)
        await ensure_indexes(database)

        now = datetime.now(timezone.utc)

        # Example quotes - Simulated data for demonstration
        quotes = [
            {
            "_id": "FOL-2026-001",
                "numeroFolio": "FOL-2026-001",
                "estadoCotizacion": "PENDING",
                "datosAsegurado": {
                    "nombre": "Empresa XYZ S.A.",
                    "rfc": "EXY880101ABC",
                },
                "datosConduccion": {
                    "codigoAgente": "AG001",
                },
                "locations": [
                    {
                        "indice": 0,
                        "nombre": "Almacén Principal",
                        "calle": "Avenida Paseo de la Reforma",
                        "numero": 505,
                        "codigoPostal": "01500",
                        "ciudad": "Mexico City",
                        "estado": "CDMX",
                        "pais": "MX",
                    }
                ],
                "configuracionLayout": {
                    "tipoNegocio": "Almacenamiento",
                    "materialConstruccion": "Concreto",
                    "sistemaSprinklers": True,
                },
                "opcionesCobertura": ["INCENDIO", "CAT", "FHM"],
                "primaNeta": 15000.50,
                "primaComercial": 18750.62,
                "primasPorUbicacion": [
                    {
                        "indice": 0,
                        "nombreUbicacion": "Almacén Principal",
                        "incendio": 8000.25,
                        "cat": 4500.00,
                        "fhm": 2500.25,
                        "primaNeta": 15000.50,
                        "alertasBloqueantes": [],
                    }
                ],
                "version": 1,
                "fechaUltimaActualizacion": now,
            },
            {
                "_id": "FOL-2026-002",
                "numeroFolio": "FOL-2026-002",
                "estadoCotizacion": "CALCULADO",
                "datosAsegurado": {
                    "nombre": "Retail Solutions Ltd.",
                    "rfc": "RSL900515XYZ",
                },
                "datosConduccion": {
                    "codigoAgente": "AG002",
                },
                "locations": [
                    {
                        "indice": 0,
                        "nombre": "Tienda Metropolitana",
                        "calle": "Boulevard Santa Fe",
                        "numero": 385,
                        "codigoPostal": "01219",
                        "ciudad": "Mexico City",
                        "estado": "CDMX",
                        "pais": "MX",
                    }
                ],
                "configuracionLayout": {
                    "tipoNegocio": "Comercio Minorista",
                    "materialConstruccion": "Acero y Vidrio",
                    "sistemaSprinklers": True,
                },
                "opcionesCobertura": ["INCENDIO", "FHM"],
                "primaNeta": 25000.00,
                "primaComercial": 31250.00,
                "primasPorUbicacion": [
                    {
                        "indice": 0,
                        "nombreUbicacion": "Tienda Metropolitana",
                        "incendio": 18000.00,
                        "cat": 0.00,
                        "fhm": 7000.00,
                        "primaNeta": 25000.00,
                        "alertasBloqueantes": [],
                    }
                ],
                "version": 1,
                "fechaUltimaActualizacion": now,
            },
        ]
        await upsert_many(database, "quotes", quotes, "_id")

        print("✅ Database validation and seeding completed successfully for Spring Boot!")
    except Exception as error:
        print(f"❌ Error validating or seeding database: {error}")
        raise
    finally:
        client.close()


if __name__ == "__main__":
    import os

    # Check if MONGODB_URI is available
    if "MONGODB_URI" not in os.environ:
        print("⚠️  MONGODB_URI not set, using default: mongodb://mongodb:27017")
        os.environ["MONGODB_URI"] = "mongodb://mongodb:27017"

    if "DATABASE_NAME" not in os.environ:
        os.environ["DATABASE_NAME"] = "PLATAFORMA_DANOS"

    asyncio.run(seed_database())
