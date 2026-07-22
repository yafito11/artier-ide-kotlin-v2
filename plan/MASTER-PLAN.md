# ARTIER IDE - MASTER PLAN
> **Versi:** 1.0 | **Tanggal:** 2026-07-22
> **Status:** IN PROGRESS - Phase 9 (Polish) Pending

---

## RINGKASAN EKSEKUTIF

**Tujuan:** Membuat Artier IDE APK yang **siap pakai** setelah install, tanpa setup kecuali API key AI.

**Target Device:** Tablet Android 11 inci (1200x1920), RAM 4GB

**Estimasi Ukuran APK:** ~250-300 MB (termasuk Linux environment)

**Estimasi Waktu:** ~27 hari

---

## DESIGN SYSTEM: OBSIDIAN LOGIC

### Color Palette

- PRIMARY: 9Router Purple #D4BBFF
- ON_PRIMARY: #3F0F81
- PRIMARY_CONTAINER: #7E57C2
- SECONDARY: Kotlin Orange #FFB875
- ON_SECONDARY: #4B2800
- SECONDARY_CONTAINER: #E08100
- TERTIARY: Jetpack Blue #9FCAFF
- ON_TERTIARY: #003258
- TERTIARY_CONTAINER: #0070BC
- ERROR: #FFB4AB
- ON_ERROR: #690005

### Surface Hierarchy

- surface-obsidian: #0A0A0A (Editor, Terminal bg)
- surface-charcoal: #1E1E1E (Sidebars)
- surface-edge: #2D2D2D (Headers, Borders)
- surface: #131313 (General bg)
- surface-container: #201F1F (Cards)
- surface-container-high: #2A2A2A (Elevated)
- surface-container-highest: #353534

### Text Colors

- on-surface: #E5E2E1 (High contrast)
- on-surface-variant: #CCC3D3 (Secondary)
- text-muted: #A0A0A0 (Dimmed)
- text-high-contrast: #F5F5F5

### Accent Colors

- router-purple: #9C27B0
- kotlin-orange: #F88909
- jetpack-blue: #4285F4

### Typography

**UI Font: Inter**
- headline-lg: 24px/600/32px
- headline-md: 18px/600/24px
- body-md: 14px/400/20px
- body-sm: 12px/400/16px
- label-caps: 10px/700/12px (letterSpacing: 0.05em)

**Code Font: JetBrains Mono**
- code-lg: 14px/400/20px
- code-md: 13px/400/18px
- code-sm: 11px/400/14px

### Spacing and Layout

- Base unit: 4px
- Gutter: 8px
- Screen margin: 12px
- Sidebar width: 240dp
- AI Panel width: 320dp
- Touch target: 40dp minimum
- Tab bar height: 40dp
- Header height: 40dp
- Indent levels: 24px per nesting level

### Component Rules

- **Buttons:** Primary = solid fill, Secondary = outline only, Text = label-caps uppercase
- **Tabs:** Bottom: sharp, Top: 4px radius, Active: 2px primary top border
- **Inputs:** Bg darker than surface, Border: 1px surface-edge, Focus: kotlin-orange border
- **AI Panel:** Background: 5% purple overlay
- **Terminal:** Bg: surface-obsidian, Font: JetBrains Mono, Success/Fail: green/red text
- **Chat Bubbles:** 3 corners: 8px radius, 1 corner: sharp
- **Sidebar:** 24px indent per level
- **Canvas Nodes:** 8px radius
- **Chips/Badges:** Small, high-contrast

---

## LAYOUT: VS CODE STYLE

```
+-------------------------------------------------------------------+
|  TITLE BAR: [hamburger Artier IDE]     [AI] [Settings] [_][ ][X]  |
+--------+----------------------------------+-----------------------+
|        |  EDITOR TABS                     |                       |
| ACTIVE |  [main.kt x] [App.kt x] [+]     |  PANEL KANAN          |
| BAR    +----------------------------------+  (AI/Skills/DB)       |
|        |                                  |                       |
|        |  SORA EDITOR                     |  Chat messages...     |
| [FILES]|  (code editing area)             |                       |
| [SEARCH]|                                 |  [Input...]           |
| [EXT]  |                                  |                       |
| [SET]  +----------------------------------+-----------------------+
|        |  BOTTOM PANEL                                           |
| SIDE   |  [Terminal] [Output] [Problems] [Tunnel]               |
| BAR    |  +-----------------------------------------------------+|
|        |  | $ npm run dev                                        ||
|        |  | > Server running on http://localhost:3000            ||
|        |  | > [Make Public]                                      ||
|        |  +-----------------------------------------------------+|
+--------+---------------------------------------------------------+
|  STATUS BAR: [main.kt] [UTF-8] [Kotlin] [Ln 42, Col 15] [AI]   |
+-------------------------------------------------------------------+
```

