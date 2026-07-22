#!/bin/bash
# Artier IDE - DevContainer Setup Script
# Runs after container creation

set -e

echo "=========================================="
echo "  Artier IDE - DevContainer Setup"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check Java version
log_info "Checking Java..."
java -version 2>&1 | head -1

# Check Android SDK
log_info "Checking Android SDK..."
if [ -d "$ANDROID_HOME" ]; then
    sdkmanager --version
else
    log_warn "Android SDK not found at $ANDROID_HOME"
fi

# Check ADB
log_info "Checking ADB..."
adb version 2>&1 | head -1 || log_warn "ADB not available yet"

# Setup Gradle cache
log_info "Setting up Gradle cache..."
mkdir -p ~/.gradle/caches
mkdir -p ~/.gradle/wrapper

# Setup Android cache
log_info "Setting up Android cache..."
mkdir -p ~/.android/cache
mkdir -p ~/.android/avd

# Check if project exists
if [ -f "/workspace/build.gradle.kts" ]; then
    log_info "Project found, building..."
    cd /workspace
    
    # Download dependencies
    log_info "Downloading Gradle dependencies..."
    ./gradlew dependencies --configuration implementation --quiet 2>/dev/null || true
    
    # Build the project
    log_info "Building project..."
    ./gradlew assembleDebug --no-daemon 2>/dev/null || log_warn "Build completed with warnings"
else
    log_warn "No project found in /workspace"
fi

# Create test script alias
log_info "Creating test aliases..."
cat >> ~/.bashrc << 'EOF'

# Artier IDE Testing Aliases
alias test-unit='./gradlew test --no-daemon'
alias test-android='./gradlew connectedAndroidTest --no-daemon'
alias test-all='./gradlew test connectedAndroidTest --no-daemon'
alias test-coverage='./gradlew jacocoTestReport --no-daemon'
alias run-app='./gradlew installDebug --no-daemon'
alias clean-build='./gradlew clean assembleDebug --no-daemon'
alias adb-connect='adb connect redroid:5555'
alias adb-install='adb -s redroid:5555 install'
alias adb-test='adb -s redroid:5555 shell am instrument -w com.artier.ide.test/androidx.test.runner.AndroidJUnitRunner'

# ReDroid aliases
alias redroid-status='adb -s redroid:5555 shell getprop ro.build.display.id'
alias redroid-reboot='adb -s redroid:5555 reboot'
alias redroid-logcat='adb -s redroid:5555 logcat'

EOF

log_info "Setup complete!"
echo ""
echo "=========================================="
echo "  Available Commands:"
echo "=========================================="
echo "  test-unit      - Run unit tests"
echo "  test-android   - Run instrumented tests"
echo "  test-all       - Run all tests"
echo "  test-coverage  - Run tests with coverage"
echo "  run-app        - Install and run app"
echo "  clean-build    - Clean and rebuild"
echo "  adb-connect    - Connect to ReDroid"
echo "  adb-install    - Install APK to ReDroid"
echo "  adb-test       - Run instrumented tests on ReDroid"
echo "=========================================="
