# Fase 6: Database Panel — Planning Detail

## Goal
Panel database native Compose untuk connect ke local SQLite, remote libSQL, dan remote Postgres. UI grid, query runner, schema browser — semua via daemon DB proxy (WebSocket).

## Scope
- Mode lokal (SQLite on-device via daemon) — default untuk prototyping
- Mode remote SQLite (libSQL/sqld self-hosted di VPS)
- Mode remote Postgres (Supabase self-hosted / self-managed)
- SSH tunnel manager untuk akses aman ke DB remote
- UI grid native Compose (LazyColumn untuk result set besar)
- Connection profile management (simpan koneksi untuk reuse)

## Komponen

### Daemon (sudah selesai)
- `src/database/db-client.ts` — proxy client Postgres + libSQL
- `src/database/db-manager.ts` — internal SQLite untuk app data
- WS handlers: `db_connect`, `db_disconnect`, `db_query`, `db_tables`, `db_schema`
- REST endpoints: `/api/db/*`

### Android (perlu dibangun)
- `data/model/DatabaseModels.kt` — DbConnection, QueryResult, ColumnInfo, DatabasePanelState
- `data/repository/DatabaseRepository.kt` — WS-based repository untuk DB proxy operations
- `ui/database/DatabaseViewModel.kt` — Hilt ViewModel
- `ui/database/DatabasePanel.kt` — Full Compose UI
  - Connection manager (pilih type, input host/port/credentials)
  - SQL query editor (OutlinedTextField monospace)
  - Result grid (LazyColumn dengan header row)
  - Schema browser (list tabel + kolom)
- WebSocket event extensions (db_connected, db_disconnected, db_query_result, db_tables, db_schema)

## Validation
1. Bisa connect ke local SQLite via daemon
2. Bisa connect ke remote libSQL/Postgres via daemon
3. Query execution menampilkan result grid
4. Schema browser menampilkan daftar tabel dan kolom
5. Connection profile bisa disimpan dan di-reuse
