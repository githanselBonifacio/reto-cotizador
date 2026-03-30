"""MongoDB database initialization and idempotent seed script."""

import asyncio
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase

ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

load_dotenv(ROOT_DIR / ".env")

from app.core.config import settings


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


async def cleanup_legacy_temp_collection(database: AsyncIOMotorDatabase) -> None:
    """Remove legacy helper collection used by older seed versions."""
    collection_names = await database.list_collection_names()
    if "_db_init" not in collection_names:
        return

    document_count = await database["_db_init"].count_documents({})
    if document_count == 0:
        await database["_db_init"].drop()
        print("✓ Removed legacy temporary collection: _db_init")


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
    await database.subscribers.create_index("email", unique=True)
    await database.agents.create_index("agent_code", unique=True)
    await database.business_lines.create_index("code", unique=True)
    await database.zip_codes.create_index("zip_code", unique=True)
    await database.folios.create_index("collection_name", unique=True)
    await database.risk_classifications.create_index("code", unique=True)
    await database.guarantees.create_index("code", unique=True)
    await database.tariffs.create_index("code", unique=True)
    print("✓ Indexes ensured")


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
        created_at = update_fields.pop("created_at", datetime.now(timezone.utc))
        update_document = {
            "$set": update_fields,
            "$setOnInsert": {"created_at": created_at},
        }
        result = await collection.update_one(filter_query, update_document, upsert=True)
        if result.upserted_id or result.modified_count:
            upserted_count += 1

    print(f"✓ Seed ensured for {collection_name}: {len(documents)} records processed, {upserted_count} inserted/updated")


