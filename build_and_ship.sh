#!/usr/bin/env bash
set -e

# ==============================================================
# MCGG Advisor — One-Click Build & Auto-Ship Script (Linux/macOS)
# ==============================================================

STB_IP="192.168.1.252"
STB_USER="stb1"
TARGET_DIR="/home/stb1/mcgogo_builds"

echo "=================================================="
echo "🚀 Memulai Build APK: MCGG Advisor..."
echo "=================================================="

# Check Gradle Wrapper
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

chmod +x gradlew 2>/dev/null || true

# 1. Compile APK
$GRADLE_CMD assembleDebug

APK_PATH=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    echo "❌ Gagal: File APK tidak ditemukan di app/build/outputs/apk/debug/"
    exit 1
fi

echo "=================================================="
echo "✅ Build Berhasil: $APK_PATH"
echo "📦 Mengirim APK otomatis ke STB ($STB_IP)..."
echo "=================================================="

# 2. SCP Transfer to STB
scp "$APK_PATH" "${STB_USER}@${STB_IP}:${TARGET_DIR}/mcgogo-debug.apk"

echo "=================================================="
echo "🎉 SUKSES! File APK telah terkirim ke STB."
echo "File berada di: ${TARGET_DIR}/mcgogo-debug.apk"
echo "=================================================="
