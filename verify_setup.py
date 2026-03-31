#!/usr/bin/env python3
"""
Quick verification script that checks if all auto-initialization components are in place.
"""

import os
import sys
from pathlib import Path


def check_file_exists(path: Path, description: str) -> bool:
    """Check if a file exists and report status."""
    exists = path.exists()
    icon = "✅" if exists else "❌"
    print(f"{icon} {description}")
    if not exists:
        print(f"   Expected: {path}")
    return exists


def main():
    """Main verification function."""
    root = Path(__file__).resolve().parent
    os.chdir(root)
    
    print("\n" + "=" * 80)
    print("  VERIFICATION OF AUTO-INITIALIZATION SETUP")
    print("=" * 80 + "\n")
    
    all_ok = True
    
    print("📁 Checking required files...\n")
    
    checks = [
        (root / "docker-compose.yml", "docker-compose.yml (with db-init service)"),
        (root / "Dockerfile.init", "Dockerfile.init (init service image)"),
        (root / "initialize_databases.py", "initialize_databases.py (orchestrator script)"),
        (root / "plataforma-core-ohs" / "scripts" / "seed_database.py", "FastAPI seed script"),
        (root / "cotizador-danos-back" / "scripts" / "seed_database.py", "Spring Boot seed script"),
        (root / "AUTOMATIC_INITIALIZATION.md", "AUTOMATIC_INITIALIZATION.md (documentation)"),
    ]
    
    for path, description in checks:
        all_ok = check_file_exists(path, description) and all_ok
    
    print("\n🔍 Checking docker-compose.yml configuration...\n")
    
    try:
        import yaml
        has_yaml = True
    except ImportError:
        print("⚠️  PyYAML not installed - skipping deep validation")
        print("   Run: pip install pyyaml")
        has_yaml = False
    
    if has_yaml:
        try:
            with open(root / "docker-compose.yml") as f:
                config = yaml.safe_load(f)
            
            # Check db-init service
            if "services" in config and "db-init" in config["services"]:
                print("✅ db-init service configured")
                db_init = config["services"]["db-init"]
                
                # Check dependencies
                deps = ["mongodb", "plataforma-core-ohs", "cotizador-danos-back", "cotizador-danos-web"]
                for service in deps:
                    if service in config["services"]:
                        service_config = config["services"][service]
                        if "depends_on" in service_config and "db-init" in service_config["depends_on"]:
                            print(f"✅ {service} depends on db-init")
                        else:
                            print(f"⚠️  {service} does NOT depend on db-init")
                            if service != "mongodb":
                                all_ok = False
            else:
                print("❌ db-init service NOT found in docker-compose.yml")
                all_ok = False
        except Exception as e:
            print(f"❌ Error parsing docker-compose.yml: {e}")
            all_ok = False
    
    print("\n📦 Checking Python dependencies...\n")
    
    deps = {
        "motor": "Motor (async MongoDB driver)",
        "pymongo": "PyMongo (MongoDB driver)",
        "dotenv": "python-dotenv (environment loader)",
    }
    
    for module, description in deps.items():
        try:
            __import__(module)
            print(f"✅ {description}")
        except ImportError:
            print(f"❌ {description} - NOT INSTALLED")
            print(f"   Run: pip install {module}")
            all_ok = False
    
    print("\n🐳 Docker configuration...\n")
    
    try:
        import subprocess
        result = subprocess.run(
            ["docker", "--version"],
            capture_output=True,
            text=True,
            timeout=5
        )
        if result.returncode == 0:
            print(f"✅ Docker: {result.stdout.strip()}")
        else:
            print("❌ Docker not working properly")
            all_ok = False
    except Exception as e:
        print(f"❌ Docker error: {e}")
        all_ok = False
    
    print("\n" + "=" * 80)
    if all_ok:
        print("  ✅ ALL CHECKS PASSED - READY TO USE AUTO-INITIALIZATION")
        print("\n  Next step: docker compose up")
    else:
        print("  ⚠️  SOME CHECKS FAILED - SEE ABOVE FOR DETAILS")
        print("\n  Please fix the issues above before running docker compose up")
    print("=" * 80 + "\n")
    
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
