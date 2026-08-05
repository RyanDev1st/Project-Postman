# 0002 — Where the working rules live

- **Status:** accepted
- **Date:** 2026-08-05
- **Deciders:** IT team

## Context

The working rules were written into `docs/reference/working-rules.md`, with a short summary in `CLAUDE.md`. Claude Code also supports a `.claude/rules/` folder. Three possible homes, and the same rule text starting to appear in two of them.

Two questions had to be settled: which folder, and what to do about the duplication.

**One observed fact settled the folder question.** A rules file in the user's global `~/.claude/rules/` was injected into the agent's context at the start of this session, before any prompt — the same as `CLAUDE.md`. So `rules/` files load **always**. The `paths:` frontmatter organizes them; it does not keep them out of context until a matching file is touched.

That removes the usual reason to split: it does not save context.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| **A. All rules in `.claude/rules/`** | Matches the Claude Code convention. Tidy | Teammates never look in `.claude/`. Rules the humans do not read are not team rules. No context saving to justify it |
| **B. All rules in `CLAUDE.md`** | One file, no drift | Pushes past the 200-line cap immediately. Mixes "why we work this way" with "how to tick a checkbox" |
| **C. Split by audience: doc for humans, summary in `CLAUDE.md`, `.claude/rules/` reserved for file-type rules** | Each reader gets what they need. Team rules stay in the repo where the team reads | The rules exist in two places and can drift |

## Decision

**Option C**, with one addition that answers its weakness: `docs/reference/working-rules.md` is named as the **single authority**. The `CLAUDE.md` block is explicitly a summary. If the two disagree, the doc wins, and the summary is corrected in the same change.

`.claude/rules/` stays empty for now.

## Why

The rules are for people first. Teammates, and the Server team, read the repo — they do not open a hidden agent folder. A rule nobody on the team reads is not a team rule.

The agent still needs the core of it in context on every task, so a short summary stays in `CLAUDE.md`. Naming one file as the authority makes the duplication safe rather than pretending it does not exist.

`.claude/rules/` is not used yet because `src/` is empty. File-type rules for a stack that has not been chosen (task P0-01) would be invented, not observed — which is the exact failure the empirical rules in group A exist to prevent.

## What we accept

The same rules appear in two files, and can drift. We take that on purpose, because the alternative is either rules the team never reads, or a `CLAUDE.md` over its size cap. The authority line and the same-change requirement are the controls.

## When to revisit

Create `.claude/rules/<topic>.md` when **both** are true:

1. `src/` holds real code, so a rule can name real file types.
2. `CLAUDE.md` is near 200 lines.

Likely first files, after P0-01: one for UI files, one for the API-call layer from P1-04.

## Affects

- `CLAUDE.md` — **How we work** block marked as a summary; **Maintaining this file** gained the placement table
- `docs/reference/working-rules.md` — named as the authority
- Unblocks nothing. Blocks nothing.
