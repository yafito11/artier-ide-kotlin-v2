---
name: code-review
description: Review code for bugs, security issues, style, and maintainability. Use when the user asks for a code review, PR review, or quality check.
license: MIT
metadata:
  author: artier
  version: "1.0"
---

# Code Review Skill

When reviewing code:

1. **Correctness** — logic errors, edge cases, null/empty handling
2. **Security** — injection, secrets, auth, path traversal
3. **Performance** — hot paths, unnecessary allocations, N+1 queries
4. **Readability** — naming, structure, dead code
5. **Tests** — missing coverage for critical paths

## Output format

- Summary (1–3 sentences)
- Findings ordered by severity: `critical` / `major` / `minor` / `nit`
- Each finding: file path, issue, suggested fix
- End with actionable next steps

Prefer concrete line-level feedback over vague advice.
