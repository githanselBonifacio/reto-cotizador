"""Tests for configuration and security"""

import pytest
from app.core.config import settings
from app.core.security import verify_api_key


class TestSettings:
    """Test application settings"""

    def test_settings_loaded(self):
        """Test that settings are loaded correctly"""
        assert settings.app_name == "Plataforma Core OHS"
        assert settings.app_version == "1.0.0"
        assert settings.database_name == "CATALOGO_DANOS"
        assert settings.algorithm == "HS256"

    def test_mongodb_connection_string(self):
        """Test MongoDB connection string property"""
        assert settings.mongodb_connection_string is not None
        assert "mongodb" in settings.mongodb_connection_string


class TestSecurity:
    """Test security functions"""

    @pytest.mark.asyncio
    async def test_verify_api_key_valid(self):
        """Test verifying valid API key"""
        # The actual key from settings
        result = await verify_api_key(settings.api_key)
        assert result == settings.api_key

    @pytest.mark.asyncio
    async def test_verify_api_key_invalid(self):
        """Test verifying invalid API key"""
        from fastapi import HTTPException

        with pytest.raises(HTTPException) as exc_info:
            await verify_api_key("invalid-key")
        assert exc_info.value.status_code == 403

