param()
# Comprehensive endpoint testing with logical flow and error handling

$base = "http://localhost:8080"
$apiKey = "your-api-key-change-in-production"
$headers = @{
    'Content-Type' = 'application/json'
    'x-api-key' = $apiKey
    'Authorization' = "ApiKey $apiKey"
}

$results = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        [int[]]$ExpectedStatus = @(200),
        [string]$Description = ""
    )
    
    Write-Host "`n" + ("="*80) -ForegroundColor Cyan
    Write-Host "TEST: $Name" -ForegroundColor Yellow
    Write-Host "DESC: $Description" -ForegroundColor Gray
    Write-Host "METH: $Method $Uri" -ForegroundColor Gray
    if ($Body) {
        $bodyStr = ($Body | ConvertTo-Json -Depth 5) -replace '\s+', ' '
        Write-Host "BODY: $bodyStr" -ForegroundColor Gray
    }
    
    $testResult = @{
        Name = $Name
        Method = $Method
        Uri = $Uri
        Status = $null
        StatusCode = $null
        Response = $null
        Error = $null
        Expected = ($ExpectedStatus -join ',')
        Success = $false
    }
    
    try {
        $webRequest = @{
            Uri = $Uri
            Method = $Method
            Headers = $headers
            UseBasicParsing = $true
        }
        
        if ($Body) {
            $webRequest.Body = ($Body | ConvertTo-Json -Depth 10)
        }
        
        $response = Invoke-WebRequest @webRequest -ErrorAction Stop
        $testResult.StatusCode = $response.StatusCode
        $testResult.Status = "[OK] SUCCESS"
        $testResult.Response = $response.Content | ConvertFrom-Json -Depth 10
        $testResult.Success = $ExpectedStatus -contains $response.StatusCode
        
    } catch {
        $response = $_.Exception.Response
        if ($response -ne $null) {
            $testResult.StatusCode = [int]$response.StatusCode
            $sr = New-Object System.IO.StreamReader($response.GetResponseStream())
            $testResult.Response = $sr.ReadToEnd()
            try {
                $testResult.Response = $testResult.Response | ConvertFrom-Json -Depth 10
            } catch {
                # Response not JSON, keep as string
            }
            $sr.Close()
            
            if ($ExpectedStatus -contains $testResult.StatusCode) {
                $testResult.Status = "[OK] SUCCESS (Expected Error)"
                $testResult.Success = $true
            } else {
                $testResult.Status = "[FAIL] FAILED"
                $testResult.Error = "Unexpected status code"
            }
        } else {
            $testResult.Status = "[ERROR] ERROR"
            $testResult.Error = $_.Exception.Message
        }
    }
    
    $successColor = if ($testResult.Success) { 'Green' } else { 'Red' }
    Write-Host "STAT: $($testResult.StatusCode)" -ForegroundColor $successColor
    Write-Host "RESULT: $($testResult.Status)" -ForegroundColor $successColor
    
    if ($testResult.Response) {
        $respDisplay = ($testResult.Response | ConvertTo-Json -Depth 3)
        if ($respDisplay -match '^\s*\{') {
            Write-Host "RESP: (object)" -ForegroundColor Gray
        } else {
            Write-Host "RESP: (string)" -ForegroundColor Gray
        }
    }
    
    if ($testResult.Error) {
        Write-Host "ERR: $($testResult.Error)" -ForegroundColor Red
    }
    
    $script:results += $testResult
    
    return $testResult.Response
}

# ============================================================================
# PHASE 1: FOLIO CREATION
# ============================================================================
Write-Host "`n`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 1: FOLIO AND QUOTE INITIALIZATION" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