---

## LUCIDE ICONS (Outline Style)

### Icon List

**Sidebar:**
- folder -> ic_folder.xml (on-surface-variant)
- folder-open -> ic_folder_open.xml (primary)
- search -> ic_search.xml (on-surface-variant)
- git-branch -> ic_git_branch.xml (on-surface-variant)
- blocks -> ic_blocks.xml (on-surface-variant)
- settings -> ic_settings.xml (on-surface-variant)

**Editor:**
- file-code -> ic_file_code.xml (on-surface-variant)
- file -> ic_file.xml (on-surface-variant)
- x -> ic_close.xml (on-surface-variant)
- plus -> ic_plus.xml (on-surface-variant)
- chevron-down -> ic_chevron_down.xml (on-surface-variant)
- chevron-right -> ic_chevron_right.xml (on-surface-variant)

**Terminal:**
- terminal -> ic_terminal.xml (primary)
- play -> ic_play.xml (secondary)
- square -> ic_stop.xml (error)

**Panels:**
- bot -> ic_bot.xml (primary)
- database -> ic_database.xml (tertiary)
- globe -> ic_globe.xml (tertiary)
- route -> ic_route.xml (secondary)
- puzzle -> ic_puzzle.xml (primary)

**Actions:**
- save -> ic_save.xml (on-surface)
- copy -> ic_copy.xml (on-surface)
- clipboard -> ic_clipboard.xml (on-surface)
- trash -> ic_trash.xml (error)
- pencil -> ic_edit.xml (on-surface)
- refresh-cw -> ic_refresh.xml (on-surface)
- arrow-left -> ic_arrow_left.xml (on-surface)
- arrow-right -> ic_arrow_right.xml (on-surface)
- check -> ic_check.xml (primary)
- loader -> ic_loader.xml (on-surface-variant)
- wifi -> ic_wifi.xml (primary)
- wifi-off -> ic_wifi_off.xml (error)

---

## LOGO INTEGRATION

**Source:** D:\yafie-project\artier-ide-kotlin-v2\logo\logo.png (500x500, RGBA, 78KB)

