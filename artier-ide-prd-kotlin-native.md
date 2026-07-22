# PRD — Artier IDE (Varian: Kotlin Native + Sora Editor)

**Product:** Artier IDE
**Tagline:** *Agentic AI First*
**Platform:** Android (tablet), target RAM minimum **4GB**
**Varian arsitektur:** Kotlin native (Jetpack Compose) + Sora Editor + Termux TerminalView + Skia/Canvas native
**Versi dokumen:** v1.0 — draft untuk revisi
**Status:** Perencanaan awal — bandingkan dengan `artier-ide-prd-react-native.md`

> Dokumen ini adalah salah satu dari 2 opsi arsitektur. Baca juga varian **React Native** untuk perbandingan sebelum memutuskan. Ringkasan trade-off ada di §0.

---

## 0. Ringkasan Trade-off vs Varian React Native

| Aspek | Kotlin Native (dokumen ini) | React Native |
|---|---|---|
| Kecepatan development | Lebih lambat (semua UI ditulis native, lebih verbose, lebih sedikit library siap pakai untuk IDE) | Lebih cepat |
| RAM footprint | **Paling rendah** — nyaris tanpa WebView (hanya untuk 9Router/DB Studio, on-demand) | Sedikit lebih tinggi (JS bridge + WebView shared) |
| Performa editor/terminal | **Native murni** — Sora Editor & Termux TerminalView dirender langsung ke Canvas Android, paling responsif & hemat memori | Bergantung WebView (CodeMirror/xterm.js), ada overhead render web |
| Reuse skill kamu | Cocok kalau kamu mau invest penuh ke Kotlin/Android native | Cocok kalau lebih familiar JS/TS/React |
| Potensi ke iOS/desktop nanti | Sulit — kode native Android tidak portable, perlu ditulis ulang total | Lebih mudah (RN cross-platform) |
| Kompleksitas native module | Rendah — semua sudah native by default, tidak ada bridge JS↔native untuk fitur inti | Sedang — perlu bridge untuk beberapa fitur |
| Risiko performa di 4GB RAM | **Risiko paling kecil** secara default, arsitektur ini yang paling sesuai constraint RAM | Perlu disiplin ketat |

**Kapan pilih Kotlin Native:** kamu prioritaskan performa & efisiensi RAM maksimal (karena ini device 4GB, bukan sekadar "nice to have"), fokus Android-only jangka panjang, dan siap invest waktu development lebih besar di awal.

---

## 1. Ringkasan Produk

Artier IDE adalah **IDE mobile-native untuk Android** yang menjadikan AI coding agent (CLI) sebagai warga kelas satu — bukan fitur tempelan. Alih-alih membangun ulang agent AI dari nol, Artier **mengorkestrasi CLI agent yang sudah ada** (Claude Code, OpenCode, Hermes Agent, dll) lewat sistem adapter, dan menyediakan model-gateway (9Router) + database remote (VPS self-hosted) supaya device tetap ringan sementara compute berat terjadi di cloud/VPS milik user sendiri.

**Masalah yang diselesaikan:**
1. Tidak ada IDE mobile yang menjadikan multi-agent CLI sebagai pusat workflow (bukan cuma chat sidebar).
2. IDE desktop-style (Electron-based) tidak bisa jalan layak di tablet Android RAM rendah.
3. Developer yang pakai banyak CLI agent (Claude Code, OpenCode, Hermes) sering kena rate limit kalau terkunci ke satu provider/SDK.
4. Kebutuhan database yang portable lintas cloud tanpa vendor lock-in, tapi tetap ringan di device.

---

## 2. Goals & Non-Goals

### Goals (v1)
- IDE berjalan stabil di tablet Android 4GB RAM dengan footprint serendah mungkin — ini varian yang paling menargetkan efisiensi RAM.
- Mendukung banyak CLI coding agent sekaligus lewat adapter pattern (tidak lock-in ke 1 SDK/vendor).
- Skill system yang portable (kompatibel `SKILL.md` / agentskills.io).
- Model gateway terintegrasi (9Router) untuk auto-fallback & quota tracking lintas provider.
- Database bisa lokal (SQLite on-device) atau remote (VPS self-hosted, Postgres/libSQL).
- Preview dev-server (`npm run dev`, dst) bisa langsung jadi URL publik yang dibuka di Chrome Android.
- Layout adaptif: Workspace Canvas & AI panel bisa disembunyikan untuk mode "full code".