$folioResp = Test-Endpoint `
    -Name "01-CREATE-FOLIO" `
    -Method "POST" `
    -Uri "$base/v1/folios" `
    -ExpectedStatus @(201) `
    -Description "Create initial folio for quote"

if (-not $folioResp) {
    Write-Host "`n`nFATAL: Could not create folio. Aborting." -ForegroundColor Red
    exit 1
}

$folio = $folioResp.numeroFolio
Write-Host "`n>>> FOLIO CREATED: $folio <<<`n" -ForegroundColor Cyan

# ============================================================================
# PHASE 2: GENERAL INFO MANAGEMENT
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 2: GENERAL INFORMATION MANAGEMENT" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

Test-Endpoint `
    -Name "02-GET-GENERAL-INFO" `
    -Method "GET" `
    -Uri "$base/v1/quotes/$folio/general-info" `
    -ExpectedStatus @(200) `
    -Description "Retrieve initial general info"

$genearlInfoBody = @{
    version = 0
    datosAsegurado = @{
        nombre = "Juan Pérez"
        apellido = "García"
        email = "juan@example.com"
        telefono = "5551234567"
        documentoId = "DNI123456789"
    }
    codigoAgente = "AGENT001"
}

$generalResp = Test-Endpoint `
    -Name "03-UPDATE-GENERAL-INFO" `
    -Method "PUT" `
    -Uri "$base/v1/quotes/$folio/general-info" `
    -Body $genearlInfoBody `
    -ExpectedStatus @(200) `
    -Description "Update insured and agent information"

$version = if ($generalResp) { $generalResp.version } else { 1 }

# ============================================================================
# PHASE 3: LOCATIONS MANAGEMENT - PREPARATION
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 3: LOCATIONS MANAGEMENT - CREATION & VALIDATION" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

Test-Endpoint `
    -Name "04-GET-LOCATIONS-EMPTY" `
    -Method "GET" `
    -Uri "$base/v1/quotes/$folio/locations" `
    -ExpectedStatus @(200) `
    -Description "Retrieve initial empty locations list"

# Create locations
$locationsBody = @{
    version = $version
    locations = @(
        @{
            indice = 0
            nombreUbicacion = "Oficina Principal"
            direccion = "Calle 123, Apartado 456"
            codigoPostal = "28001"
            giro = "COMERCIO"
            claveIncendio = "FIRE_KEY_001"
            buildingValue = 1200000
            contentsValue = 500000
            garantias = @()
        },
        @{
            indice = 1
            nombreUbicacion = "Almacén"
            direccion = "Ruta 45, Km 10"
            codigoPostal = "28002"
            giro = "ALMACENAMIENTO"
            claveIncendio = "FIRE_KEY_002"
            buildingValue = 800000
            contentsValue = 300000
            garantias = @()
        }
    )
}

$locationsResp = Test-Endpoint `
    -Name "05-PUT-LOCATIONS" `
    -Method "PUT" `
    -Uri "$base/v1/quotes/$folio/locations" `
    -Body $locationsBody `
    -ExpectedStatus @(200, 409) `
    -Description "Replace entire locations list with 2 locations"

$version = if ($locationsResp) { $locationsResp.version } else { $version }

# ============================================================================
# PHASE 4: LOCATION VALIDATION
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 4: LOCATION VALIDATION WITH ZIP CODE" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

# PATCH location 0 to add zip validation
$patchBody = @{
    version = $version
    codigoPostal = "28001"
    giro = "COMERCIO"
    claveIncendio = "FIRE_KEY_001"
}

$patchResp = Test-Endpoint `
    -Name "06-PATCH-LOCATION-0" `
    -Method "PATCH" `
    -Uri "$base/v1/quotes/$folio/locations/0" `
    -Body $patchBody `
    -ExpectedStatus @(200, 409) `
    -Description "Validate location 0 with zip code check"

$version = if ($patchResp) { $patchResp.version } else { $version }

# PATCH location 1
$patchBody2 = @{
    version = $version
    codigoPostal = "28002"
    giro = "ALMACENAMIENTO"
    claveIncendio = "FIRE_KEY_002"
}

$patchResp2 = Test-Endpoint `
    -Name "07-PATCH-LOCATION-1" `
    -Method "PATCH" `
    -Uri "$base/v1/quotes/$folio/locations/1" `
    -Body $patchBody2 `
    -ExpectedStatus @(200, 409) `
    -Description "Validate location 1 with zip code check"

$version = if ($patchResp2) { $patchResp2.version } else { $version }

# ============================================================================
# PHASE 5: UI LAYOUT CONFIGURATION
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 5: UI LAYOUT CONFIGURATION" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

Test-Endpoint `
    -Name "08-GET-LAYOUT" `
    -Method "GET" `
    -Uri "$base/v1/quotes/$folio/locations/layout" `
    -ExpectedStatus @(200) `
    -Description "Retrieve current layout configuration"

