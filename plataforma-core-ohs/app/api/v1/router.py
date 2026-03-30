# API v1 Router - Main API aggregator
from fastapi import APIRouter
from app.api.v1.endpoints import subscribers, agents, business_lines, zip_codes, folios, catalogs, tariffs

router = APIRouter(prefix="/v1")

# Include all endpoint routers
router.include_router(subscribers.router)
router.include_router(agents.router)
router.include_router(business_lines.router)
router.include_router(zip_codes.router)
router.include_router(folios.router)
router.include_router(catalogs.router)
router.include_router(tariffs.router)

