# Artier IDE - Kotlin Native

Agentic AI First IDE untuk Android dengan performa native.

## Architecture

- **App Layer:** Kotlin + Jetpack Compose (native, bukan hybrid)
- **Editor:** Sora Editor (native Android)
- **Terminal:** Termux TerminalView + TerminalEmulator (native)
- **Daemon:** Node.js + TypeScript + Fastify (jalan di proot)
- **State Management:** ViewModel + StateFlow

## Features (Phase 1)

- ✅ Sora Editor dengan syntax highlighting
- ✅ Terminal integration via Termux components
- ✅ File explorer native Compose
- ✅ Tab management untuk editor
- ✅ WebSocket communication dengan daemon

## Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) atau lebih baru
- JDK 17
- Android SDK 34
- Device Android dengan RAM minimal 4GB

### Build

```bash
# Clone repository
git clone https://github.com/your-username/artier-ide-kotlin.git
cd artier-ide-kotlin

# Build project
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Configuration

1. **Daemon Setup:**
   - Daemon berjalan di proot environment
   - Default port: 8080
   - WebSocket endpoint: `ws://127.0.0.1:8080/ws`

2. **Editor Configuration:**
   - TextMate grammar untuk syntax highlighting
   - Support 50+ bahasa pemrograman
   - Auto-detect language dari file extension

3. **Terminal Configuration:**
   - PTY backend via WebSocket
   - Support bash, zsh, dan shell lainnya
   - Scroll buffer dan copy/paste support

## Project Structure

```
artier-ide-kotlin/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/artier/ide/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── editor/          # Editor components
│   │   │   │   │   ├── terminal/        # Terminal components
│   │   │   │   │   ├── fileexplorer/    # File explorer
│   │   │   │   │   ├── workspace/       # Main workspace
│   │   │   │   │   ├── components/      # Shared components
│   │   │   │   │   └── theme/           # App theme
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/           # Data models
│   │   │   │   │   ├── remote/          # Network clients
│   │   │   │   │   └── repository/      # Data repositories
│   │   │   │   ├── di/                  # Dependency injection
│   │   │   │   └── ArtierApp.kt         # Application class
│   │   │   ├── res/                     # Resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                        # Unit tests
│   │   └── androidTest/                 # Instrumentation tests
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Development

### Phase 1: Editor & Terminal

**Goal:** Integrasi editor native dan terminal tanpa WebView.

**Components:**
1. Sora Editor wrapper untuk Compose
2. TerminalView wrapper untuk Compose
3. File explorer dengan lazy loading
4. Tab management untuk editor
5. WebSocket client untuk daemon communication

**Validation:**
- ✅ Buka project dan edit file dengan syntax highlighting
- ✅ Jalankan terminal dan eksekusi command
- ✅ File explorer bisa navigasi dan buka file
- ✅ Tab management berfungsi dengan benar
- ✅ Komunikasi WebSocket stabil
- ✅ Tidak ada WebView yang aktif
- ✅ RAM usage < 500MB saat idle

### Completed Phases

- ✅ **Phase 2:** Public Tunnel (cloudflared)
- ✅ **Phase 3:** 9Router Embed
- ✅ **Phase 4:** Agent Adapter System
- ✅ **Phase 5:** Skill System (`SKILL.md` / agentskills.io)
- ✅ **Phase 6:** Database Panel
- ✅ **Phase 7:** Workspace Canvas
- ✅ **Phase 8:** Polish & Optimization

## Performance Targets

- Cold start: < 3 detik
- RAM idle: < 250MB
- RAM aktif (editor + terminal): < 500MB
- File open response: < 500ms
- Terminal latency: < 100ms
- Crash rate: < 1%

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Sora Editor](https://github.com/Rosemoe/sora-editor) - Native code editor for Android
- [Termux](https://termux.dev/) - Terminal emulator for Android
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Fastify](https://www.fastify.io/) - Fast and low overhead web framework for Node.js