$layoutBody = @{
    version = $version
    configuracionLayout = @{
        active = "map"
        collapsed = @("risk", "notes")
        columns = 2
    }
}

$layoutResp = Test-Endpoint `
    -Name "09-PUT-LAYOUT" `
    -Method "PUT" `
    -Uri "$base/v1/quotes/$folio/locations/layout" `
    -Body $layoutBody `
    -ExpectedStatus @(200, 409) `
    -Description "Update layout configuration"

$version = if ($layoutResp) { $layoutResp.version } else { $version }

# ============================================================================
# PHASE 6: LOCATIONS SUMMARY
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 6: LOCATIONS SUMMARY & VALIDATION STATUS" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

Test-Endpoint `
    -Name "10-GET-LOCATIONS-SUMMARY" `
    -Method "GET" `
    -Uri "$base/v1/quotes/$folio/locations/summary" `
    -ExpectedStatus @(200) `
    -Description "Get financial and validation summary for all locations"

# ============================================================================
# PHASE 7: COVERAGE OPTIONS MANAGEMENT
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 7: COVERAGE OPTIONS CONFIGURATION" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

Test-Endpoint `
    -Name "11-GET-COVERAGE-OPTIONS" `
    -Method "GET" `
    -Uri "$base/v1/quotes/$folio/coverage-options" `
    -ExpectedStatus @(200) `
    -Description "Retrieve available and selected coverage options"

$coverageBody = @{
    version = $version
    opcionesCobertura = @("INCENDIO", "CAT", "FHM")
}

$coverageResp = Test-Endpoint `
    -Name "12-PUT-COVERAGE-OPTIONS" `
    -Method "PUT" `
    -Uri "$base/v1/quotes/$folio/coverage-options" `
    -Body $coverageBody `
    -ExpectedStatus @(200, 409) `
    -Description "Update selected coverage options"

$version = if ($coverageResp) { $coverageResp.version } else { $version }

# ============================================================================
# PHASE 8: PREMIUM CALCULATION
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 8: QUOTE CALCULATION" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

$calcBody = @{
    version = $version
    calculationParameters = @{
        incluirSobretasa = $true
        aplicarDescuentos = $true
    }
}

$calcResp = Test-Endpoint `
    -Name "13-POST-CALCULATE" `
    -Method "POST" `
    -Uri "$base/v1/quotes/$folio/calculate" `
    -Body $calcBody `
    -ExpectedStatus @(200, 409) `
    -Description "Trigger calculation engine and persist results"

$version = if ($calcResp) { $calcResp.version } else { $version }

# ============================================================================
# PHASE 9: STATE VERIFICATION
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 9: FINAL STATE VERIFICATION" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

Test-Endpoint `
    -Name "14-GET-STATE" `
    -Method "GET" `
    -Uri "$base/v1/quotes/$folio/state" `
    -ExpectedStatus @(200) `
    -Description "Verify final quote state"

Test-Endpoint `
    -Name "15-GET-LOCATIONS-FINAL" `
    -Method "GET" `
    -Uri "$base/v1/quotes/$folio/locations" `
    -ExpectedStatus @(200) `
    -Description "Retrieve final locations with all calculations"

# ============================================================================
# PHASE 10: CONCURRENCY & ERROR SCENARIOS
# ============================================================================
Write-Host "`n" + ("#"*80) -ForegroundColor Magenta
Write-Host "PHASE 10: ERROR SCENARIOS & CONCURRENCY CONTROL" -ForegroundColor Magenta
Write-Host ("#"*80) + "`n" -ForegroundColor Magenta

