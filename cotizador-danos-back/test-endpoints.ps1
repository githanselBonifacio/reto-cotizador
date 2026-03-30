param()

# Comprehensive endpoint testing script - Clean version
$base = "http://localhost:8080"
$headers = @{
    'Content-Type' = 'application/json'
}

$results = @()
$folio = $null
$version = 0

function RunTest {
    param([string]$name, [string]$method, [string]$uri, $body, [int[]]$expectedStatus)
    
    Write-Host "`n>>> TEST: $name" -ForegroundColor Cyan
    Write-Host "    $method $uri" -ForegroundColor Gray
    
    try {
        $req = @{
            Uri = $uri
            Method = $method
            Headers = $headers
            UseBasicParsing = $true
        }
        if ($body) {
            $req.Body = ($body | ConvertTo-Json)
        }
        
        $resp = Invoke-WebRequest @req -ErrorAction Stop
        $statusOk = $expectedStatus -contains $resp.StatusCode
        $respObj = $resp.Content | ConvertFrom-Json
        
        $result = @{ name=$name; method=$method; uri=$uri; status=$resp.StatusCode; ok=$statusOk; error=$null }
        $results += $result
        
        Write-Host "    Status: $($resp.StatusCode) [$(if($statusOk){'PASS'}else{'FAIL'})]" -ForegroundColor $(if($statusOk){'Green'}else{'Red'})
        
        return $respObj
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $status = [int]$resp.StatusCode
            $statusOk = $expectedStatus -contains $status
            $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $body = $sr.ReadToEnd()
            $sr.Close()
            
            $result = @{ name=$name; method=$method; uri=$uri; status=$status; ok=$statusOk; error=$body }
            $results += $result
            
            Write-Host "    Status: $status [$(if($statusOk){'PASS'}else{'FAIL'})]" -ForegroundColor $(if($statusOk){'Green'}else{'Red'})
            
            try {
                return ($body | ConvertFrom-Json)
            } catch {
                return $body
            }
        } else {
            $result = @{ name=$name; method=$method; uri=$uri; status=0; ok=$false; error=$_.Exception.Message }
            $results += $result
            
            Write-Host "    ERROR: $($_.Exception.Message)" -ForegroundColor Red
            return $null
        }
    }
}

# ============ PHASE 1: FOLIO CREATION ============
Write-Host "`n`n========== PHASE 1: FOLIO CREATION ==========" -ForegroundColor Magenta

$folioResp = RunTest "CREATE_FOLIO" "POST" "$base/v1/folios" $null 201
if ($folioResp -and $folioResp.numeroFolio) {
    $folio = $folioResp.numeroFolio
    $version = $folioResp.version
    Write-Host "    FOLIO: $folio (v$version)" -ForegroundColor Green
} else {
    Write-Host "    ERROR: Could not create folio" -ForegroundColor Red
    exit 1
}

# ============ PHASE 2: GENERAL INFO ============
Write-Host "`n========== PHASE 2: GENERAL INFORMATION ==========" -ForegroundColor Magenta

RunTest "GET_GENERAL_INFO" "GET" "$base/v1/quotes/$folio/general-info" $null 200

$genlBody = @{
    version = $version
    datosAsegurado = @{
        nombre = "Juan"
        apellido = "Perez"
        email = "juan@test.com"
        telefono = "5551234567"
        documentoId = "DNI123"
    }
    codigoAgente = "AGT001"
}
$genlResp = RunTest "UPDATE_GENERAL_INFO" "PUT" "$base/v1/quotes/$folio/general-info" $genlBody 200
if ($genlResp.version) { $version = $genlResp.version }

# ============ PHASE 3: LOCATIONS ============
Write-Host "`n========== PHASE 3: LOCATIONS MANAGEMENT ==========" -ForegroundColor Magenta

RunTest "GET_LOCATIONS_EMPTY" "GET" "$base/v1/quotes/$folio/locations" $null 200

$locBody = @{
    version = $version
    locations = @(
        @{
            index = 0
            locationName = "Oficina Principal"
            address = "Calle Principal 123"
            zipCode = "28001"
            giro = "COMERCIO"
            fireKey = "FK_COMERCIO_001"
            insuredAmount = 1200000
            contentsValue = 500000
        },
        @{
            index = 1
            locationName = "Almacen Central"
            address = "Ruta Industrial Km 10"
            zipCode = "28002"
            giro = "ALMACENAMIENTO"
            fireKey = "FK_ALMAC_002"
            insuredAmount = 800000
            contentsValue = 300000
        }
    )
}
$locResp = RunTest "PUT_LOCATIONS" "PUT" "$base/v1/quotes/$folio/locations" $locBody 200
if ($locResp.version) { $version = $locResp.version }

# ============ PHASE 4: LOCATION VALIDATION ============
Write-Host "`n========== PHASE 4: LOCATION VALIDATION ==========" -ForegroundColor Magenta