### Non-Goals (v1)
- ❌ Bukan pengganti IDE desktop penuh — fokus mobile-first.
- ❌ Tidak menjalankan model AI lokal di device (semua inference via API/cloud).
- ❌ Tidak reimplement agent AI sendiri dari nol — selalu memakai CLI/SDK yang sudah ada.
- ❌ **Tidak cross-platform** — varian ini eksplisit Android-only, tidak ada rencana port ke iOS/desktop tanpa rewrite total.

---

## 3. Target User

- Developer yang kerja/eksperimen coding dari tablet Android (mobile-first workflow, termasuk kamu sendiri).
- User yang sudah terbiasa pakai banyak CLI agent (Claude Code, OpenCode, Hermes, dll) dan ingin satu tempat untuk mengorkestrasi semuanya.
- User yang ingin kontrol penuh atas biaya AI (routing ke provider termurah/tersedia) dan data (self-hosted DB, bukan vendor lock-in).
- User yang **RAM device-nya sangat terbatas** sehingga performa native adalah prioritas non-negotiable.

---

## 4. Constraint Utama: RAM 4GB

| Alokasi | Perkiraan |
|---|---|
| Android OS + background apps | ~1.5–2 GB |
| **Sisa untuk Artier IDE** | ~1.8–2.2 GB |

Implikasi (varian ini menjawab constraint ini paling agresif):
- ❌ Tidak pakai Electron.
- ❌ Tidak pakai WebView untuk editor/terminal — pakai **Sora Editor** & **Termux TerminalView**, keduanya native Android (Canvas-based rendering, bukan Chromium).
- ❌ Tidak jalankan LLM lokal di device.
- ✅ WebView **hanya untuk 2 fitur non-inti**: panel 9Router dan DB Studio (Supabase Studio/pgAdmin) — keduanya on-demand, di-destroy saat panel ditutup, tidak pernah aktif bersamaan.
- ✅ Satu CLI agent aktif dalam satu waktu secara default.
- ✅ Canvas workspace pakai Jetpack Compose Canvas API (native, tanpa WebView).
- ✅ UI shell 100% Jetpack Compose — tidak ada Chromium yang di-render terus-menerus di background.

---

## 5. Arsitektur Tingkat Tinggi

```
┌───────────────────────────────────────────────────────────────┐
│  Artier IDE — Android App (Kotlin, Jetpack Compose)             │
│                                                                   │
│  ┌──────────────┐  ┌───────────────────┐  ┌─────────────────┐ │
│  │ Sidebar       │  │ Workspace Canvas   │  │ AI Assistant     │ │
│  │ (Compose)     │  │ (Compose Canvas    │  │ Panel (Compose)  │ │
│  │               │  │  API, native)      │  │                  │ │
│  └──────────────┘  └───────────────────┘  └─────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ Editor: Sora Editor (native View, TextMate grammar)          │ │
│  │ Terminal: TerminalView + TerminalEmulator (dari codebase      │ │
│  │           Termux, native Canvas rendering)                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ WebView on-demand (lazy, destroy saat ditutup) — EXCEPTION    │ │
│  │  - 9Router dashboard                                        │ │
│  │  - DB Studio (Supabase Studio / pgAdmin, opsional)           │ │
│  └───────────────────────────────────────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────────┘
                            │ OkHttp (HTTP) / OkHttp WebSocket
┌──────────────────────────▼──────────────────────────────────────┐
│  Linux Environment (proot, non-root — pola Termux/Tiny Container)│
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Artier Daemon (Node.js + TypeScript + Fastify)                │ │
│  │  - Agent adapter registry (spawn & parse CLI agent output)    │ │
│  │  - Skill loader (SKILL.md)                                    │ │
│  │  - File API, PTY bridge (node-pty), SQLite lokal               │ │
│  │  - DB proxy (pg client / @libsql/client) ke lokal atau VPS     │ │
│  │  - SSH tunnel manager, cloudflared tunnel manager               │ │
│  └────────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 9Router (subprocess, port 20128)                              │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ CLI agent (npm/pkg terpasang): Claude Code, OpenCode,          │ │
│  │ Hermes Agent, Codex, Gemini CLI, dll — spawn on-demand         │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ cloudflared (tunnel binary, on-demand)                         │ │
│  └────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
                            │ SSH tunnel / WireGuard (opsional)
┌──────────────────────────▼──────────────────────────────────────┐
│  VPS milik user (Podman rootless + podman-compose)                │
│  - libSQL/sqld (remote SQLite server)                              │
│  - Postgres + Supabase Studio (slim, self-hosted)                  │
└───────────────────────────────────────────────────────────────┘
```

