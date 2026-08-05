# src

App source code. **Empty until task P0-01 picks the tech stack**, then P1-01 creates the project here.

## Folder rules

From `CLAUDE.md`. They apply from the first file.

1. **Feature folders only.** Group by what the code does for the user, not by file type. `src/locker/open/`, not `src/components/` and `src/services/`.
2. **Nest.** More than 5–7 files in one folder means it is doing too much. Extract a sub-folder.
3. **Keep things together.** A feature's code, types, tests and local notes live in that feature's folder.
4. **No dumping ground.** Never put feature logic in a global `utils/` or `types/` folder.
5. **300 lines per file, hard cap.** Over it → split within the same feature folder.

## Planned shape

This is a sketch, not a rule. Adjust it once the stack is chosen, and update this file.

```
src/
├── api/              one door — every network call goes through here (P1-04)
├── auth/
│   ├── login/        login screen, both account types (P2-01..03)
│   └── session/      token storage, expiry, logout (P2-04..06)
├── points/           locker point list, free counts, nearest (P3-01..04)
├── locker/
│   ├── list/         lockers in a point, size, state (P3-05..07)
│   ├── open/         open and lock, safety rules (P4-01..08)
│   └── history/      my open and lock history (P4-07)
├── scan/             camera, barcode, fallback (P5-01..07)
├── mode/             normal and exam season (P6-01..05)
└── handoff/          parcel drop and collect — Phase 7, later
```
