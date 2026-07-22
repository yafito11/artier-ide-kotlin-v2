# Fase 1: Editor & Terminal - Planning Detail

## Goal
Integrasi editor native (Sora Editor) dan terminal (Termux TerminalView) tanpa WebView, dengan file explorer dan tab management.

## Duration
3-4 minggu

## Scope
- Integrasi Sora Editor dengan TextMate grammar
- Integrasi TerminalView dari codebase Termux
- File explorer native Compose
- Tab management untuk editor
- Komunikasi WebSocket dengan daemon

## Komponen yang Dibangun

### 1. Sora Editor Integration
**Fitur:**
- Syntax highlighting untuk 50+ bahasa
- Line numbers
- Code folding
- Basic autocomplete (via LSP nanti)
- Multi-tab support

**Setup:**
- Tambah dependency Sora Editor di build.gradle
- Buat wrapper Compose untuk AndroidView
- Konfigurasi TextMate grammar
- Theme management (dark/light)

### 2. Termux Terminal Integration
**Fitur:**
- Terminal emulation lengkap
- Scroll buffer
- Copy/paste support
- PTY backend via WebSocket

**Setup:**
- Integrasi TerminalView sebagai Compose wrapper
- WebSocket client untuk PTY communication
- Terminal session management

### 3. File Explorer
**Fitur:**
- Tree view structure
- File/folder icons
- Context menu (rename, delete, copy)
- Search functionality
- Breadcrumb navigation

**Setup:**
- LazyColumn dengan nested items
- File operations via daemon API
- Icon mapping berdasarkan extension

### 4. Tab Management
**Fitur:**
- Multiple editor tabs
- Tab switching dengan state preservation
- Close tab dengan save confirmation
- Tab reordering (drag & drop)
- New tab button

**Setup:**
- ViewModel untuk tab state
- Editor instance pooling
- Memory management untuk tab inactive

### 5. WebSocket Communication
**Fitur:**
- Real-time terminal I/O
- File system operations
- Editor events (save, change)
- Heartbeat connection

**Setup:**
- OkHttp WebSocket client
- Message protocol (JSON)
- Reconnection logic
- Error handling

## File Structure
```
artier-ide/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/artier/ide/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── editor/
│   │   │   │   │   │   ├── SoraEditorWrapper.kt
│   │   │   │   │   │   ├── EditorTabManager.kt
│   │   │   │   │   │   └── EditorViewModel.kt
│   │   │   │   │   ├── terminal/
│   │   │   │   │   │   ├── TerminalWrapper.kt
│   │   │   │   │   │   └── TerminalViewModel.kt
│   │   │   │   │   ├── fileexplorer/
│   │   │   │   │   │   ├── FileExplorer.kt
│   │   │   │   │   │   ├── FileItem.kt
│   │   │   │   │   │   └── FileExplorerViewModel.kt
│   │   │   │   │   ├── workspace/
│   │   │   │   │   │   └── WorkspaceScreen.kt
│   │   │   │   │   └── components/
│   │   │   │   │       ├── TabRow.kt
│   │   │   │   │       └── Sidebar.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── FileNode.kt
│   │   │   │   │   │   ├── EditorTab.kt
│   │   │   │   │   │   └── TerminalSession.kt
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── WebSocketClient.kt
│   │   │   │   │   │   └── DaemonApi.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── FileRepository.kt
│   │   │   │   │       └── EditorRepository.kt
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt
│   │   │   │   └── ArtierApp.kt
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Dependencies

### Core
```kotlin
// Jetpack Compose
implementation("androidx.compose.ui:ui:1.5.0")
implementation("androidx.compose.material3:material3:1.1.1")
implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
implementation("androidx.activity:activity-compose:1.7.2")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.1")

// WebSocket
implementation("com.squareup.okhttp3:okhttp:4.11.0")

// Image loading
implementation("io.coil-kt:coil-compose:2.4.0")
```

### Sora Editor
```kotlin
// Sora Editor (add via JitPack or local AAR)
implementation("com.github.Rosemoe.sora-editor:editor:0.23.4")
implementation("com.github.Rosemoe.sora-editor:language-textmate:0.23.4")
```

### Termux Components
```kotlin
// Terminal View (extract from Termux source)
implementation("com.termux:terminal-view:0.118.0")
// Or include as local module
```

## Implementation Steps

### Minggu 1: Setup Foundation
1. **Hari 1-2:** Setup Android project dengan Gradle Kotlin DSL
2. **Hari 3-4:** Konfigurasi Jetpack Compose dengan Material 3
3. **Hari 5-7:** Setup WebSocket client dan daemon communication

### Minggu 2: Editor Integration
1. **Hari 1-3:** Integrasi Sora Editor dengan basic wrapper
2. **Hari 4-5:** TextMate grammar setup
3. **Hari 6-7:** Tab management system

### Minggu 3: Terminal Integration
1. **Hari 1-3:** Integrasi TerminalView dengan Compose
2. **Hari 4-5:** PTY backend via WebSocket
3. **Hari 6-7:** Terminal session management

### Minggu 4: File Explorer & Polish
1. **Hari 1-3:** File explorer UI dengan lazy loading
2. **Hari 4-5:** File operations integration
3. **Hari 6-7:** Testing dan bug fixes

## Validation Criteria
1. ✅ Buka project dan edit file dengan syntax highlighting
2. ✅ Jalankan terminal dan eksekusi command
3. ✅ File explorer bisa navigasi dan buka file
4. ✅ Tab management berfungsi dengan benar
5. ✅ Komunikasi WebSocket stabil
6. ✅ Tidak ada WebView yang aktif
7. ✅ RAM usage < 500MB saat idle

## Risks & Mitigations

### 1. Sora Editor Integration Complexity
**Risk:** Integrasi yang kompleks dengan Compose
**Mitigation:** Mulai dengan AndroidView wrapper sederhana, tingkatkan bertahap

### 2. Termux Components Compatibility
**Risk:** Komponen Termux mungkin tidak kompatibel langsung
**Mitigation:** Fork dan modify sesuai kebutuhan, atau gunakan dependency lokal

### 3. Memory Management
**Risk:** Banyak tab editor bisa menghabiskan memori
**Mitigation:** Lazy init, suspend tab inactive, batasi jumlah tab aktif

### 4. WebSocket Stability
**Risk:** Koneksi terputus saat daemon restart
**Mitigation:** Auto-reconnect, heartbeat, state persistence

## Testing Strategy

### Unit Tests
- ViewModel logic
- Data models
- Repository functions

### Integration Tests
- WebSocket communication
- File operations
- Editor-terminal interaction

### UI Tests
- File explorer navigation
- Tab switching
- Terminal input/output

### Device Testing
- Test di device fisik 4GB RAM
- Monitor RAM usage dengan Android Profiler
- Test dengan project besar (1000+ files)

## Success Metrics
1. Cold start < 3 detik
2. RAM idle < 250MB
3. RAM aktif (editor + terminal) < 500MB
4. File open response < 500ms
5. Terminal latency < 100ms
6. Zero crash dalam 1 jam penggunaan

## Next Steps (Fase 2)
- Integrasi cloudflared untuk public tunnel
- Auto-detect port listening
- UI untuk public URL management