# Artier IDE - DevContainer Setup

This DevContainer configuration provides a complete Android development environment with ReDroid (Docker-based Android emulator) for automated testing.

## Features

- **ReDroid Emulator**: Android 14 emulator running in Docker
- **Android SDK 34**: Full Android development toolkit
- **Java 17**: Required for Android development
- **Gradle 8.5**: Build system
- **Automated Testing**: Unit tests, instrumented tests, coverage reports

## Quick Start

### Option 1: GitHub Codespace (Recommended)

1. Go to your repository on GitHub
2. Click "Code" → "Codespaces" → "Create codespace on main"
3. Wait for the container to build (first time takes ~5-10 minutes)
4. The emulator will start automatically

### Option 2: VS Code DevContainers

1. Install VS Code and DevContainers extension
2. Clone the repository
3. Open in VS Code
4. Click "Reopen in Container" when prompted

## Available Commands

### Testing Commands
```bash
# Run unit tests
test-unit

# Run instrumented tests (requires emulator)
test-android

# Run all tests
test-all

# Run tests with coverage report
test-coverage
```

### Build Commands
```bash
# Install and run app on emulator
run-app

# Clean and rebuild
clean-build
```

### ADB Commands
```bash
# Connect to ReDroid emulator
adb connect redroid:5555

# Install APK
adb -s redroid:5555 install app.apk

# View logs
adb -s redroid:5555 logcat

# Open shell
adb -s redroid:5555 shell
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Codespace                          │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐    ┌──────────────────────────┐  │
│  │   DevContainer        │    │   ReDroid Emulator       │  │
│  │                       │    │                          │  │
│  │  - Java 17            │◄──►│  - Android 14            │  │
│  │  - Android SDK 34     │    │  - ARM64 emulation       │  │
│  │  - Gradle 8.5         │    │  - Port 5555             │  │
│  │  - Kotlin 1.9         │    │                          │  │
│  │                       │    │                          │  │
│  └──────────────────────┘    └──────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   /workspace                         │  │
│  │                                                       │  │
│  │  - Artier IDE source code                            │  │
│  │  - Test reports                                       │  │
│  │  - Build outputs                                      │  │
│  │                                                       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Test Reports

After running tests, reports are saved to:
- **Unit Tests**: `app/build/reports/tests/testDebugUnitTest/`
- **Coverage**: `app/build/reports/jacoco/`
- **Lint**: `app/build/reports/lint-results-debug.html`
- **Test Summary**: `test-reports/test-report-*.txt`

## Troubleshooting

### Emulator Not Starting
```bash
# Check ReDroid status
docker ps | grep redroid

# View ReDroid logs
docker logs artier-redroid

# Restart ReDroid
docker restart artier-redroid
```

### Build Failures
```bash
# Clean build
./gradlew clean

# Check Java version
java -version

# Verify Android SDK
echo $ANDROID_HOME
ls $ANDROID_HOME
```

### Connection Issues
```bash
# Reconnect to emulator
adb disconnect
adb connect redroid:5555

# Check emulator status
adb devices -l
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ANDROID_HOME` | `/opt/android-sdk` | Android SDK path |
| `JAVA_HOME` | `/usr/lib/jvm/java-17-openjdk-amd64` | Java installation path |
| `REDROID_HOST` | `redroid` | ReDroid container hostname |
| `REDROID_PORT` | `5555` | ReDroid ADB port |

## Ports

| Port | Service |
|------|---------|
| 5555 | ReDroid ADB |
| 5037 | ADB Server |
| 8080 | Artier Daemon |
| 3000 | Development Server |

## Performance Tips

1. **First run**: Container build takes ~5-10 minutes
2. **Subsequent runs**: Starts in ~30 seconds
3. **Caching**: Gradle and Android SDK are cached
4. **Parallel tests**: Gradle runs tests in parallel by default

## Resources

- [ReDroid Documentation](https://github.com/remote-android/redroid-doc)
- [DevContainers](https://containers.dev/)
- [GitHub Codespaces](https://docs.github.com/en/codespaces)
