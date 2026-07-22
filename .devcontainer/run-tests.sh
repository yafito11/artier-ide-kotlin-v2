#!/bin/bash
# Artier IDE - Automated Test Runner
# Runs all tests automatically

set -e

echo "=========================================="
echo "  Artier IDE - Automated Test Suite"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Configuration
REDROID_HOST=${REDROID_HOST:-redroid}
REDROID_PORT=${REDROID_PORT:-5555}
REPORT_DIR="/workspace/test-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="$REPORT_DIR/test-report-$TIMESTAMP.txt"

# Create report directory
mkdir -p "$REPORT_DIR"

# Initialize report
echo "Artier IDE Test Report" > "$REPORT_FILE"
echo "Generated: $(date)" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

run_test() {
    local test_name="$1"
    local test_command="$2"
    local test_type="$3"
    
    log_step "Running: $test_name"
    echo "" >> "$REPORT_FILE"
    echo "TEST: $test_name" >> "$REPORT_FILE"
    echo "TYPE: $test_type" >> "$REPORT_FILE"
    echo "COMMAND: $test_command" >> "$REPORT_FILE"
    echo "---" >> "$REPORT_FILE"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # Run test and capture output
    OUTPUT=$(eval "$test_command" 2>&1)
    EXIT_CODE=$?
    
    # Log output
    echo "$OUTPUT" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    if [ $EXIT_CODE -eq 0 ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_info "✓ PASSED: $test_name"
        echo "RESULT: PASSED" >> "$REPORT_FILE"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_error "✗ FAILED: $test_name (exit code: $EXIT_CODE)"
        echo "RESULT: FAILED (exit code: $EXIT_CODE)" >> "$REPORT_FILE"
    fi
}

# ==========================================
# Phase 1: Unit Tests
# ==========================================
echo "" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "PHASE 1: UNIT TESTS" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"

log_info "Phase 1: Running Unit Tests..."
cd /workspace

# Run all unit tests
run_test "All Unit Tests" "./gradlew test --no-daemon --continue" "Unit"

# Run specific test classes
run_test "Model Tests" "./gradlew test --tests 'com.artier.ide.data.model.*' --no-daemon" "Unit"
run_test "ProotManager Tests" "./gradlew test --tests 'com.artier.ide.proot.*' --no-daemon" "Unit"
run_test "DaemonClient Tests" "./gradlew test --tests 'com.artier.ide.data.remote.DaemonClientTest' --no-daemon" "Unit"
run_test "WebSocketClient Tests" "./gradlew test --tests 'com.artier.ide.data.remote.WebSocketClientTest' --no-daemon" "Unit"
run_test "TerminalViewModel Tests" "./gradlew test --tests 'com.artier.ide.ui.terminal.*' --no-daemon" "Unit"
run_test "DatabaseViewModel Tests" "./gradlew test --tests 'com.artier.ide.ui.database.*' --no-daemon" "Unit"

# ==========================================
# Phase 2: Build Verification
# ==========================================
echo "" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "PHASE 2: BUILD VERIFICATION" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"

log_info "Phase 2: Verifying Build..."

run_test "Clean Build" "./gradlew clean --no-daemon" "Build"
run_test "Debug Build" "./gradlew assembleDebug --no-daemon" "Build"
run_test "Release Build" "./gradlew assembleRelease --no-daemon" "Build"

# ==========================================
# Phase 3: Code Quality
# ==========================================
echo "" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "PHASE 3: CODE QUALITY" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"

log_info "Phase 3: Checking Code Quality..."

run_test "Lint Check" "./gradlew lint --no-daemon" "Quality"

# ==========================================
# Phase 4: Instrumented Tests (if emulator available)
# ==========================================
echo "" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "PHASE 4: INSTRUMENTED TESTS" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"

log_info "Phase 4: Running Instrumented Tests..."

# Check if emulator is available
if adb connect ${REDROID_HOST}:${REDROID_PORT} 2>/dev/null | grep -q "connected"; then
    log_info "Emulator connected, running instrumented tests..."
    
    # Wait for emulator to be ready
    sleep 5
    
    # Run instrumented tests
    run_test "Instrumented Tests" "./gradlew connectedAndroidTest --no-daemon" "Instrumented"
    
    # Install and run app
    APK_PATH="/workspace/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        log_info "Installing APK on emulator..."
        adb -s ${REDROID_HOST}:${REDROID_PORT} install -r "$APK_PATH" 2>/dev/null || true
        
        # Launch app
        log_info "Launching app..."
        adb -s ${REDROID_HOST}:${REDROID_PORT} shell am start -n com.artier.ide/.MainActivity 2>/dev/null || true
        
        # Wait for app to start
        sleep 5
        
        # Capture screenshot
        log_info "Capturing screenshot..."
        adb -s ${REDROID_HOST}:${REDROID_PORT} shell screencap -p /sdcard/screenshot.png 2>/dev/null || true
        adb -s ${REDROID_HOST}:${REDROID_PORT} pull /sdcard/screenshot.png "$REPORT_DIR/screenshot-$TIMESTAMP.png" 2>/dev/null || true
    fi
else
    log_warn "Emulator not available, skipping instrumented tests"
    SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
    echo "RESULT: SKIPPED (Emulator not available)" >> "$REPORT_FILE"
fi

# ==========================================
# Phase 5: Coverage Report
# ==========================================
echo "" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "PHASE 5: COVERAGE REPORT" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"

log_info "Phase 5: Generating Coverage Report..."

run_test "Coverage Report" "./gradlew jacocoTestReport --no-daemon" "Coverage"

# ==========================================
# Generate Summary
# ==========================================
echo "" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "SUMMARY" >> "$REPORT_FILE"
echo "==========================================" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "Total Tests:  $TOTAL_TESTS" >> "$REPORT_FILE"
echo "Passed:       $PASSED_TESTS" >> "$REPORT_FILE"
echo "Failed:       $FAILED_TESTS" >> "$REPORT_FILE"
echo "Skipped:      $SKIPPED_TESTS" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $FAILED_TESTS -eq 0 ]; then
    echo "STATUS: ALL TESTS PASSED ✓" >> "$REPORT_FILE"
    log_info "All tests passed!"
else
    echo "STATUS: SOME TESTS FAILED ✗" >> "$REPORT_FILE"
    log_error "$FAILED_TESTS test(s) failed"
fi

echo "" >> "$REPORT_FILE"
echo "Report saved to: $REPORT_FILE" >> "$REPORT_FILE"

# Display summary
echo ""
echo "=========================================="
echo "  Test Summary"
echo "=========================================="
echo "  Total:   $TOTAL_TESTS"
echo "  Passed:  $PASSED_TESTS"
echo "  Failed:  $FAILED_TESTS"
echo "  Skipped: $SKIPPED_TESTS"
echo ""
echo "  Report:  $REPORT_FILE"
echo "=========================================="

# Exit with failure if any tests failed
if [ $FAILED_TESTS -gt 0 ]; then
    exit 1
fi
