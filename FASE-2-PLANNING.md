# Fase 2: Public Tunnel - Planning Detail

## Goal
Integrasi cloudflared untuk membuat dev server bisa diakses publik via URL, dengan auto-detect port dan UI management.

## Duration
1-2 minggu

## Scope
- Integrasi cloudflared binary untuk ARM64
- Auto-detect port listening baru
- Tombol "Make Public" di panel terminal
- URL display dengan copy to clipboard
- Auto-open di Chrome via Android Intent
- Tunnel auto-expire saat idle

## Komponen yang Dibangun

### 1. Cloudflared Integration
**Fitur:**
- Download dan install cloudflared binary
- Spawn cloudflared process
- Parse tunnel URL dari output
- Handle tunnel lifecycle

**Setup:**
- Download cloudflared binary untuk ARM64
- Store binary di assets atau download on-demand
- Spawn process dengan argument `tunnel --url http://localhost:PORT`

### 2. Port Detection
**Fitur:**
- Monitor port listening baru
- Auto-detect dev server (npm, python, dll)
- Trigger "Make Public" button

**Setup:**
- Monitor `/proc/net/tcp` atau gunakan `netstat`
- Filter port yang baru listen
- Match dengan dev server patterns

### 3. UI Components
**Fitur:**
- "Make Public" button di terminal panel
- URL display dengan copy button
- Open in Chrome button
- Tunnel status indicator

**Setup:**
- Compose UI components
- State management untuk tunnel
- Clipboard manager

### 4. Android Intent
**Fitur:**
- Buka URL di Chrome
- Fallback ke browser default
- Handle Chrome not installed

**Setup:**
- Intent.ACTION_VIEW
- setPackage("com.android.chrome")
- Fallback ke default browser

## Implementation Steps

### Minggu 1: Core Integration

**Hari 1-2: Cloudflared Setup**
- Download cloudflared binary untuk ARM64
- Setup binary management
- Implement tunnel spawn

**Hari 3-4: Port Detection**
- Implement port monitoring
- Auto-detect dev server
- Event system untuk notifikasi

**Hari 5-7: Tunnel Management**
- Tunnel lifecycle management
- URL parsing dari cloudflared output
- Error handling dan recovery

### Minggu 2: UI & Integration

**Hari 1-2: UI Components**
- "Make Public" button
- URL display dengan copy
- Status indicator

**Hari 3-4: Android Intent**
- Chrome intent implementation
- Fallback handling
- Share URL functionality

**Hari 5-7: Testing & Polish**
- Integration testing
- Error handling
- Performance optimization

## File Structure
```
artier-ide-kotlin-v2/
├── app/
│   └── src/main/java/com/artier/ide/
│       ├── ui/
│       │   ├── tunnel/
│       │   │   ├── TunnelPanel.kt
│       │   │   ├── TunnelViewModel.kt
│       │   │   └── TunnelState.kt
│       │   └── components/
│       │       └── UrlDisplay.kt
│       ├── data/
│       │   ├── model/
│       │   │   └── TunnelSession.kt
│       │   └── remote/
│       │       └── CloudflaredManager.kt
│       └── utils/
│           ├── IntentUtils.kt
│           └── ClipboardUtils.kt
├── daemon/
│   └── server.js (update dengan tunnel endpoints)
└── assets/
    └── cloudflared (binary)
```

## API Endpoints (Daemon)

### Create Tunnel
```json
// Request
{
  "type": "tunnel_create",
  "payload": {
    "port": 3000,
    "service": "npm"
  }
}

// Response
{
  "type": "tunnel_created",
  "payload": {
    "tunnelId": "abc123",
    "url": "https://xxxx.trycloudflare.com",
    "port": 3000
  }
}
```

### Close Tunnel
```json
// Request
{
  "type": "tunnel_close",
  "payload": {
    "tunnelId": "abc123"
  }
}

// Response
{
  "type": "tunnel_closed",
  "payload": {
    "tunnelId": "abc123"
  }
}
```

### Get Tunnel Status
```json
// Request
{
  "type": "tunnel_status",
  "payload": {
    "tunnelId": "abc123"
  }
}

// Response
{
  "type": "tunnel_status",
  "payload": {
    "tunnelId": "abc123",
    "status": "active",
    "url": "https://xxxx.trycloudflare.com",
    "uptime": 3600
  }
}
```

## Validation Criteria
1. ✅ Cloudflared binary bisa di-download dan dijalankan
2. ✅ Tunnel bisa dibuat untuk port tertentu
3. ✅ URL tunnel bisa di-copy ke clipboard
4. ✅ URL bisa dibuka di Chrome
5. ✅ Tunnel auto-close saat idle
6. ✅ Error handling untuk cloudflared failures

## Risks & Mitigations

### 1. Cloudflared Binary Size
**Risk:** Binary cloudflared cukup besar (~30MB)
**Mitigation:** Download on-demand, cache di app storage

### 2. Network Restrictions
**Risk:** Beberapa network memblokir tunnel
**Mitigation:** Fallback ke manual URL, error handling yang baik

### 3. Port Detection Accuracy
**Risk:** Sulit detect semua jenis dev server
**Mitigation:** Manual port input sebagai fallback

## Dependencies

### Android App
- OkHttp untuk HTTP requests
- Android ClipboardManager
- Intent API

### Daemon
- child_process untuk spawn cloudflared
- fs untuk binary management
- net untuk port detection

## Testing Strategy

### Unit Tests
- Tunnel state management
- URL parsing
- Intent creation

### Integration Tests
- Cloudflared spawn
- Port detection
- Tunnel lifecycle

### UI Tests
- Button interactions
- URL display
- Copy to clipboard

## Success Metrics
1. Tunnel creation < 5 detik
2. URL copy成功率 > 99%
3. Chrome open success rate > 95%
4. Tunnel auto-close setelah 5 menit idle
5. Zero crash pada tunnel operations