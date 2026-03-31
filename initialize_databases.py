#!/usr/bin/env python3
"""
Automatic database initialization script for Docker startup.
This script is run by the db-init service and ensures both MongoDB databases
are properly seeded with initial data.

Designed to run once per Docker Compose session with idempotent operations.
"""

import asyncio
import sys
import os
from pathlib import Path
from typing import Optional
import time
import logging

# Configure logging for Docker
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


def print_banner(title: str):
    """Print a formatted banner."""
    print("\n" + "=" * 80)
    print("  " + title.center(76) + "  ")
    print("=" * 80)


def print_section(title: str):
    """Print a section header."""
    print(f"\n{'─' * 80}")
    print(f"  {title}")
    print(f"{'─' * 80}\n")


async def run_fastapi_seed():
    """Run FastAPI database seeding."""
    logger.info("Starting FastAPI (plataforma-core-ohs) database seeding...")
    
    try:
        # Import and run FastAPI seed
        sys.path.insert(0, '/app')
        
        # Try to import seed_fastapi
        try:
            from seed_fastapi import seed_database as fastapi_seed
        except ImportError:
            logger.warning("Could not directly import, attempting via subprocess...")
            import subprocess
            result = subprocess.run(
                [sys.executable, '/app/seed_fastapi.py'],
                capture_output=True,
                text=True,
                timeout=120
            )
            if result.returncode == 0:
                print(result.stdout)
                logger.info("✅ FastAPI seeding completed successfully")
                return True
            else:
                print(result.stderr)
                logger.error("❌ FastAPI seeding failed")
                return False
        
        # Run the seed function
        await fastapi_seed()
        logger.info("✅ FastAPI seeding completed successfully")
        return True
        
    except Exception as e:
        logger.error(f"❌ FastAPI seeding failed: {e}")
        return False


async def run_spring_seed():
    """Run Spring Boot database seeding."""
    logger.info("Starting Spring Boot (cotizador-danos-back) database seeding...")
    
    try:
        # Import and run Spring Boot seed
        sys.path.insert(0, '/app')
        spring_database = os.getenv('SPRING_DATABASE', 'PLATAFORMA_DANOS')
        previous_database_name = os.getenv('DATABASE_NAME')
        os.environ['DATABASE_NAME'] = spring_database
        
        # Try to import seed_spring
        try:
            from seed_spring import seed_database as spring_seed
        except ImportError:
            logger.warning("Could not directly import, attempting via subprocess...")
            import subprocess
            result = subprocess.run(
                [sys.executable, '/app/seed_spring.py'],
                capture_output=True,
                text=True,
                timeout=120
            )
            if result.returncode == 0:
                print(result.stdout)
                logger.info("✅ Spring Boot seeding completed successfully")
                return True
            else:
                print(result.stderr)
                logger.error("❌ Spring Boot seeding failed")
                return False
        
        # Run the seed function
        await spring_seed()
        logger.info("✅ Spring Boot seeding completed successfully")
        return True
        
    except Exception as e:
        logger.error(f"❌ Spring Boot seeding failed: {e}")
        return False
    finally:
        if previous_database_name is None:
            os.environ.pop('DATABASE_NAME', None)
        else:
            os.environ['DATABASE_NAME'] = previous_database_name


async def check_mongodb_connectivity(max_retries: int = 10, retry_delay: int = 3) -> bool:
    """Check MongoDB connectivity with retries."""
    logger.info(f"Checking MongoDB connectivity (max {max_retries} retries, {retry_delay}s delay)...")
    
    try:
        from motor.motor_asyncio import AsyncIOMotorClient
    except ImportError:
        logger.error("motor package not installed")
        return False
    
    mongodb_uri = os.getenv('MONGODB_URI', 'mongodb://mongodb:27017')
    
    for attempt in range(1, max_retries + 1):
        try:
            client = AsyncIOMotorClient(mongodb_uri, serverSelectionTimeoutMS=5000)
            await client.admin.command('ping')
            logger.info(f"✅ MongoDB connection successful (attempt {attempt}/{max_retries})")
            client.close()
            return True
        except Exception as e:
            if attempt < max_retries:
                logger.warning(f"⏳ MongoDB not ready (attempt {attempt}/{max_retries}): {e}")
                await asyncio.sleep(retry_delay)
            else:
                logger.error(f"❌ MongoDB connection failed after {max_retries} attempts")
                return False
    
    return False


async def initialize_databases():
    """Main initialization procedure."""
    print_banner("MongoDB DATABASE INITIALIZATION")
    logger.info("Starting automatic database initialization...")
    
    # Set environment variables
    os.environ.setdefault('MONGODB_URI', 'mongodb://mongodb:27017')
    os.environ.setdefault('DATABASE_NAME', 'CATALOGO_DANOS')
    
    print_section("SYSTEM CHECK")
    logger.info(f"Python version: {sys.version}")
    logger.info(f"MongoDB URI: {os.getenv('MONGODB_URI')}")
    logger.info(f"FastAPI DB: {os.getenv('DATABASE_NAME')}")
    logger.info(f"Spring DB: {os.getenv('SPRING_DATABASE', 'PLATAFORMA_DANOS')}")
    
    # Check MongoDB connectivity
    print_section("MONGODB CONNECTIVITY")
    if not await check_mongodb_connectivity(max_retries=15, retry_delay=2):
        logger.error("Could not connect to MongoDB after retries")
        return 1
    
    # Run seeds
    print_section("DATABASE SEEDING")
    
    results = {}
    
    # Seed FastAPI
    logger.info("=" * 80)
    logger.info("FastAPI (plataforma-core-ohs) - CATALOGO_DANOS")
    logger.info("=" * 80)
    results['fastapi'] = await run_fastapi_seed()
    
    # Seed Spring Boot
    logger.info("\n" + "=" * 80)
    logger.info("Spring Boot (cotizador-danos-back) - PLATAFORMA_DANOS")
    logger.info("=" * 80)
    results['spring'] = await run_spring_seed()
    
    # Summary
    print_section("INITIALIZATION SUMMARY")
    
    total = len(results)
    success = sum(1 for v in results.values() if v)
    failed = total - success
    
    logger.info(f"Total services: {total}")
    logger.info(f"Successfully seeded: {success}")
    
    for service, status in results.items():
        status_icon = "✅" if status else "❌"
        status_text = "SEEDED" if status else "FAILED"
        logger.info(f"{status_icon} {service.upper()}: {status_text}")
    
    # Final result
    print_section("RESULT")
    
    if success == total:
        logger.info("🎉 All databases initialized successfully!")
        print_banner("INITIALIZATION COMPLETE - READY TO START SERVICES")
        return 0
    else:
        logger.error(f"⚠️  {failed} service(s) failed to initialize")
        print_banner("INITIALIZATION INCOMPLETE - MANUAL INTERVENTION NEEDED")
        return 1


async def main():
    """Main entry point."""
    try:
        exit_code = await initialize_databases()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        logger.warning("\nInitialization interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Unexpected error during initialization: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
