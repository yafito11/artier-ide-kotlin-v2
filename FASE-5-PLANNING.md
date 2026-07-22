# Fase 5: Skill System — Planning Detail

## Status: ✅ COMPLETED

## Goal
Loader `SKILL.md` kompatibel agentskills.io, daftar skill browsable (Compose LazyColumn), install dari folder/URL, dan inject skill aktif ke konteks agent.

## Scope
- Parse frontmatter YAML + body Markdown (`name`, `description`, optional `license`, `compatibility`, `metadata`, `allowed-tools`)
- Discovery paths:
  - `~/.artier/skills/<name>/SKILL.md` (user global)
  - `~/.agents/skills/<name>/SKILL.md` (agentskills convention)
  - `<project>/.artier/skills/` dan `<project>/.agents/skills/`
  - Bundled sample skills di `daemon/skills/`
- REST + WebSocket API di daemon
- UI native Compose: list, detail, enable/disable, install from path/URL
- Progressive disclosure: metadata dulu; full body saat diaktifkan / diminta agent

## Komponen

### Daemon
- `src/skills/skill-manager.ts`
- Routes: `GET /api/skills`, `GET /api/skills/:name`, `POST /api/skills/install`, `POST /api/skills/scan`, `DELETE /api/skills/:name`
- WS: `skill_list`, `skill_get`, `skill_install`, `skill_uninstall`, `skill_scan`, `skill_set_enabled`

### Android
- `data/model/SkillModels.kt`
- `data/repository/SkillRepository.kt`
- `ui/skills/SkillPanel.kt`, `SkillViewModel.kt`
- Integrasi ke `AiViewModel` (inject enabled skill bodies ke prompt tool-loop / system context)

## Validation
1. Skill sample ter-load dari bundled path
2. List skill muncul di panel Compose
3. Install dari folder lokal
4. Enable skill → body masuk ke agent context
5. Format name/description valid agentskills.io
