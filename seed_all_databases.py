#!/usr/bin/env python3
"""
Master seeding script for both FastAPI and Spring Boot MongoDB databases.
This script ensures both databases are properly initialized with seed data.

Usage:
    python seed_all_databases.py
    
    Environment variables:
    - MONGODB_URI: MongoDB connection string (default: mongodb://mongodb:27017)
    - DEBUG: Enable verbose output (default: false)
"""

import asyncio
import subprocess
import sys
from pathlib import Path
import os
import time


def print_section(title: str):
    """Print a formatted section header."""
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)


def print_status(message: str, status: str = "info"):
    """Print a status message with icon."""
    icons = {
        "info": "ℹ️ ",
        "success": "✅",
        "warning": "⚠️ ",
        "error": "❌",
    }
    icon = icons.get(status, "•")
    print(f"{icon} {message}")


async def run_seed_script(script_path: Path, app_name: str) -> bool:
    """Run a seed script asynchronously."""
    if not script_path.exists():
        print_status(f"Script not found: {script_path}", "warning")
        return False

    print_status(f"Starting seed process for {app_name}...", "info")

    try:
        process = await asyncio.create_subprocess_exec(
            sys.executable,
            str(script_path),
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
            env={**os.environ},
        )

        stdout, stderr = await process.communicate()

        if process.returncode == 0:
            output = stdout.decode()
            print(output)
            print_status(f"{app_name} seeding completed successfully!", "success")
            return True
        else:
            error = stderr.decode()
            print_status(f"{app_name} seeding failed!", "error")
            print(error)
            return False
    except Exception as e:
        print_status(f"Error running {app_name} seed script: {e}", "error")
        return False


async def seed_all_databases():
    """Execute seeding for both FastAPI and Spring Boot."""
    workspace_root = Path(__file__).resolve().parent

    print_section("🌱 MONGODB DATABASE SEEDING - MULTI-APPLICATION")
    print_status("Workspace: " + str(workspace_root), "info")

    # Paths to seed scripts
    fastapi_seed = workspace_root / "plataforma-core-ohs" / "scripts" / "seed_database.py"
    spring_seed = workspace_root / "cotizador-danos-back" / "scripts" / "seed_database.py"

    print_section("📋 SEED SCRIPTS VERIFICATION")
    print_status(f"FastAPI seed script: {'✓ Found' if fastapi_seed.exists() else '✗ NOT FOUND'}", "info")
    print_status(f"Spring Boot seed script: {'✓ Found' if spring_seed.exists() else '✗ NOT FOUND'}", "info")

    results = {}

    # Run both seeds sequentially (safer for MongoDB)
    print_section("🔄 SEEDING FASTAPI DATABASE (plataforma-core-ohs)")
    results["fastapi"] = await run_seed_script(fastapi_seed, "FastAPI (plataforma-core-ohs)")

    print_section("🔄 SEEDING SPRING BOOT DATABASE (cotizador-danos-back)")
    results["spring"] = await run_seed_script(spring_seed, "Spring Boot (cotizador-danos-back)")

    # Summary
    print_section("📊 SEEDING SUMMARY")
    total = len(results)
    success = sum(1 for v in results.values() if v)
    failed = total - success

    print_status(f"Total applications: {total}", "info")
    print_status(f"Successfully seeded: {success}", "success" if success == total else "warning")
    if failed > 0:
        print_status(f"Failed: {failed}", "error")

    for app, status in results.items():
        status_text = "✓" if status else "✗"
        print(f"  {status_text} {app.upper()}: {'SEEDED' if status else 'FAILED'}")

    # Final status
    print_section("RESULT")
    if success == total:
        print_status("All databases seeded successfully! 🎉", "success")
        return 0
    else:
        print_status(f"Some databases failed to seed. Please check errors above.", "error")
        return 1


def main():
    """Main entry point."""
    # Check MongoDB connectivity first
    print_section("🔍 ENVIRONMENT CHECK")

    # Check Python version
    print_status(f"Python version: {sys.version.split()[0]}", "info")

    # Check required packages
    try:
        import motor
        import dotenv
        print_status("Required packages (motor, python-dotenv): ✓ Installed", "success")
    except ImportError as e:
        print_status(f"Missing required package: {e}", "error")
        print_status("Install with: pip install motor python-dotenv", "warning")
        return 1

    # Run async seeding
    try:
        exit_code = asyncio.run(seed_all_databases())
        return exit_code
    except Exception as e:
        print_section("ERROR")
        print_status(f"Unexpected error: {e}", "error")
        return 1


if __name__ == "__main__":
    sys.exit(main())
