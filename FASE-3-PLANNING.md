# Fase 3: 9Router Embed - Planning Detail

## Goal
Integrasi 9Router sebagai model gateway dengan WebView on-demand untuk dashboard, dan konfigurasi CLI agent.

## Duration
2-3 minggu

## Scope
- Subprocess 9Router management
- WebView on-demand untuk dashboard (exception pertama)
- Model gateway functionality (auto-fallback, quota tracking)
- Konfigurasi CLI agent menunjuk endpoint 9Router
- Token reduction (RTK) dan format translation

## Komponen yang Dibangun

### 1. 9Router Process Manager
**Fitur:**
- Spawn 9Router sebagai subprocess
- Health monitoring
- Auto-restart jika crash
- Port management (default: 20128)

**Setup:**
- 9Router binary/Node.js server
- Process lifecycle management
- Configuration management

### 2. WebView Dashboard (On-Demand)
**Fitur:**
- Lazy WebView initialization
- Destroy saat panel ditutup
- JavaScript whitelist untuk keamanan
- Dashboard URL: http://localhost:20128

**Setup:**
- Android WebView component
- Lifecycle management
- Security configuration

### 3. Model Gateway
**Fitur:**
- Auto-fallback antar provider
- Quota tracking
- Token reduction (RTK)
- Format translation (OpenAI/Claude/Gemini)

**Setup:**
- Provider configuration
- Request/response transformation
- Usage tracking

### 4. CLI Agent Configuration
**Fitur:**
- Auto-configure CLI agents ke endpoint 9Router
- Environment variable management
- Config file generation

**Setup:**
- Agent config templates
- Config injection
- Validation

## Implementation Steps

### Minggu 1: Core Integration

**Hari 1-2: 9Router Setup**
- Create 9Router server (Node.js/Express)
- Implement model gateway logic
- Provider management

**Hari 3-4: Process Management**
- Spawn 9Router as subprocess
- Health monitoring
- Auto-restart logic

**Hari 5-7: WebView Integration**
- Lazy WebView component
- Lifecycle management
- Security configuration

### Minggu 2: Gateway Features

**Hari 1-2: Auto-Fallback**
- Provider selection logic
- Failover handling
- Retry mechanisms

**Hari 3-4: Quota Tracking**
- Usage monitoring
- Rate limiting
- Cost calculation

**Hari 5-7: Format Translation**
- OpenAI format
- Claude format
- Gemini format

### Minggu 3: CLI Integration

**Hari 1-2: Agent Configuration**
- Config templates
- Auto-configuration
- Validation

**Hari 3-4: Testing & Polish**
- Integration testing
- Error handling
- Performance optimization

## File Structure
```
artier-ide-kotlin-v2/
├── app/
│   └── src/main/java/com/artier/ide/
│       ├── ui/router/
│       │   ├── RouterPanel.kt
│       │   ├── RouterViewModel.kt
│       │   └── WebViewDashboard.kt
│       ├── data/
│       │   ├── model/
│       │   │   ├── RouterConfig.kt
│       │   │   ├── Provider.kt
│       │   │   └── QuotaUsage.kt
│       │   └── remote/
│       │       └── RouterManager.kt
│       └── utils/
│           └── ConfigUtils.kt
├── daemon/
│   └── router/
│       ├── server.js
│       ├── providers/
│       │   ├── openai.js
│       │   ├── claude.js
│       │   └── gemini.js
│       └── middleware/
│           ├── fallback.js
│           ├── quota.js
│           └── format.js
└── assets/
    └── config/
        ├── agents/
        │   ├── claude-code.json
        │   ├── opencode.json
        │   └── hermes.json
        └── providers.json
```

## API Endpoints

### 9Router Server (Port 20128)

**Chat Completion**
```json
POST /v1/chat/completions
{
  "model": "gpt-4",
  "messages": [...],
  "stream": true
}
```

**Models List**
```json
GET /v1/models
```

**Quota Status**
```json
GET /api/quota
```

**Provider Status**
```json
GET /api/providers
```

## Validation Criteria
1. ✅ 9Router bisa dijalankan sebagai subprocess
2. ✅ WebView bisa dibuka dan ditutup (on-demand)
3. ✅ Auto-fallback antar provider berfungsi
4. ✅ Quota tracking akurat
5. ✅ CLI agents bisa menggunakan endpoint 9Router
6. ✅ Format translation berfungsi

## Risks & Mitigations

### 1. WebView Memory Usage
**Risk:** WebView memakan banyak memori
**Mitigation:** Lazy initialization, destroy saat ditutup

### 2. 9Router Stability
**Risk:** 9Router crash atau hang
**Mitigation:** Health monitoring, auto-restart

### 3. Provider API Keys
**Risk:** Keys tidak tersedia atau expired
**Mitigation:** Validation, graceful degradation

## Dependencies

### Android App
- Android WebView
- OkHttp untuk API calls
- Hilt untuk DI

### 9Router Server
- Node.js
- Express
- axios untuk HTTP requests

## Testing Strategy

### Unit Tests
- Config parsing
- Format translation
- Quota calculation

### Integration Tests
- 9Router spawn
- WebView lifecycle
- CLI agent configuration

### UI Tests
- Panel interactions
- WebView loading
- Error displays

## Success Metrics
1. 9Router startup < 3 detik
2. WebView load time < 2 detik
3. Fallback latency < 500ms
4. Quota tracking accuracy > 99%
5. Zero memory leaks