# src

Source for both front-ends. One folder each — they share no code, only the API contract.

| Folder | What it is | Stack | Started |
| --- | --- | --- | --- |
| `cabinet/` | The cabinet screen, for the shipper. A public terminal, no login | Web page — [ADR 0009](../docs/adr/0009-start-phase-1-early.md) | P1-03 ✅ |
| `app/` | The phone app, for the receiver | Kotlin, native Android — [ADR 0001](../docs/adr/0001-tech-stack.md) | P1-01, not yet |

## Folder rules

From `CLAUDE.md`. They apply from the first file.

1. **Feature folders only.** Group by what the code does for the user, not by file type. `src/app/scan/`, not `src/components/` and `src/services/`.
2. **Nest.** More than 5–7 files in one folder means it is doing too much. Extract a sub-folder.
3. **Keep things together.** A feature's code, types, tests and local notes live in that feature's folder.
4. **No dumping ground.** Never put feature logic in a global `utils/` or `types/` folder.
5. **300 lines per file, hard cap.** Over it → split within the same feature folder.

## Planned shape

A sketch, not a rule. Task IDs say when each part arrives.

```
src/
├── cabinet/          the shipper's screen — one web page
│   ├── index.html    P1-03 ✅ skeleton: one screen, a version number
│   ├── screen.css    never assumes a screen size (P0-02 is deferred)
│   ├── version.js    the one place the version is written
│   ├── api/          every server call goes through here (P1-06)
│   ├── drop/         find the receiver, open a box (P3-01..07)
│   ├── session/      the QR the phone scans (P5-01)
│   └── collect/      the typed backup code (P5-08, P5-09)
│
└── app/              the receiver's phone app — Kotlin
    ├── api/          one door — every network call goes through here (P1-04)
    ├── auth/         register by phone number, keep the token (P2-01..07)
    ├── parcels/      what is waiting, and where (P4-04)
    ├── scan/         read the QR on the cabinet screen (P5-02)
    └── history/      past parcels (P7-01)
```

**The cabinet screen never gets a login, a parcel list, or a full name.** Anyone can walk up to it.
