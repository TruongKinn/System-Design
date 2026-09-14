Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🚀 STARTING E2E VERIFICATION: DATAFLOW EXPORT & IMPORT" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. TEST EXPORT SERVICE (EXCEL)
Write-Host "`n--- [TEST 1] Triggering Real Excel Export (5,000 records) ---" -ForegroundColor Yellow
$exportPayload = @{
    entityName = "CUSTOMER"
    requestedRecords = 5000
    fileFormat = "xlsx"
} | ConvertTo-Json

$exportRes = Invoke-RestMethod -Uri "http://localhost:8083/api/exports/create" -Method Post -Body $exportPayload -ContentType "application/json"
$jobId = $exportRes.jobId
Write-Host "Export Job Created: $jobId, Initial Status: $($exportRes.status)" -ForegroundColor Green

# Polling progress
$isCompleted = $false
$maxRetries = 20
$retries = 0
$downloadUrl = ""

while (-not $isCompleted -and $retries -lt $maxRetries) {
    Start-Sleep -Seconds 1
    $retries++
    $statusRes = Invoke-RestMethod -Uri "http://localhost:8083/api/exports/$jobId/status" -Method Get
    Write-Host "Poll #($retries) - Status = $($statusRes.status), Processed = $($statusRes.processedRecords)/$($statusRes.totalRecords), Percent = $($statusRes.progressPercentage)%" -ForegroundColor Gray
    
    if ($statusRes.status -eq "COMPLETED") {
        $isCompleted = $true
        $downloadUrl = $statusRes.downloadUrl
    }
}

if ($isCompleted) {
    Write-Host "SUCCESS: Export Job [$jobId] COMPLETED!" -ForegroundColor Green
    Write-Host "MinIO Presigned Download URL: $downloadUrl" -ForegroundColor Green
    
    # Download file to verify
    $tempExcelPath = "$PSScriptRoot\test_downloaded_export.xlsx"
    Invoke-WebRequest -Uri $downloadUrl -OutFile $tempExcelPath
    $fileSize = (Get-Item $tempExcelPath).Length
    Write-Host "Downloaded file size from MinIO: $fileSize bytes ($([math]::Round($fileSize/1KB, 2)) KB)" -ForegroundColor Green
    if ($fileSize -gt 1000) {
        Write-Host "PASSED: Real Excel file verified on MinIO storage!" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Downloaded file is too small or empty!" -ForegroundColor Red
    }
} else {
    Write-Host "FAILED: Export Job did not complete in time." -ForegroundColor Red
}

# 2. TEST IMPORT SERVICE (CSV)
Write-Host "`n--- [TEST 2] Triggering Real CSV Import to PostgreSQL ---" -ForegroundColor Yellow
$csvPath = "$PSScriptRoot\test_customers_upload.csv"
$csvLines = @(
    "Mã Khách Hàng,Họ và Tên,Email,Số Điện Thoại"
)
for ($i = 1; $i -le 500; $i++) {
    $csvLines += "CUST-$("{0:D6}" -f $i),Khách Hàng Test $i,customer$i@dataflow.vn,09$("{0:D8}" -f $i)"
}
$csvLines | Set-Content -Path $csvPath -Encoding UTF8
Write-Host "Created test CSV with 500 records at: $csvPath" -ForegroundColor Gray

# Upload using curl.exe
$uploadCommand = "curl.exe -s -X POST http://localhost:8084/api/imports/upload -F `"file=@$csvPath`""
$importRaw = Invoke-Expression $uploadCommand
$importJob = $importRaw | ConvertFrom-Json
$importJobId = $importJob.jobId
Write-Host "Import Job Created: $importJobId, Initial Status: $($importJob.status)" -ForegroundColor Green

# Polling import progress
$isImportCompleted = $false
$retries = 0
while (-not $isImportCompleted -and $retries -lt $maxRetries) {
    Start-Sleep -Seconds 1
    $retries++
    $importStatus = Invoke-RestMethod -Uri "http://localhost:8084/api/imports/$importJobId/status" -Method Get
    Write-Host "Poll #($retries) - Status = $($importStatus.status), Imported = $($importStatus.importedRows)/$($importStatus.totalRows), Errors = $($importStatus.errorRows), Percent = $($importStatus.progressPercentage)%" -ForegroundColor Gray
    if ($importStatus.status -eq "COMPLETED") {
        $isImportCompleted = $true
    }
}

if ($isImportCompleted) {
    Write-Host "SUCCESS: Import Job [$importJobId] COMPLETED!" -ForegroundColor Green
    
    # Check PostgreSQL database count
    $dbCountQuery = "docker exec dataflow-postgres psql -U postgres -d dataflow_db -t -c `"SELECT count(*) FROM imported_customers WHERE import_job_id = '$importJobId';`""
    $dbCountRaw = Invoke-Expression $dbCountQuery
    $dbCountStr = ($dbCountRaw -join "") -replace "[^0-9]", ""
    $insertedCount = 0
    if ($dbCountStr) { $insertedCount = [int]$dbCountStr }
    Write-Host "PostgreSQL verification: $insertedCount rows inserted into 'imported_customers' table!" -ForegroundColor Green
    if ($insertedCount -ge 500) {
        Write-Host "PASSED: All 500 records verified inside PostgreSQL!" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Expected 500 records in DB, found $insertedCount" -ForegroundColor Red
    }
} else {
    Write-Host "FAILED: Import Job did not complete in time." -ForegroundColor Red
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "🎉 ALL E2E VERIFICATIONS COMPLETE!" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