# Test stale version
$staleLayoutBody = @{
    version = 0
    configuracionLayout = @{
        active = "list"
        collapsed = @()
        columns = 1
    }
}

Test-Endpoint `
    -Name "16-PUT-LAYOUT-STALE" `
    -Method "PUT" `
    -Uri "$base/v1/quotes/$folio/locations/layout" `
    -Body $staleLayoutBody `
    -ExpectedStatus @(409) `
    -Description "Attempt update with stale version (should conflict)"

# Test invalid folio
Test-Endpoint `
    -Name "17-GET-INVALID-FOLIO" `
    -Method "GET" `
    -Uri "$base/v1/quotes/INVALID-FOLIO/state" `
    -ExpectedStatus @(404, 500) `
    -Description "Query with non-existent folio"

# Test invalid location index
$invalidPatchBody = @{
    version = $version
    codigoPostal = "99999"
    giro = "OTROS"
}

Test-Endpoint `
    -Name "18-PATCH-INVALID-INDEX" `
    -Method "PATCH" `
    -Uri "$base/v1/quotes/$folio/locations/999" `
    -Body $invalidPatchBody `
    -ExpectedStatus @(400, 404, 500) `
    -Description "Patch location with invalid index"

# ============================================================================
# SUMMARY REPORT
# ============================================================================
Write-Host "`n`n" + ("="*80) -ForegroundColor White
Write-Host "COMPREHENSIVE TEST SUMMARY" -ForegroundColor White
Write-Host ("="*80) + "`n" -ForegroundColor White

$totalTests = $results.Count
$successTests = ($results | Where-Object { $_.Success }).Count
$failedTests = $totalTests - $successTests
$percentage = if ($totalTests -gt 0) { [math]::Round(($successTests / $totalTests) * 100, 2) } else { 0 }

Write-Host "TOTAL TESTS: $totalTests" -ForegroundColor Cyan
Write-Host "PASSED: $successTests" -ForegroundColor Green
Write-Host "FAILED: $failedTests" -ForegroundColor $(if ($failedTests -gt 0) { 'Red' } else { 'Green' })
Write-Host "SUCCESS RATE: $percentage%" -ForegroundColor $(if ($percentage -ge 80) { 'Green' } else { 'Yellow' })

Write-Host "`n" + ("-"*80) -ForegroundColor Gray
Write-Host "DETAILED RESULTS:" -ForegroundColor White

$results | ForEach-Object {
    $statusColor = if ($_.Success) { 'Green' } else { 'Red' }
    $statusSymbol = if ($_.Success) { '[OK]' } else { '[FAIL]' }
    Write-Host "$statusSymbol [$($_.StatusCode)] $($_.Name)" -ForegroundColor $statusColor -NoNewline
    Write-Host " (Expected: $($_.Expected))" -ForegroundColor Gray
    
    if ($_.Error) {
        Write-Host "  ERROR: $($_.Error)" -ForegroundColor Red
    }
}

Write-Host "`n" + ("-"*80) -ForegroundColor Gray

# Report issues
$failedResults = $results | Where-Object { -not $_.Success }
if ($failedResults.Count -gt 0) {
    Write-Host "`nFAILED ENDPOINTS REQUIRING ATTENTION:" -ForegroundColor Red
    $failedResults | ForEach-Object {
        Write-Host "  [ERROR] $($_.Name)" -ForegroundColor Red
        Write-Host "     Method: $($_.Method) $($_.Uri)" -ForegroundColor Gray
        Write-Host "     Status: $($_.StatusCode) (Expected: $($_.Expected))" -ForegroundColor Yellow
        if ($_.Error) {
            Write-Host "     Error: $($_.Error)" -ForegroundColor Red
        }
        Write-Host ""
    }
}

# Export results to JSON
$reportPath = "$PSScriptRoot/test-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
$results | ConvertTo-Json -Depth 10 | Out-File -FilePath $reportPath
Write-Host "Report saved to: $reportPath" -ForegroundColor Cyan

Write-Host "`n" + ("="*80) -ForegroundColor White
