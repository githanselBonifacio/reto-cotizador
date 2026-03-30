"""Tests for serializers module"""

import pytest
from bson import ObjectId
from app.core.serializers import (
    serialize_mongo_value,
    serialize_mongo_document,
    serialize_mongo_documents,
)


class TestSerializeMongo:
    """Test serialization of MongoDB values"""

    def test_serialize_object_id(self):
        """Test ObjectId serialization to string"""
        obj_id = ObjectId("507f1f77bcf86cd799439011")
        result = serialize_mongo_value(obj_id)
        assert result == "507f1f77bcf86cd799439011"
        assert isinstance(result, str)

    def test_serialize_primitive_types(self):
        """Test serialization of primitive types (should remain unchanged)"""
        assert serialize_mongo_value("test") == "test"
        assert serialize_mongo_value(42) == 42
        assert serialize_mongo_value(3.14) == 3.14
        assert serialize_mongo_value(True) is True
        assert serialize_mongo_value(None) is None

    def test_serialize_list_with_object_ids(self):
        """Test serialization of list containing ObjectIds"""
        obj_id = ObjectId("507f1f77bcf86cd799439011")
        result = serialize_mongo_value([obj_id, "test", 42])
        assert result == ["507f1f77bcf86cd799439011", "test", 42]

    def test_serialize_nested_dict_with_object_ids(self):
        """Test serialization of nested dict with ObjectIds"""
        obj_id = ObjectId("507f1f77bcf86cd799439011")
        data = {
            "_id": obj_id,
            "name": "Test",
            "nested": {"id": obj_id, "value": 42},
        }
        result = serialize_mongo_value(data)
        assert result["_id"] == "507f1f77bcf86cd799439011"
        assert result["nested"]["id"] == "507f1f77bcf86cd799439011"
        assert result["nested"]["value"] == 42

    def test_serialize_mongo_document_with_none(self):
        """Test serialization of None document"""
        result = serialize_mongo_document(None)
        assert result is None

    def test_serialize_mongo_document_with_object_id(self):
        """Test serialization of MongoDB document"""
        obj_id = ObjectId("507f1f77bcf86cd799439011")
        doc = {"_id": obj_id, "name": "Test"}
        result = serialize_mongo_document(doc)
        assert result["_id"] == "507f1f77bcf86cd799439011"
        assert result["name"] == "Test"

    def test_serialize_mongo_documents(self):
        """Test serialization of list of MongoDB documents"""
        obj_id1 = ObjectId("507f1f77bcf86cd799439011")
        obj_id2 = ObjectId("507f1f77bcf86cd799439012")
        docs = [
            {"_id": obj_id1, "name": "Test 1"},
            {"_id": obj_id2, "name": "Test 2"},
        ]
        result = serialize_mongo_documents(docs)
        assert len(result) == 2
        assert result[0]["_id"] == "507f1f77bcf86cd799439011"
        assert result[1]["_id"] == "507f1f77bcf86cd799439012"

    def test_serialize_mongo_documents_empty_list(self):
        """Test serialization of empty list"""
        result = serialize_mongo_documents([])
        assert result == []

    def test_serialize_complex_nested_structure(self):
        """Test serialization of complex nested structure"""
        obj_id = ObjectId("507f1f77bcf86cd799439011")
        data = {
            "_id": obj_id,
            "items": [
                {"id": obj_id, "value": 1},
                {"id": ObjectId("507f1f77bcf86cd799439012"), "value": 2},
            ],
            "metadata": {"created_id": obj_id},
        }
        result = serialize_mongo_value(data)
        assert result["_id"] == "507f1f77bcf86cd799439011"
        assert result["items"][0]["id"] == "507f1f77bcf86cd799439011"
        assert result["items"][1]["id"] == "507f1f77bcf86cd799439012"
        assert result["metadata"]["created_id"] == "507f1f77bcf86cd799439011"