async def seed_database() -> None:
    """Create database, collections, indexes, and seed local MongoDB data."""
    client = AsyncIOMotorClient(settings.mongodb_connection_string)

    try:
        await client.admin.command("ping")
        print(f"✓ Connected to MongoDB: {settings.mongodb_connection_string}")
        print("🌱 Starting database validation and seeding...")

        database = await ensure_database_exists(client, settings.database_name)

        collection_names = [
            "subscribers",
            "agents",
            "business_lines",
            "zip_codes",
            "folios",
            "risk_classifications",
            "guarantees",
            "tariffs",
        ]
        await ensure_collections_exist(database, collection_names)
        await cleanup_legacy_temp_collection(database)
        await ensure_indexes(database)

        now = datetime.now(timezone.utc)
        expires_at = now + timedelta(days=365)

        subscribers = [
            {
                "name": "Acme Insurance Corp",
                "email": "contact@acme-ins.com",
                "phone": "+1-555-0101",
                "status": "active",
                "created_at": now,
                "updated_at": now,
            },
            {
                "name": "Global Risk Management",
                "email": "info@globalrisk.com",
                "phone": "+1-555-0102",
                "status": "active",
                "created_at": now,
                "updated_at": now,
            },
        ]
        await upsert_many(database, "subscribers", subscribers, "email")

        acme_subscriber = await database.subscribers.find_one({"email": "contact@acme-ins.com"})
        global_subscriber = await database.subscribers.find_one({"email": "info@globalrisk.com"})

        agents = [
            {
                "name": "John Carter",
                "email": "john.carter@acme-ins.com",
                "agent_code": "AG001",
                "subscriber_id": str(acme_subscriber["_id"]) if acme_subscriber else None,
                "status": "active",
                "created_at": now,
                "updated_at": now,
            },
            {
                "name": "Laura Mitchell",
                "email": "laura.mitchell@globalrisk.com",
                "agent_code": "AG002",
                "subscriber_id": str(global_subscriber["_id"]) if global_subscriber else None,
                "status": "active",
                "created_at": now,
                "updated_at": now,
            },
        ]
        await upsert_many(database, "agents", agents, "agent_code")

        business_lines = [
            {
                "code": "BL001",
                "name": "Fire Insurance",
                "description": "Coverage for fire and related damages",
                "status": "active",
                "created_at": now,
            },
            {
                "code": "BL002",
                "name": "Catastrophe Coverage",
                "description": "Protection against natural disasters",
                "status": "active",
                "created_at": now,
            },
            {
                "code": "BL003",
                "name": "All Risks Coverage",
                "description": "Comprehensive coverage for multiple risk types",
                "status": "active",
                "created_at": now,
            },
        ]
        await upsert_many(database, "business_lines", business_lines, "code")

        zip_codes = [
            {
                "zip_code": "01000",
                "city": "Mexico City",
                "state": "CDMX",
                "country": "MX",
                "risk_zone": "LOW",
                "status": "active",
                "created_at": now,
            },
            {
                "zip_code": "64000",
                "city": "Monterrey",
                "state": "NL",
                "country": "MX",
                "risk_zone": "MEDIUM",
                "status": "active",
                "created_at": now,
            },
        ]
        await upsert_many(database, "zip_codes", zip_codes, "zip_code")

        folios = [
            {
                "collection_name": "folios",
                "sequence_value": 0,
                "prefix": "FOL",
            }
        ]
        await upsert_many(database, "folios", folios, "collection_name")

        risk_classifications = [
            {
                "code": "RC001",
                "name": "Low Risk",
                "description": "Low probability of claims",
                "risk_level": "LOW",
            },
            {
                "code": "RC002",
                "name": "Medium Risk",
                "description": "Medium probability of claims",
                "risk_level": "MEDIUM",
            },
            {
                "code": "RC003",
                "name": "High Risk",
                "description": "High probability of claims",
                "risk_level": "HIGH",
            },
            {
                "code": "RC004",
                "name": "Critical Risk",
                "description": "Very high probability of claims",
                "risk_level": "CRITICAL",
            },
        ]
        await upsert_many(database, "risk_classifications", risk_classifications, "code")

        guarantees = [
            {
                "code": "GAR001",
                "name": "Fire Damage",
                "description": "Coverage for fire-related damages",
                "coverage_type": "FIRE",
                "coverage_amount": 1000000,
            },
            {
                "code": "GAR002",
                "name": "Theft Coverage",
                "description": "Protection against theft and robbery",
                "coverage_type": "THEFT",
                "coverage_amount": 500000,
            },
            {
                "code": "GAR003",
                "name": "Natural Disaster",
                "description": "Coverage for natural disasters",
                "coverage_type": "NATURAL_DISASTER",
                "coverage_amount": 2000000,
            },
        ]
        await upsert_many(database, "guarantees", guarantees, "code")

        tariffs = [
            {
                "code": "TARIFF001",
                "name": "Fire Factor - Standard",
                "factor_type": "INCENDIO",
                "base_rate": 0.015,
                "min_rate": 0.010,
                "max_rate": 0.025,
                "business_line_id": None,
                "effective_date": now,
                "expiration_date": expires_at,
                "status": "active",
            },
            {
                "code": "TARIFF002",
                "name": "CAT Factor - Standard",
                "factor_type": "CAT",
                "base_rate": 0.005,
                "min_rate": 0.003,
                "max_rate": 0.010,
                "business_line_id": None,
                "effective_date": now,
                "expiration_date": expires_at,
                "status": "active",
            },
            {
                "code": "TARIFF003",
                "name": "FHM Factor - Standard",
                "factor_type": "FHM",
                "base_rate": 0.008,
                "min_rate": 0.005,
                "max_rate": 0.015,
                "business_line_id": None,
                "effective_date": now,
                "expiration_date": expires_at,
                "status": "active",
            },
        ]
        await upsert_many(database, "tariffs", tariffs, "code")

        print("✅ Database validation and seeding completed successfully!")
    except Exception as error:
        print(f"❌ Error validating or seeding database: {error}")
        raise
    finally:
        client.close()


if __name__ == "__main__":
    asyncio.run(seed_database())

