# MongoDB Serialization Helpers
from typing import Any

from bson import ObjectId


def serialize_mongo_value(value: Any) -> Any:
    """Convert MongoDB-specific values to JSON-friendly values."""
    if isinstance(value, ObjectId):
        return str(value)

    if isinstance(value, list):
        return [serialize_mongo_value(item) for item in value]

    if isinstance(value, dict):
        return {
            key: serialize_mongo_value(item)
            for key, item in value.items()
        }

    return value


def serialize_mongo_document(document: dict[str, Any] | None) -> dict[str, Any] | None:
    """Serialize a single MongoDB document."""
    if document is None:
        return None
    return serialize_mongo_value(document)


def serialize_mongo_documents(documents: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Serialize a list of MongoDB documents."""
    return [serialize_mongo_document(document) for document in documents]

