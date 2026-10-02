# ============================================================
#  FreelanceHub Backend Startup Script
#  Run this from the project root in PowerShell:
#     .\start-backend.ps1
# ============================================================

$appProps = ".\backend\src\main\resources\application.properties"

# ── Ask for MySQL password ────────────────────────────────
Write-Host ""
Write-Host "  ⚡ FreelanceHub Backend Startup" -ForegroundColor Cyan
Write-Host "  ─────────────────────────────────" -ForegroundColor DarkGray
Write-Host ""
$mysqlPassword = Read-Host "  Enter your MySQL root password (press Enter if blank)"

# ── Test the password ─────────────────────────────────────
Write-Host ""
Write-Host "  Testing MySQL connection..." -ForegroundColor Yellow
$testResult = mysql -u root "-p$mysqlPassword" -e "SELECT 1;" 2>&1
if ($testResult -match "Access denied") {
    Write-Host "  ✗ MySQL password is incorrect. Please try again." -ForegroundColor Red
    exit 1
}
Write-Host "  ✓ MySQL connection successful!" -ForegroundColor Green

# ── Create database if missing ────────────────────────────
Write-Host "  Setting up database..." -ForegroundColor Yellow
mysql -u root "-p$mysqlPassword" -e "CREATE DATABASE IF NOT EXISTS freelancer_platform;" 2>&1 | Out-Null

$schemaResult = mysql -u root "-p$mysqlPassword" freelancer_platform -e "SHOW TABLES;" 2>&1
if ($schemaResult -notmatch "users") {
    Write-Host "  Importing schema and sample data..." -ForegroundColor Yellow
    mysql -u root "-p$mysqlPassword" freelancer_platform < ".\database\schema.sql" 2>&1 | Out-Null
    Write-Host "  ✓ Schema imported!" -ForegroundColor Green
} else {
    Write-Host "  ✓ Database already set up." -ForegroundColor Green
}

# ── Patch application.properties with the real password ───
Write-Host "  Configuring backend..." -ForegroundColor Yellow
$content = Get-Content $appProps -Raw
$content = $content -replace 'spring\.datasource\.password=.*', "spring.datasource.password=$mysqlPassword"
Set-Content $appProps $content
Write-Host "  ✓ Password saved to application.properties" -ForegroundColor Green

# ── Start Spring Boot ─────────────────────────────────────
Write-Host ""
Write-Host "  Starting Spring Boot on http://localhost:8080 ..." -ForegroundColor Cyan
Write-Host "  Press Ctrl+C to stop." -ForegroundColor DarkGray
Write-Host ""
Set-Location ".\backend"
mvn spring-boot:run
