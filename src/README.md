# src

Source for both front-ends, one folder each — they share no code, only the API contract. Plus one server we do **not** ship.

| Folder | What it is | Stack | Started |
| --- | --- | --- | --- |
| `cabinet/` | The cabinet screen, for the shipper. A public terminal, no login | Web page — [ADR 0009](../docs/adr/0009-start-phase-1-early.md) | P1-03 ✅ |
| `app/` | The phone app, for the receiver | Kotlin, native Android — [ADR 0001](../docs/adr/0001-tech-stack.md) | P1-01, not yet |
| `otp-server/` | **Superseded, and no longer built.** It was the reference login server written before we owned the real one. [ADR 0019](../docs/adr/0019-we-own-the-server.md) and [ADR 0020](../docs/adr/0020-the-server-is-kotlin.md) made `server/` ours, and this stopped being a Gradle module — nothing in `settings.gradle.kts` includes it, so it does not compile as part of the build and cannot be run. Kept, not deleted, because `/auth/google`, `/auth/set-password` and `/auth/password-login` are written and tested here and P2-08 and P2-09 need them ported | Kotlin, Ktor | dead — source for P2-08, P2-09 |

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
    └── src/main/kotlin/vn/edu/vgu/smartlocker/
        ├── MainActivity.kt   which screen is showing
        ├── ScreenFrame.kt    the edges every screen shares
        ├── auth/             phone number, one-time code (P2-01..07)
        ├── parcels/          what is waiting, and history (P4-04, P7-01)
        ├── pickup/           scan, typed code, opened (P5-02, P5-08)
        └── api/              one door — every network call (P1-04)
```

The seven screens, and the rule each one follows, are in
[docs/reference/app-screens.md](../docs/reference/app-screens.md). They exist
and can be tapped through; none of them talks to a server yet.

`src/app/` is a Gradle module, so its own sources sit under `src/app/src/main/`.
That doubling is Gradle's layout, kept on purpose — a team learning Android
from zero should find the paths that every tutorial shows.

**The cabinet screen never gets a login, a parcel list, or a full name.** Anyone can walk up to it.