**Output:**
- ic_launcher.png: 48/72/96/144/192 px in mipmap-*/
- ic_launcher_round.png: Same in mipmap-*/
- ic_launcher_foreground.png: 432x432 in drawable/
- ic_launcher_background.png: 432x432 in drawable/ (solid surface-obsidian #0A0A0A)

---

## LINUX ENVIRONMENT BUNDLE

### Components

- **proot** (termux-packages): ~3 MB -> assets/bin/proot
- **Alpine rootfs** (alpine-minirootfs): ~80 MB -> assets/rootfs/
- **Node.js v20 LTS** (termux): ~40 MB -> assets/rootfs/usr/bin/node
- **npm v10** (termux): ~5 MB -> assets/rootfs/usr/bin/npm
- **bash** (termux): ~1 MB -> assets/rootfs/usr/bin/bash
- **Daemon server** (daemon/ folder): ~200 KB -> assets/rootfs/opt/artier/daemon/
- **Daemon deps** (npm install): ~50 MB -> assets/rootfs/opt/artier/daemon/node_modules/
- **cloudflared** (GitHub release): ~12 MB -> assets/bin/cloudflared
- **Basic utils** (termux): ~5 MB -> assets/rootfs/usr/bin/

**Total Estimasi: ~200-250 MB**

### First Launch Flow

1. App Start
2. Check: /data/data/com.artier.ide/files/rootfs/ exists?
3. IF NOT EXISTS:
   a. Show progress: "Extracting Linux environment..."
   b. Extract assets/rootfs/ -> files/rootfs/
   c. Extract assets/bin/ -> files/bin/
   d. chmod +x proot, node, cloudflared
   e. Mark extraction complete
4. Start proot:
   proot -0 -r files/rootfs -w / /usr/bin/bash -c "cd /opt/artier/daemon && node server.js"
5. Wait for daemon ready (WebSocket connect)
6. App ready!

### ProotManager

- isExtracted(): Check if rootfs extracted
- extractRootfs(onProgress): Extract assets to filesDir
- startDaemon(): Start proot + Node.js daemon
- stopDaemon(): Kill daemon process
- isDaemonRunning(): Check daemon status

---

## WEBSOCKET COMMUNICATION

### Events (Daemon -> App)

- Connected / Disconnected
- DaemonReady(version)
- TerminalOutput(sessionId, data)
- TerminalExit(sessionId, exitCode)
- FileContent(path, content)
- FileSaved(path)
- QueryResult(rows, rowCount)
- Tables(tables)
- Schema(tableName, columns)
- TunnelStatus(url, port)
- AgentMessage(content)
- Error(message)

### Commands (App -> Daemon)

- CreateTerminal(workingDir)
- TerminalInput(sessionId, data)
- TerminalResize(sessionId, cols, rows)
- CloseTerminal(sessionId)
- ReadFile(path)
- WriteFile(path, content)
- ListDirectory(path)
- CreateFile(path)
- CreateDirectory(path)
- DeleteFile(path)
- RenameFile(oldPath, newPath)
- RunAgent(agent, prompt)
- ConnectDatabase(type, config)
- QueryDatabase(sql)
- CreateTunnel(port, service)
- CloseTunnel(tunnelId)

---

## FITUR STATUS

- **Editor:** Working (Sora Editor + TextMate) ✅
- **Terminal:** Working (DaemonClient + PTY) ✅
- **File Explorer:** Working (DaemonClient + FileAPI) ✅
- **AI Assistant:** Working (Multi-agent + ToolLoop) ✅
- **Database:** Working (SQLite/PostgreSQL/libSQL) ✅
- **Skills:** Working (agentskills.io compatible) ✅
- **Tunnel:** Working (cloudflared integration) ✅
- **Workspace Canvas:** Basic (needs improvement) - P3

---

## IMPLEMENTATION PHASES

### PHASE 1: THEME and TYPOGRAPHY (Hari 1) ✅ COMPLETED
- [x] Create ui/theme/Color.kt
- [x] Create ui/theme/Type.kt
- [x] Update ui/theme/Theme.kt
- [x] Update res/values/colors.xml
- [x] Update res/values/themes.xml
- [x] Download font files

### PHASE 2: ICONS (Hari 1-2) ✅ COMPLETED
- [x] Generate launcher icons from logo.png
- [x] Create adaptive icon XML
- [x] Convert Lucide SVG to Vector Drawable (30+ icons)

### PHASE 3: UI COMPONENTS (Hari 2-4) ✅ COMPLETED
- [x] Create TopBar.kt
- [x] Create ActivityBar.kt
- [x] Create SidePanel.kt
- [x] Create EditorTabs.kt
- [x] Create BottomPanel.kt
- [x] Create StatusBar.kt
- [x] Create EmptyState.kt

### PHASE 4: WORKSPACE LAYOUT (Hari 4-5) ✅ COMPLETED
- [x] Refactor WorkspaceScreen.kt
- [x] Implement 3-zone layout
- [x] Responsive behavior

### PHASE 5: LINUX BUNDLE (Hari 5-8) ✅ COMPLETED
- [x] Create download script (scripts/download-linux-bundle.sh)
- [x] Bundle daemon code + deps
- [x] Implement ProotManager.kt (binary extraction, command execution)
- [x] Implement DaemonManager.kt (lifecycle management)
- [x] Implement SystemInitializer.kt (orchestration + progress)

### PHASE 6: WEBSOCKET (Hari 8-10) ✅ COMPLETED
- [x] Create DaemonClient.kt (unified high-level interface)
- [x] Implement WebSocket + events + commands
- [x] Update DaemonApi.kt (all commands + events)
- [x] Connection state management
- [x] Auto-reconnect support

### PHASE 7: FEATURES (Hari 10-18) ✅ COMPLETED
- [x] Fix Sora Editor (TextMate integration)
- [x] Implement Terminal + PTY (multi-session)
- [x] Implement File Explorer (CRUD operations)
- [x] Implement AI Assistant (multi-agent support)
- [x] Implement Database Panel (SQLite/PostgreSQL/libSQL)
- [x] Implement Skills Panel (agentskills.io compatible)
- [x] Implement Tunnel Panel (cloudflared)

### PHASE 8: TESTING (Hari 18-21) ✅ COMPLETED
- [x] Unit tests (ProotManager, DaemonClient, ViewModels, Models)
- [x] Integration tests (DatabaseRepository, WebSocket)
- [x] Testing guidelines documented
- [x] Mock classes for testing

### PHASE 9: POLISH (Hari 21-24)
- [ ] RAM optimization
- [ ] Performance tuning
- [ ] Final build

---

## TOTAL: ~24 HARI

## NOTES
- Logo: D:\yafie-project\artier-ide-kotlin-v2\logo\logo.png
- Keystore: artier-release.keystore (password: artier123)
- Design: Obsidian Logic (DESIGN.md)
- PRD: artier-ide-prd-kotlin-native.md