**Perbedaan kunci vs varian RN:** tidak ada WebView untuk editor/terminal sama sekali. `TerminalView`/`TerminalEmulator` diambil langsung dari source Termux (open-source, battle-tested — dipakai jutaan install Termux itu sendiri), dan Sora Editor adalah library editor kode native Android yang sudah dipakai di aplikasi seperti AIDE.

---

## 6. Tech Stack Lengkap

### 6.1 App Layer (Kotlin Native)

| Komponen | Pilihan | Alasan |
|---|---|---|
| Bahasa | **Kotlin** | Standar Android modern |
| UI framework | **Jetpack Compose** (penuh, bukan hybrid) | Deklaratif, performa baik, dukungan animasi/collapsible panel native |
| Canvas/graphics | **Compose Canvas API** (`androidx.compose.ui.graphics.drawscope`) | Workspace Canvas (node graph, pan/zoom via `detectTransformGestures`) — native, tanpa WebView |
| Navigation | **Navigation Compose** | Standar Android |
| State management | **StateFlow / ViewModel** (Android Architecture Components) | Idiomatik Kotlin, tidak perlu library eksternal |
| Dependency Injection | **Hilt** (opsional, kalau kompleksitas app membutuhkan) | Standar Android untuk skala menengah-besar |
| Networking | **OkHttp** (HTTP client) + **OkHttp WebSocket** | Komunikasi ke daemon lokal |
| File system bridge | Storage Access Framework (SAF) langsung via Android API | Native, tidak perlu library tambahan |
| Secure storage | **Android Keystore** (via `EncryptedSharedPreferences` / Jetpack Security) | Simpan API key provider, credential VPS/DB terenkripsi |
| Build tool | **Gradle (Kotlin DSL)** | Standar Android |
| WebView (exception) | `android.webkit.WebView` bawaan Android, dipakai **hanya** untuk panel 9Router & DB Studio | Dua-duanya sudah web app sendiri (Next.js), tidak perlu reimplement UI-nya |

### 6.2 Editor (Native — bukan WebView)

