# ==============================================================
# MCGG Advisor — One-Click Build & Auto-Ship Script (Windows)
# ==============================================================
$STB_IP = "192.168.1.252"
$STB_USER = "stb1"
$TARGET_DIR = "/home/stb1/mcgogo_builds"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "🚀 Memulai Build APK: MCGG Advisor..." -ForegroundColor Cyan
Write-Host "=================================================="

# 1. Compile APK via gradlew.bat
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    gradle assembleDebug
}

$APK = Get-ChildItem -Path "app\build\outputs\apk\debug\*.apk" | Select-Object -First 1

if (-not $APK) {
    Write-Host "❌ Gagal: File APK tidak ditemukan!" -ForegroundColor Red
    Exit 1
}

Write-Host "==================================================" -ForegroundColor Green
Write-Host "✅ Build Berhasil: $($APK.FullName)" -ForegroundColor Green
Write-Host "📦 Mengirim APK otomatis ke STB ($STB_IP)..." -ForegroundColor Yellow
Write-Host "=================================================="

# 2. SCP Transfer to STB
scp "$($APK.FullName)" "${STB_USER}@${STB_IP}:${TARGET_DIR}/mcgogo-debug.apk"

Write-Host "==================================================" -ForegroundColor Green
Write-Host "🎉 SUKSES! File APK telah mendarat di STB." -ForegroundColor Green
Write-Host "Path di STB: ${TARGET_DIR}/mcgogo-debug.apk" -ForegroundColor Cyan
Write-Host "=================================================="
