---
name: git-commit
description: Draft clear conventional commit messages and summarize git diffs. Use when the user asks to commit, write a commit message, or summarize staged changes.
license: MIT
metadata:
  author: artier
  version: "1.0"
---

# Git Commit Skill

## Commit message rules

- Prefer [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `perf:`
- Subject line ≤ 72 chars, imperative mood
- Body explains **why**, not just what
- Never invent changes that are not in the diff

## Workflow

1. Inspect `git status` and `git diff` / staged diff
2. Group related changes into one logical commit when possible
3. Propose a message; wait for user confirmation before running `git commit`
4. Do not amend, force-push, or skip hooks unless the user explicitly asks