| Komponen | Pilihan | Alasan |
|---|---|---|
| Editor engine | **[Sora Editor](https://github.com/Rosemoe/sora-editor)** | Library editor kode 100% native Android, dipakai di app seperti AIDE |
| Syntax highlighting | TextMate grammar (didukung native oleh Sora Editor) | Sama grammar engine yang dipakai VS Code, mendukung hampir semua bahasa |
| Autocomplete/LSP | Sora Editor mendukung integrasi LSP client custom | Autocomplete, diagnostics dari language server yang jalan di proot |
| Multi-tab | Dikelola di layer Compose (state ViewModel), Sora Editor instance per tab (dengan lazy init) | Hindari banyak instance editor aktif render bersamaan |

### 6.3 Terminal (Native — bukan WebView)

| Komponen | Pilihan | Alasan |
|---|---|---|
| Terminal view | **`TerminalView`** dari codebase Termux (open-source, terbukti di jutaan install) | Render langsung ke Canvas Android, tanpa overhead web rendering |
| Terminal emulator | **`TerminalEmulator`** (Termux) | Interpretasi escape sequence, scroll buffer, dll — sudah matang |
| PTY backend | `node-pty` di sisi daemon (Node.js), terhubung ke `TerminalView` via WebSocket dari app | Daemon tetap yang menjalankan proses shell sesungguhnya |

### 6.4 Daemon Layer (jalan di dalam proot) — sama dengan varian RN

| Komponen | Pilihan | Alasan |
|---|---|---|
| Runtime | **Node.js LTS** | Kompatibilitas terbaik dengan ekosistem CLI agent (semua Node-based) |
| Bahasa | **TypeScript** | Tipe konsisten dengan agent adapter contract |
| HTTP server | **Fastify** | Overhead memori lebih kecil dari Express |
| Realtime | **SSE** (stream output agent), **WebSocket `ws`** (terminal PTY, DB query stream) | Sesuai kebutuhan arah data |
| Storage lokal | **SQLite** (`better-sqlite3`) | Project metadata, riwayat chat, cache, koneksi profile |
| DB client | `pg` (Postgres), `@libsql/client` (SQLite lokal/remote) | Query proxy ke DB lokal maupun VPS |
| Proses management | `child_process.spawn` langsung | Hindari overhead PM2 di device |

> Catatan: daemon-nya **identik** di kedua varian PRD (RN dan Kotlin) — perbedaan hanya di App Layer (UI). Ini keuntungan desain: kamu bisa switch UI framework di masa depan tanpa menulis ulang daemon/agent system/DB layer.

### 6.5 Linux Runtime (Android, non-root) — sama dengan varian RN

| Komponen | Pilihan | Alasan |
|---|---|---|
| Container/isolation | **proot** (fork ala Tiny Container, atau reuse Termux terpasang) | Non-root, battle-tested, komunitas besar (termux-packages) |
| Package manager | `pkg` (Termux-style) | Install runtime bahasa on-demand (Node, Python, Go, dll) sesuai kebutuhan project |
| Tunnel dev-server | **cloudflared** (quick tunnel) | Single binary, ARM64 support, tanpa akun, gratis |
| Tunnel database | **SSH tunnel** (default) / WireGuard (opsional, fase lanjut) | Amankan akses ke VPS tanpa expose port publik |

### 6.6 Agent System — sama dengan varian RN

| Komponen | Pilihan | Alasan |
|---|---|---|
| Pola integrasi | **Adapter pattern** custom (terinspirasi Open Design `AgentAdapter`) | Tidak lock-in ke satu SDK/vendor, tambah CLI baru = 1 modul baru |
| Kontrak adapter | `detect()`, `spawn()`, `parseStream()` → event seragam (`tool_call`, `text_delta`, `error`, `result`) | Konsisten lintas CLI apa pun |
| CLI didukung (v1) | Claude Code, OpenCode (via `@opencode-ai/sdk` atau CLI langsung), Hermes Agent CLI | Prioritas berdasarkan riset sebelumnya |
| Fallback tanpa CLI | Tool-loop minimal (Read/Write/Edit) langsung di daemon, model via 9Router | Pola "api-fallback" ala Open Design |
| Skill format | `SKILL.md` (+`assets/`, `references/`) kompatibel **agentskills.io** | Portable lintas Claude Code/Hermes/Artier |

### 6.7 Model Gateway — 9Router (WebView exception)

| Aspek | Detail |
|---|---|
| Deployment | Subprocess yang di-spawn daemon, bind `127.0.0.1:20128` |
| UI | **WebView native Android** (`android.webkit.WebView`), dibuka **hanya saat panel diklik**, `destroy()` saat ditutup | Tidak reimplement UI dashboard 9Router |
| Fungsi | Auto-fallback, quota tracking, token reduction (RTK), format translation OpenAI/Claude/Gemini |
| Integrasi CLI agent | CLI agent dikonfigurasi menunjuk endpoint 9Router (`localhost:20128/v1`), bukan langsung ke provider asli |

### 6.8 Database — sama dengan varian RN

| Mode | Teknologi | Catatan |
|---|---|---|
| **Lokal** | SQLite file langsung (via `better-sqlite3` di daemon) | Default untuk prototyping cepat, offline |
| **Remote SQLite** | **libSQL/`sqld`** self-hosted di VPS (Docker image `ghcr.io/tursodatabase/libsql-server`) | SQLite yang bisa diakses network (HTTP/gRPC), auth via JWT |
| **Remote Postgres** | **Supabase self-hosted (slim)** — `supabase/postgres` + `supabase/studio` saja di Podman/VPS | Hindari full-stack Supabase yang berat untuk VPS kecil |
| Container di VPS | **Podman rootless** + `podman-compose` | Lebih aman, portable — pindah cloud tinggal `podman-compose down/up` + copy volume |
| Akses aman | **SSH tunnel** default (`ssh -L`), WireGuard opsional untuk fase lanjut | Jangan expose port DB langsung ke publik |
| UI di Artier | Panel **native Compose** (tabel, query runner, hasil grid, `LazyColumn` untuk grid data besar) via DB proxy daemon; fallback WebView on-demand ke Supabase Studio/pgAdmin | Compose lebih hemat RAM dibanding render grid di WebView |

### 6.9 Public URL / Dev Preview — sama dengan varian RN

| Aspek | Detail |
|---|---|
| Tool | **cloudflared** quick tunnel |
| Trigger | Daemon deteksi port baru listen → tombol "Make Public" muncul di panel terminal (Compose) |
| Output | URL `https://xxxxx.trycloudflare.com` |
| Buka di browser | Android Intent `ACTION_VIEW` dengan `setPackage("com.android.chrome")`, fallback browser default — **native langsung dari Kotlin**, tanpa perlu JS bridge sama sekali |
| Lifecycle | Tunnel mati otomatis saat dev-server berhenti atau app lama di-background |

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(publicUrl))
intent.setPackage("com.android.chrome")
try {
    startActivity(intent)
} catch (e: ActivityNotFoundException) {
    intent.setPackage(null)
    startActivity(intent)
}
```

---

## 7. Fitur (Scope per Fase)

### Fase 0 — Fondasi
- Bootstrap proot rootfs + daemon Node.js kosong + Compose app shell yang bisa render UI dasar dan komunikasi ke daemon via OkHttp.
- **Validasi:** app jalan stabil di device 4GB tanpa crash, RAM idle < 250MB (target lebih ketat dari varian RN karena tanpa WebView utama).

### Fase 1 — Editor & Terminal
- Integrasi Sora Editor + TerminalView (Termux), file explorer native Compose, tab management.
- **Validasi:** bisa edit file & jalankan `npm run dev` dari terminal, tanpa WebView aktif sama sekali di fase ini.

### Fase 2 — Public Tunnel
- Integrasi cloudflared + deteksi port otomatis + tombol buka di Chrome (native Intent).

### Fase 3 — 9Router Embed
- Subprocess 9Router + WebView on-demand (exception pertama) + konfigurasi CLI agent menunjuk endpoint-nya.

### Fase 4 — Agent Adapter Pertama
- Mulai dari **OpenCode**, lalu tambah **Claude Code** dan **Hermes Agent**.
- Panel AI Assistant native Compose (chat, pilih agent aktif, riwayat sesi).

### Fase 5 — Skill System
- Loader `SKILL.md`, browsable skill list (Compose `LazyColumn`), install skill dari folder/URL.

### Fase 6 — Database Panel
- Mode lokal (SQLite) dulu → tambah mode remote (libSQL VPS) → tambah mode Postgres (Supabase self-hosted) + SSH tunnel manager. UI grid native Compose.

### Fase 7 — Workspace Canvas
- Node graph via Compose Canvas API, auto-generate dari scan struktur project, toggle show/hide untuk mode "full code".

### Fase 8 — Polish & Optimisasi RAM
- Audit memory real-device (Android Profiler), lazy-load agresif, idle daemon suspend, minimalkan recomposition Compose yang tidak perlu.

---

## 8. Praktik Manajemen Memori (Wajib)

1. **WebView hanya untuk 9Router & DB Studio** — tidak pernah untuk editor/terminal.
2. **Satu instance editor Sora Editor per tab aktif yang terlihat** — tab lain di-suspend (tidak render), bukan semua tab aktif bersamaan.
3. **1 proses CLI agent aktif** secara default; multi-agent paralel = opt-in eksplisit dengan warning RAM.
4. **Streaming, bukan buffering penuh** — file besar & output CLI panjang selalu di-chunk.
5. **Canvas native (Compose Canvas API)** — tanpa dependency graphics library eksternal berat.
6. **Idle daemon suspend** saat app di-background lama, resume cepat dari state SQLite.
7. **Minimalkan recomposition Compose** — pakai `remember`, `derivedStateOf` dengan disiplin, hindari state global yang trigger render berlebihan.
8. **Test di device fisik 4GB** dengan **Android Profiler** aktif sejak fase awal, bukan emulator.

---

## 9. Keamanan

- API key provider & credential VPS/DB disimpan di **Android Keystore** (`EncryptedSharedPreferences`), tidak pernah plaintext di SQLite lokal.
- Akses DB remote **wajib lewat tunnel** (SSH/WireGuard), tidak pernah expose port DB langsung ke internet.
- 9Router & daemon bind ke `127.0.0.1` saja secara default — tidak reachable dari network luar device.
- Tunnel publik (cloudflared) untuk dev-preview **auto-expire**/dimatikan saat idle — bukan tunnel permanen.
- WebView untuk 9Router/DB Studio dikonfigurasi dengan **JavaScript origin whitelist** ketat (hanya izinkan `localhost`), cegah kebocoran ke domain luar.

---

## 10. Metrik Sukses (v1)

| Metrik | Target |
|---|---|
| RAM idle (app terbuka, tanpa project) | **< 250 MB** (lebih ketat dari varian RN karena tanpa WebView utama) |
| RAM aktif (editor + terminal + 1 agent aktif) | **< 900 MB** |
| Waktu cold start app | < 3 detik (Compose native umumnya lebih cepat start dari RN bridge init) |
| Waktu spawn CLI agent pertama kali | < 3 detik (setelah CLI ter-install) |
| Crash rate | < 1% sesi |

---

## 11. Risiko & Pertanyaan Terbuka (untuk direvisi)

1. **Investasi waktu development** — varian ini butuh lebih banyak kerja manual di UI (semua komponen ditulis native, tidak ada library IDE-siap-pakai sekomplet ekosistem web). Perlu dikonfirmasi: apakah kamu punya waktu/kenyamanan cukup dengan Kotlin/Compose untuk semua ini?
2. **Integrasi LSP ke Sora Editor** — perlu riset lebih lanjut soal cara terbaik menghubungkan Sora Editor ke language server (protokol LSP standar) — apakah ada contoh implementasi yang bisa dijadikan referensi.
3. **Distribusi proot rootfs** — sama seperti varian RN: bundle sendiri (ala Tiny Container) atau bergantung ke Termux terpisah?
4. **Skala Workspace Canvas** — v1 manual (user susun sendiri) atau auto-detect dari scan kode sejak awal?
5. **Prioritas CLI agent** — apakah urutan OpenCode → Claude Code → Hermes di Fase 4 sudah sesuai preferensi kamu?
6. **Supabase slim vs full-stack** — fitur Supabase apa saja yang benar-benar kamu butuhkan (Auth? Storage? Realtime?)?
7. **Kemungkinan ekspansi platform** — kalau ada rencana suatu saat mau ke iOS/desktop juga, ini pertimbangan besar untuk **tidak** memilih varian Kotlin native (karena tidak portable), atau kalau tetap Android-only selamanya, ini justru pilihan paling optimal.

---

## 12. Ringkasan Tech Stack (Quick Reference)

| Layer | Pilihan |
|---|---|
| App framework | Kotlin + Jetpack Compose (native, bukan hybrid) |
| State | ViewModel + StateFlow |
| Canvas | Compose Canvas API (native) |
| Editor | **Sora Editor** (native) |
| Terminal | **Termux TerminalView + TerminalEmulator** (native) + node-pty (backend) |
| Networking | OkHttp + OkHttp WebSocket |
| Secure storage | Android Keystore (EncryptedSharedPreferences) |
| Daemon | Node.js + TypeScript + Fastify + SQLite (better-sqlite3) — *sama dengan varian RN* |
| Linux runtime | proot (non-root, ala Termux/Tiny Container) — *sama dengan varian RN* |
| Agent system | Adapter pattern custom + SKILL.md loader — *sama dengan varian RN* |
| Model gateway | 9Router (subprocess + WebView dashboard on-demand, exception) |
| DB lokal | SQLite |
| DB remote SQLite | libSQL/sqld self-hosted (Docker/Podman di VPS) |
| DB remote Postgres | Supabase self-hosted slim (Podman di VPS) |
| Akses DB remote | SSH tunnel (default), WireGuard (opsional) |
| Dev-preview tunnel | cloudflared quick tunnel |
| Container VPS | Podman rootless + podman-compose |
| WebView | **Hanya** untuk 9Router & DB Studio, on-demand, destroy saat ditutup |
