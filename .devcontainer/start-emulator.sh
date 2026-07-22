#!/bin/bash
# Artier IDE - Start ReDroid Emulator
# Runs after container starts

set -e

echo "=========================================="
echo "  Starting ReDroid Emulator Connection"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

REDROID_HOST=${REDROID_HOST:-redroid}
REDROID_PORT=${REDROID_PORT:-5555}

# Wait for ReDroid to be ready
log_info "Waiting for ReDroid emulator..."
MAX_ATTEMPTS=30
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if adb connect ${REDROID_HOST}:${REDROID_PORT} 2>/dev/null | grep -q "connected"; then
        log_info "Connected to ReDroid!"
        break
    fi
    
    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        log_error "Failed to connect to ReDroid after $MAX_ATTEMPTS attempts"
        log_warn "Trying to connect anyway..."
        adb connect ${REDROID_HOST}:${REDROID_PORT} || true
        break
    fi
    
    log_warn "Attempt $ATTEMPT/$MAX_ATTEMPTS - waiting 2s..."
    sleep 2
    ATTEMPT=$((ATTEMPT + 1))
done

# Verify connection
log_info "Verifying emulator connection..."
adb devices -l 2>/dev/null || log_warn "adb devices failed"

# Check emulator status
log_info "Checking emulator status..."
BUILD_ID=$(adb -s ${REDROID_HOST}:${REDROID_PORT} shell getprop ro.build.display.id 2>/dev/null || echo "Unknown")
SDK_VERSION=$(adb -s ${REDROID_HOST}:${REDROID_PORT} shell getprop ro.build.version.sdk 2>/dev/null || echo "Unknown")
MODEL=$(adb -s ${REDROID_HOST}:${REDROID_PORT} shell getprop ro.product.model 2>/dev/null || echo "Unknown")

log_info "Emulator Info:"
echo "  Build ID:    $BUILD_ID"
echo "  SDK Version: $SDK_VERSION"
echo "  Model:       $MODEL"
echo "  Device:      ${REDROID_HOST}:${REDROID_PORT}"

# Install APK if available
APK_PATH="/workspace/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    log_info "Installing APK..."
    adb -s ${REDROID_HOST}:${REDROID_PORT} install -r "$APK_PATH" 2>/dev/null || log_warn "APK installation failed"
fi

# Run quick smoke test
log_info "Running smoke test..."
adb -s ${REDROID_HOST}:${REDROID_PORT} shell pm list packages 2>/dev/null | head -5 || true

echo ""
echo "=========================================="
echo "  ReDroid Emulator Ready!"
echo "=========================================="
echo "  Device: ${REDROID_HOST}:${REDROID_PORT}"
echo "  Model:  $MODEL"
echo "  SDK:    $SDK_VERSION"
echo ""
echo "  Useful Commands:"
echo "    adb -s ${REDROID_HOST}:${REDROID_PORT} shell"
echo "    adb -s ${REDROID_HOST}:${REDROID_PORT} install app.apk"
echo "    adb -s ${REDROID_HOST}:${REDROID_PORT} logcat"
echo "=========================================="