$patch0Body = @{
    version = $version
    codigoPostal = "28001"
    giro = "COMERCIO"
    claveIncendio = "FK001"
}
$patch0Resp = RunTest "PATCH_LOCATION_0" "PATCH" "$base/v1/quotes/$folio/locations/0" $patch0Body 200
if ($patch0Resp.version) { $version = $patch0Resp.version }

$patch1Body = @{
    version = $version
    codigoPostal = "28002"
    giro = "ALMAC"
    claveIncendio = "FK002"
}
$patch1Resp = RunTest "PATCH_LOCATION_1" "PATCH" "$base/v1/quotes/$folio/locations/1" $patch1Body 200
if ($patch1Resp.version) { $version = $patch1Resp.version }

# ============ PHASE 5: LAYOUT ============
Write-Host "`n========== PHASE 5: LAYOUT CONFIGURATION ==========" -ForegroundColor Magenta

RunTest "GET_LAYOUT" "GET" "$base/v1/quotes/$folio/locations/layout" $null 200

$layoutBody = @{
    version = $version
    configuracionLayout = @{
        active = "map"
        collapsed = @("risk")
        columns = 2
    }
}
$layoutResp = RunTest "PUT_LAYOUT" "PUT" "$base/v1/quotes/$folio/locations/layout" $layoutBody 200
if ($layoutResp.version) { $version = $layoutResp.version }

# ============ PHASE 6: SUMMARY ============
Write-Host "`n========== PHASE 6: LOCATIONS SUMMARY ==========" -ForegroundColor Magenta

RunTest "GET_SUMMARY" "GET" "$base/v1/quotes/$folio/locations/summary" $null 200

# ============ PHASE 7: COVERAGE ============
Write-Host "`n========== PHASE 7: COVERAGE OPTIONS ==========" -ForegroundColor Magenta

RunTest "GET_COVERAGE" "GET" "$base/v1/quotes/$folio/coverage-options" $null 200

$covBody = @{
    version = $version
    opcionesCobertura = @("INCENDIO", "CAT", "FHM")
}
$covResp = RunTest "PUT_COVERAGE" "PUT" "$base/v1/quotes/$folio/coverage-options" $covBody 200
if ($covResp.version) { $version = $covResp.version }

# ============ PHASE 8: CALCULATE ============
Write-Host "`n========== PHASE 8: QUOTE CALCULATION ==========" -ForegroundColor Magenta

$calcBody = @{
    version = $version
    parametros_calculo = @{
        incluirSobretasa = 1
        aplicarDescuentos = 0
    }
}
$calcResp = RunTest "POST_CALCULATE" "POST" "$base/v1/quotes/$folio/calculate" $calcBody 200
if ($calcResp.version) { $version = $calcResp.version }

# ============ PHASE 9: STATE ============
Write-Host "`n========== PHASE 9: FINAL STATE ==========" -ForegroundColor Magenta

RunTest "GET_STATE" "GET" "$base/v1/quotes/$folio/state" $null 200

RunTest "GET_LOCATIONS_FINAL" "GET" "$base/v1/quotes/$folio/locations" $null 200

# ============ PHASE 10: ERROR SCENARIOS ============
Write-Host "`n========== PHASE 10: ERROR SCENARIOS ==========" -ForegroundColor Magenta

$staleBody = @{
    version = 0
    configuracionLayout = @{ active = "list"; collapsed = @(); columns = 1 }
}
RunTest "STALE_VERSION" "PUT" "$base/v1/quotes/$folio/locations/layout" $staleBody 409

RunTest "INVALID_FOLIO" "GET" "$base/v1/quotes/INVALID/state" $null @(404, 500)

RunTest "INVALID_INDEX" "PATCH" "$base/v1/quotes/$folio/locations/999" $patch0Body @(400, 404, 500)

# ============ SUMMARY ============
Write-Host "`n`n========== TEST SUMMARY ==========" -ForegroundColor White

$passed = ($results | Where-Object { $_.ok }).Count
$failed = ($results | Where-Object { -not $_.ok }).Count
$total = $results.Count
$pct = if ($total -gt 0) { [math]::Round(($passed / $total) * 100, 1) } else { 0 }

Write-Host "Total: $total | Passed: $passed | Failed: $failed | Rate: $pct%" -ForegroundColor $(if ($failed -eq 0) { 'Green' } else { 'Yellow' })

Write-Host "`n--- RESULTS ---" -ForegroundColor White
$results | ForEach-Object {
    $symbol = if ($_.ok) { "[OK]" } else { "[FAIL]" }
    $color = if ($_.ok) { "Green" } else { "Red" }
    Write-Host "$symbol $($_.name) - Status: $($_.status)" -ForegroundColor $color
    if ($_.error) {
        Write-Host "     Error: $($_.error)" -ForegroundColor Red
    }
}

Write-Host "`n========================================" -ForegroundColor White
