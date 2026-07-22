# Artier IDE — Automated Testing Checklist

> Checklist lengkap untuk memastikan semua fitur teruji secara otomatis.
> Gunakan file ini bersama `TESTING-GUIDELINES.md`.

---

## Daftar Isi

1. [Core Infrastructure](#1-core-infrastructure)
2. [WebSocket Client](#2-websocket-client)
3. [Daemon Communication](#3-daemon-communication)
4. [AI Assistant](#4-ai-assistant)
5. [Database Panel](#5-database-panel)
6. [Terminal](#6-terminal)
7. [File Explorer](#7-file-explorer)
8. [Tunnel](#8-tunnel)
9. [Router](#9-router)
10. [Skill System](#10-skill-system)
11. [Workspace Canvas](#11-workspace-canvas)
12. [Editor](#12-editor)
13. [Performance Checklist](#13-performance-checklist)
14. [Security Checklist](#14-security-checklist)
15. [Edge Cases](#15-edge-cases)

---

## 1. Core Infrastructure

### 1.1 App Startup
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 1.1.1 | App launches without crash | E2E | P0 | [x] |
| 1.1.2 | Cold start time < 3 seconds | Perf | P0 | [ ] |
| 1.1.3 | Warm start time < 1 second | Perf | P1 | [x] |
| 1.1.4 | Daemon starts on app launch | Unit | P0 | [x] |
| 1.1.5 | SystemInitializer completes successfully | Unit | P0 | [x] |
| 1.1.6 | PRoot environment initializes | Unit | P1 | [x] |
| 1.1.7 | SecureStorage initializes without crash | Unit | P0 | [x] |

### 1.2 DI (Hilt)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 1.2.1 | AppModule provides all dependencies | Unit | P0 | [x] |
| 1.2.2 | Singleton scope works correctly | Unit | P1 | [x] |
| 1.2.3 | No circular dependency | Unit | P0 | [x] |

---

## 2. WebSocket Client

### 2.1 Connection
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 2.1.1 | Connects to daemon successfully | Unit | P0 | [x] |
| 2.1.2 | Handles connection failure gracefully | Unit | P0 | [x] |
| 2.1.3 | Reconnects on disconnect (exponential backoff) | Unit | P0 | [ ] |
| 2.1.4 | Max reconnect attempts respected | Unit | P1 | [x] |
| 2.1.5 | Cancel reconnect job on manual disconnect | Unit | P0 | [x] |
| 2.1.6 | Coroutine scope cancelled on disconnect | Unit | P0 | [x] |

### 2.2 Message Handling
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 2.2.1 | Parses JSON messages correctly | Unit | P0 | [x] |
| 2.2.2 | Handles malformed JSON without crash | Unit | P0 | [x] |
| 2.2.3 | Routes to correct message handler | Unit | P0 | [x] |
| 2.2.4 | Multiple handlers for same event work | Unit | P1 | [ ] |
| 2.2.5 | Handler removal stops callbacks | Unit | P1 | [x] |
| 2.2.6 | Unknown events don't crash | Unit | P1 | [x] |
| 2.2.7 | Dual JSON parsing (org.json → Gson) works | Unit | P0 | [x] |

### 2.3 Message Sending
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 2.3.1 | Sends event with correct JSON format | Unit | P0 | [x] |
| 2.3.2 | Handles send when not connected | Unit | P1 | [x] |
| 2.3.3 | Large payloads handled correctly | Unit | P1 | [x] |

---

## 3. Daemon Communication

### 3.1 DaemonApi Event Mapping
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 3.1.1 | `terminal_create` maps correctly | Unit | P0 | [x] |
| 3.1.2 | `terminal_data` maps correctly | Unit | P0 | [ ] |
| 3.1.3 | `terminal_destroy` maps correctly | Unit | P0 | [ ] |
| 3.1.4 | `file_read` maps correctly | Unit | P0 | [x] |
| 3.1.5 | `file_write` maps correctly | Unit | P0 | [x] |
| 3.1.6 | `file_list` maps correctly | Unit | P0 | [ ] |
| 3.1.7 | `file_delete` maps correctly | Unit | P0 | [x] |
| 3.1.8 | `db_connect` maps correctly | Unit | P0 | [x] |
| 3.1.9 | `db_disconnect` maps correctly | Unit | P0 | [x] |
| 3.1.10 | `db_query` maps correctly | Unit | P0 | [x] |
| 3.1.11 | `db_tables` maps correctly | Unit | P0 | [x] |
| 3.1.12 | `db_schema` maps correctly | Unit | P0 | [x] |
| 3.1.13 | `tunnel_start` maps correctly | Unit | P0 | [ ] |
| 3.1.14 | `tunnel_stop` maps correctly | Unit | P0 | [ ] |
| 3.1.15 | `skill_list` maps correctly | Unit | P0 | [ ] |
| 3.1.16 | `skill_detail` maps correctly | Unit | P0 | [ ] |
| 3.1.17 | `agent_spawn` maps correctly | Unit | P0 | [ ] |
| 3.1.18 | `agent_input` maps correctly | Unit | P0 | [ ] |
| 3.1.19 | `agent_stop` maps correctly | Unit | P0 | [ ] |

### 3.2 Daemon Events
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 3.2.1 | `terminal_output` received and parsed | Unit | P0 | [x] |
| 3.2.2 | `file_content` received and parsed | Unit | P0 | [x] |
| 3.2.3 | `db_connected` received and parsed | Unit | P0 | [x] |
| 3.2.4 | `db_disconnected` received and parsed | Unit | P0 | [x] |
| 3.2.5 | `db_query_result` received and parsed | Unit | P0 | [x] |
| 3.2.6 | `db_tables` received and parsed | Unit | P0 | [x] |
| 3.2.7 | `db_schema` received and parsed | Unit | P0 | [x] |
| 3.2.8 | `tunnel_started` received and parsed | Unit | P0 | [x] |
| 3.2.9 | `tunnel_stopped` received and parsed | Unit | P0 | [x] |
| 3.2.10 | `tunnel_url` received and parsed | Unit | P0 | [x] |
| 3.2.11 | `skill_list_result` received and parsed | Unit | P0 | [ ] |
| 3.2.12 | `skill_detail_result` received and parsed | Unit | P0 | [ ] |
| 3.2.13 | `agent_status` received and parsed | Unit | P0 | [ ] |
| 3.2.14 | `agent_output` received and parsed | Unit | P0 | [ ] |
| 3.2.15 | `agent_error` received and parsed | Unit | P0 | [ ] |
| 3.2.16 | `router_status` received and parsed | Unit | P0 | [x] |
| 3.2.17 | `router_config` received and parsed | Unit | P0 | [x] |

---

## 4. AI Assistant

### 4.1 AiViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 4.1.1 | Initial state is idle | Unit | P0 | [x] |
| 4.1.2 | Update input changes state | Unit | P0 | [x] |
| 4.1.3 | Send message sets loading to true | Unit | P0 | [x] |
| 4.1.4 | Send message clears input | Unit | P0 | [x] |
| 4.1.5 | Receive message appends to list | Unit | P0 | [x] |
| 4.1.6 | Receive error sets error state | Unit | P0 | [x] |
| 4.1.7 | Clear error removes error | Unit | P1 | [x] |
| 4.1.8 | Session list loads correctly | Unit | P1 | [x] |
| 4.1.9 | Switch session updates messages | Unit | P1 | [x] |
| 4.1.10 | Delete session removes from list | Unit | P1 | [x] |

### 4.2 AiAssistantPanel (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 4.2.1 | Panel renders with header | UI | P0 | [x] |
| 4.2.2 | Input field accepts text | UI | P0 | [x] |
| 4.2.3 | Send button enabled when input not empty | UI | P0 | [x] |
| 4.2.4 | Send button disabled when input empty | UI | P0 | [x] |
| 4.2.5 | Messages list renders correctly | UI | P0 | [x] |
| 4.2.6 | Messages list scrolls to bottom on new msg | UI | P1 | [x] |
| 4.2.7 | Loading indicator shows during send | UI | P1 | [x] |
| 4.2.8 | Error message displays correctly | UI | P1 | [x] |
| 4.2.9 | Close button calls onClose | UI | P1 | [x] |

### 4.3 ChatRepository
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 4.3.1 | Send message via WebSocket | Integration | P0 | [x] |
| 4.3.2 | Receive response via WebSocket | Integration | P0 | [x] |
| 4.3.3 | Handle connection loss during chat | Integration | P1 | [x] |
| 4.3.4 | Session persistence via daemon | Integration | P1 | [x] |

---

## 5. Database Panel

### 5.1 DatabaseViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 5.1.1 | Initial state has no connection | Unit | P0 | [x] |
| 5.1.2 | Connect sends correct parameters | Unit | P0 | [x] |
| 5.1.3 | Disconnect sends correct connectionId | Unit | P0 | [x] |
| 5.1.4 | Query sends SQL correctly | Unit | P0 | [x] |
| 5.1.5 | Load tables triggers listTables | Unit | P0 | [x] |
| 5.1.6 | Select table triggers getTableSchema | Unit | P0 | [x] |
| 5.1.7 | Show/hide connect dialog | Unit | P1 | [x] |
| 5.1.8 | Update connect form fields | Unit | P1 | [x] |
| 5.1.9 | Clear query result | Unit | P1 | [x] |

### 5.2 DatabaseRepository
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 5.2.1 | Connect sends db_connect event | Integration | P0 | [x] |
| 5.2.2 | db_connected updates activeConnection | Integration | P0 | [x] |
| 5.2.3 | db_disconnected clears activeConnection | Integration | P0 | [x] |
| 5.2.4 | db_query_result parses rows correctly | Integration | P0 | [x] |
| 5.2.5 | db_tables parses table list | Integration | P0 | [x] |
| 5.2.6 | db_schema parses columns correctly | Integration | P0 | [x] |
| 5.2.7 | Query with error shows error state | Integration | P1 | [x] |
| 5.2.8 | Multiple connections handled | Integration | P1 | [x] |

### 5.3 DatabasePanel (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 5.3.1 | Panel renders with header | UI | P0 | [x] |
| 5.3.2 | Connect button opens dialog | UI | P0 | [x] |
| 5.3.3 | Dialog form accepts all fields | UI | P0 | [x] |
| 5.3.4 | Port field accepts only numbers | UI | P0 | [x] |
| 5.3.5 | Connect button sends connection | UI | P0 | [x] |
| 5.3.6 | Disconnect button works | UI | P1 | [x] |
| 5.3.7 | Table list displays correctly | UI | P0 | [x] |
| 5.3.8 | Table click loads schema | UI | P0 | [x] |
| 5.3.9 | Query editor accepts SQL | UI | P0 | [x] |
| 5.3.10 | Execute button sends query | UI | P0 | [x] |
| 5.3.11 | Results table displays correctly | UI | P0 | [x] |
| 5.3.12 | Error message displays | UI | P1 | [x] |
| 5.3.13 | Loading indicator shows | UI | P1 | [x] |
| 5.3.14 | Close button works | UI | P1 | [x] |

---

## 6. Terminal

### 6.1 TerminalViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 6.1.1 | Create session sends terminal_create | Unit | P0 | [x] |
| 6.1.2 | Send input sends terminal_data | Unit | P0 | [ ] |
| 6.1.3 | Destroy session sends terminal_destroy | Unit | P0 | [ ] |
| 6.1.4 | Receive output updates session | Unit | P0 | [x] |
| 6.1.5 | Multiple sessions handled | Unit | P1 | [x] |
| 6.1.6 | Session list updates correctly | Unit | P1 | [x] |

### 6.2 TerminalWrapper (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 6.2.1 | Terminal renders correctly | UI | P0 | [x] |
| 6.2.2 | Input field accepts keyboard input | UI | P0 | [x] |
| 6.2.3 | Output displays correctly | UI | P0 | [x] |
| 6.2.4 | Auto-scroll on new output | UI | P1 | [x] |
| 6.2.5 | Copy/paste works | UI | P1 | [x] |
| 6.2.6 | Clear terminal works | UI | P1 | [x] |

### 6.3 Daemon PTY
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 6.3.1 | PTY creates successfully | Daemon | P0 | [x] |
| 6.3.2 | PTY receives input | Daemon | P0 | [x] |
| 6.3.3 | PTY produces output | Daemon | P0 | [x] |
| 6.3.4 | PTY handles resize | Daemon | P1 | [x] |
| 6.3.5 | PTY cleans up on destroy | Daemon | P0 | [x] |

---

## 7. File Explorer

### 7.1 FileExplorerViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 7.1.1 | Load root directory | Unit | P0 | [x] |
| 7.1.2 | Expand folder loads children | Unit | P0 | [x] |
| 7.1.3 | Collapse folder hides children | Unit | P1 | [x] |
| 7.1.4 | Select file updates state | Unit | P0 | [x] |
| 7.1.5 | Create file sends request | Unit | P1 | [x] |
| 7.1.6 | Delete file sends request | Unit | P1 | [x] |
| 7.1.7 | Rename file sends request | Unit | P1 | [x] |
| 7.1.8 | Search filters files | Unit | P1 | [x] |

### 7.2 FileExplorer (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 7.2.1 | File tree renders correctly | UI | P0 | [x] |
| 7.2.2 | Folder expand/collapse works | UI | P0 | [x] |
| 7.2.3 | File selection highlights | UI | P0 | [x] |
| 7.2.4 | File click calls onFileSelected | UI | P0 | [x] |
| 7.2.5 | Large file list scrolls smoothly | Perf | P0 | [ ] |
| 7.2.6 | Nested folders indented correctly | UI | P1 | [x] |
| 7.2.7 | File icons display correctly | UI | P1 | [x] |
| 7.2.8 | Context menu appears on right-click | UI | P1 | [x] |

### 7.3 File Operations via Daemon
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 7.3.1 | Read file returns content | Integration | P0 | [x] |
| 7.3.2 | Write file saves correctly | Integration | P0 | [x] |
| 7.3.3 | List directory returns entries | Integration | P0 | [ ] |
| 7.3.4 | Delete file removes entry | Integration | P1 | [x] |
| 7.3.5 | File not found returns error | Integration | P1 | [x] |

---

## 8. Tunnel

### 8.1 TunnelViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 8.1.1 | Start tunnel sends tunnel_start | Unit | P0 | [ ] |
| 8.1.2 | Stop tunnel sends tunnel_stop | Unit | P0 | [ ] |
| 8.1.3 | Receive URL updates state | Unit | P0 | [x] |
| 8.1.4 | Receive error updates error state | Unit | P1 | [x] |
| 8.1.5 | Active session tracked correctly | Unit | P0 | [x] |

### 8.2 TunnelPanel (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 8.2.1 | Panel renders with header | UI | P0 | [x] |
| 8.2.2 | Start button visible when no session | UI | P0 | [x] |
| 8.2.3 | Stop button visible when session active | UI | P0 | [x] |
| 8.2.4 | Tunnel URL displays correctly | UI | P0 | [x] |
| 8.2.5 | Status indicator shows connection state | UI | P1 | [x] |
| 8.2.6 | Copy URL button works | UI | P1 | [x] |
| 8.2.7 | Close button works | UI | P1 | [x] |

### 8.3 Daemon Tunnel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 8.3.1 | Cloudflared starts successfully | Daemon | P0 | [x] |
| 8.3.2 | Tunnel URL returned correctly | Daemon | P0 | [x] |
| 8.3.3 | SSH tunnel starts successfully | Daemon | P1 | [x] |
| 8.3.4 | Tunnel stops cleanly | Daemon | P0 | [x] |
| 8.3.5 | Tunnel handles network failure | Daemon | P1 | [x] |

---

## 9. Router

### 9.1 RouterViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 9.1.1 | Load config from daemon | Unit | P0 | [x] |
| 9.1.2 | Save config sends to daemon | Unit | P0 | [x] |
| 9.1.3 | Update provider list | Unit | P1 | [x] |
| 9.1.4 | Toggle active provider | Unit | P1 | [x] |
| 9.1.5 | Connection status updates | Unit | P0 | [x] |

### 9.2 RouterPanel (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 9.2.1 | Panel renders with header | UI | P0 | [x] |
| 9.2.2 | Config form displays current values | UI | P0 | [x] |
| 9.2.3 | Save button sends config | UI | P0 | [x] |
| 9.2.4 | Status indicator shows connection | UI | P1 | [x] |
| 9.2.5 | Provider list renders | UI | P1 | [x] |
| 9.2.6 | WebView dashboard loads | UI | P1 | [x] |
| 9.2.7 | Close button works | UI | P1 | [x] |

---

## 10. Skill System

### 10.1 SkillViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 10.1.1 | Load skill list on init | Unit | P0 | [x] |
| 10.1.2 | Search filters skills | Unit | P0 | [x] |
| 10.1.3 | Select skill loads detail | Unit | P0 | [x] |
| 10.1.4 | Clear search restores full list | Unit | P1 | [x] |
| 10.1.5 | Error state on load failure | Unit | P1 | [x] |

### 10.2 SkillRepository
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 10.2.1 | listSkills sends skill_list event | Integration | P0 | [ ] |
| 10.2.2 | skill_list_result parsed correctly | Integration | P0 | [ ] |
| 10.2.3 | getDetail sends skill_detail event | Integration | P0 | [ ] |
| 10.2.4 | skill_detail_result parsed correctly | Integration | P0 | [ ] |
| 10.2.5 | Handle daemon not available | Integration | P1 | [x] |

### 10.3 SkillPanel (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 10.3.1 | Panel renders with header | UI | P0 | [x] |
| 10.3.2 | Skill list displays correctly | UI | P0 | [x] |
| 10.3.3 | Search field filters skills | UI | P0 | [x] |
| 10.3.4 | Skill click opens detail dialog | UI | P0 | [x] |
| 10.3.5 | Detail dialog shows full info | UI | P0 | [x] |
| 10.3.6 | Loading indicator shows | UI | P1 | [x] |
| 10.3.7 | Error message displays | UI | P1 | [x] |
| 10.3.8 | Close button works | UI | P1 | [x] |

### 10.4 Daemon Skills
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 10.4.1 | SkillManager loads bundled skills | Daemon | P0 | [x] |
| 10.4.2 | SKILL.md parsed correctly | Daemon | P0 | [x] |
| 10.4.3 | Skill list returns all skills | Daemon | P0 | [x] |
| 10.4.4 | Skill detail returns full content | Daemon | P0 | [x] |
| 10.4.5 | Custom skills from workspace loaded | Daemon | P1 | [x] |

---

## 11. Workspace Canvas

### 11.1 CanvasViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 11.1.1 | Load file tree as graph nodes | Unit | P0 | [x] |
| 11.1.2 | Tree layout calculates positions | Unit | P0 | [x] |
| 11.1.3 | Pan offset updates correctly | Unit | P0 | [x] |
| 11.1.4 | Zoom level updates correctly | Unit | P0 | [x] |
| 11.1.5 | Select node updates state | Unit | P1 | [x] |
| 11.1.6 | Expand/collapse node works | Unit | P1 | [x] |

### 11.2 WorkspaceCanvas (Compose UI)
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 11.2.1 | Canvas renders nodes | UI | P0 | [x] |
| 11.2.2 | Canvas renders edges | UI | P0 | [x] |
| 11.2.3 | Pan gesture works (drag) | UI | P0 | [x] |
| 11.2.4 | Zoom gesture works (pinch) | UI | P0 | [x] |
| 11.2.5 | Zoom button in works | UI | P1 | [x] |
| 11.2.6 | Zoom button out works | UI | P1 | [x] |
| 11.2.7 | Node click selects node | UI | P0 | [x] |
| 11.2.8 | Large graph (100+ nodes) scrolls smooth | Perf | P0 | [ ] |
| 11.2.9 | File node click opens file | UI | P1 | [x] |
| 11.2.10 | Minimap renders correctly | UI | P1 | [x] |

---

## 12. Editor

### 12.1 EditorViewModel
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 12.1.1 | Open file creates tab | Unit | P0 | [x] |
| 12.1.2 | Close tab removes tab | Unit | P0 | [x] |
| 12.1.3 | Switch tab updates active | Unit | P0 | [x] |
| 12.1.4 | Content change updates state | Unit | P0 | [x] |
| 12.1.5 | Save file persists content | Unit | P0 | [x] |

### 12.2 SoraEditorWrapper
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 12.2.1 | Editor renders code | UI | P0 | [x] |
| 12.2.2 | Syntax highlighting works | UI | P0 | [x] |
| 12.2.3 | Line numbers display | UI | P1 | [x] |
| 12.2.4 | Auto-indent works | UI | P1 | [x] |
| 12.2.5 | Find/replace works | UI | P1 | [x] |

### 12.3 EditorTabManager
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 12.3.1 | Tabs display correctly | UI | P0 | [x] |
| 12.3.2 | Tab click switches file | UI | P0 | [x] |
| 12.3.3 | Tab close removes tab | UI | P0 | [x] |
| 12.3.4 | Modified indicator shows | UI | P1 | [x] |
| 12.3.5 | Tab overflow scrolls | UI | P1 | [x] |

---

## 13. Performance Checklist

### 13.1 Startup Performance
| # | Test Case | Target | Status |
|---|-----------|--------|--------|
| 13.1.1 | Cold start time | < 3s | [ ] |
| 13.1.2 | Warm start time | < 1s | [x] |
| 13.1.3 | Memory at startup | < 150MB | [x] |
| 13.1.4 | Daemon ready time | < 2s | [ ] |

### 13.2 Runtime Performance
| # | Test Case | Target | Status |
|---|-----------|--------|--------|
| 13.2.1 | Frame rate during scroll | 60fps | [ ] |
| 13.2.2 | Frame rate during pan/zoom | 60fps | [ ] |
| 13.2.3 | Memory after 10 min usage | < 300MB | [ ] |
| 13.2.4 | CPU during idle | < 5% | [x] |
| 13.2.5 | CPU during chat | < 20% | [ ] |
| 13.2.6 | Battery drain per hour | < 5% | [x] |

### 13.3 WebSocket Performance
| # | Test Case | Target | Status |
|---|-----------|--------|--------|
| 13.3.1 | Message throughput | > 1000/sec | [ ] |
| 13.3.2 | Latency (send → receive) | < 50ms | [x] |
| 13.3.3 | Reconnect time | < 5s | [x] |
| 13.3.4 | Large payload handling | < 10MB | [x] |

### 13.4 Database Performance
| # | Test Case | Target | Status |
|---|-----------|--------|--------|
| 13.4.1 | Query execution time | < 1s | [x] |
| 13.4.2 | Table list load time | < 500ms | [x] |
| 13.4.3 | Schema load time | < 500ms | [x] |
| 13.4.4 | Result set rendering (1000 rows) | < 500ms | [ ] |

### 13.5 Memory Performance
| # | Test Case | Target | Status |
|---|-----------|--------|--------|
| 13.5.1 | No memory leaks (LeakCanary) | 0 leaks | [ ] |
| 13.5.2 | No bitmap leaks | 0 leaks | [x] |
| 13.5.3 | No coroutine leaks | 0 leaks | [x] |
| 13.5.4 | WebSocket cleanup on disconnect | Clean | [x] |
| 13.5.5 | ViewModel cleanup on clear | Clean | [x] |

---

## 14. Security Checklist

### 14.1 Data Security
| # | Test Case | Type | Status |
|---|-----------|------|--------|
| 14.1.1 | Passwords stored encrypted | Unit | [x] |
| 14.1.2 | No secrets in logs | Security | [x] |
| 14.1.3 | No secrets in git | Security | [x] |
| 14.1.4 | WebView origin whitelist enforced | Unit | P0 | [x] |
| 14.1.5 | Daemon binds to 127.0.0.1 only | Unit | P0 | [x] |
| 14.1.6 | 9Router binds to 127.0.0.1 only | Unit | P0 | [x] |

### 14.2 Input Security
| # | Test Case | Type | Status |
|---|-----------|------|--------|
| 14.2.1 | SQL injection prevented | Security | [x] |
| 14.2.2 | XSS in WebView prevented | Security | [x] |
| 14.2.3 | Path traversal prevented | Security | [x] |
| 14.2.4 | Command injection prevented | Security | [x] |

---

## 15. Edge Cases

### 15.1 Network Edge Cases
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 15.1.1 | Daemon not running on startup | Unit | P0 | [x] |
| 15.1.2 | Daemon crashes during operation | Unit | P0 | [x] |
| 15.1.3 | Network disconnection mid-operation | Unit | P1 | [ ] |
| 15.1.4 | Slow network response | Unit | P1 | [ ] |
| 15.1.5 | Concurrent WebSocket messages | Unit | P1 | [x] |

### 15.2 UI Edge Cases
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 15.2.1 | Empty state (no files) | UI | P0 | [x] |
| 15.2.2 | Very long file name | UI | P1 | [x] |
| 15.2.3 | Very long SQL query | UI | P1 | [x] |
| 15.2.4 | No database connections | UI | P1 | [x] |
| 15.2.5 | No skills available | UI | P1 | [x] |
| 15.2.6 | Rotation doesn't crash | UI | P0 | [ ] |
| 15.2.7 | Background/foreground transition | UI | P0 | [x] |
| 15.2.8 | Low memory condition | UI | P1 | [ ] |

### 15.3 Data Edge Cases
| # | Test Case | Type | Priority | Status |
|---|-----------|------|----------|--------|
| 15.3.1 | Empty WebSocket message | Unit | P1 | [x] |
| 15.3.2 | Invalid JSON from daemon | Unit | P0 | [x] |
| 15.3.3 | Missing fields in payload | Unit | P1 | [x] |
| 15.3.4 | Null values in payload | Unit | P1 | [x] |
| 15.3.5 | Very large payload | Unit | P1 | [x] |
| 15.3.6 | Unicode in file names | Unit | P1 | [x] |
| 15.3.7 | Binary content in file | Unit | P1 | [x] |

---

## Summary

| Category | Total Tests | P0 | P1 | Status |
|----------|-------------|-----|-----|--------|
| Core Infrastructure | 10 | 7 | 3 | 9/10 Passed |
| WebSocket Client | 16 | 9 | 7 | 14/16 Passed |
| Daemon Communication | 36 | 32 | 4 | 22/36 Passed |
| AI Assistant | 23 | 14 | 9 | 23/23 Passed |
| Database Panel | 31 | 19 | 12 | 31/31 Passed |
| Terminal | 17 | 10 | 7 | 15/17 Passed |
| File Explorer | 21 | 10 | 11 | 19/21 Passed |
| Tunnel | 17 | 9 | 8 | 15/17 Passed |
| Router | 12 | 7 | 5 | 12/12 Passed |
| Skill System | 23 | 12 | 11 | 19/23 Passed |
| Workspace Canvas | 16 | 9 | 7 | 15/16 Passed |
| Editor | 15 | 8 | 7 | 15/15 Passed |
| Performance | 23 | 8 | 15 | 15/23 Passed |
| Security | 10 | 4 | 6 | 10/10 Passed |
| Edge Cases | 20 | 7 | 13 | 16/20 Passed |
| **TOTAL** | **290** | **165** | **125** | **240/290 Passed (82.7%)** |

---

## Priority Legend

| Priority | Description | Timeline |
|----------|-------------|----------|
| P0 | Critical - blocks release | Must fix before release |
| P1 | Important - should fix | Should fix before release |
| P2 | Nice to have | Can defer |

---

## How to Use This Checklist

1. **Before Development**: Review all P0 tests for the feature you're building
2. **During Development**: Write tests alongside code
3. **Before Commit**: Run `./gradlew test` and verify P0 tests pass
4. **Before PR**: Ensure coverage meets minimum targets
5. **Before Release**: All P0 tests must pass, 80%+ P1 tests should pass

---

*Last updated: Juli 2026*
*Project: Artier IDE Kotlin Native v2*
