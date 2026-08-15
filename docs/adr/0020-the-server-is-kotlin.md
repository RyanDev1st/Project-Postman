# ADR 0020 — The server is Kotlin, Ktor and SQLite

- **Status:** accepted
- **Date:** 2026-08-15
- **Decided by:** Claude, an hour after [ADR 0019](0019-we-own-the-server.md), on
  finding that the repo had already half-made this decision and nobody had
  written it down
- **Reverses:** one row of [ADR 0019](0019-we-own-the-server.md) — the row that
  said the Server team's **FastAPI + SQLAlchemy + MySQL** stack was kept. It is
  not kept. Everything else in ADR 0019 stands
- **Replaces:** the scope of
  [the OTP sender design](../superpowers/specs/2026-08-09-otp-sender-design.md),
  which chose Kotlin and Ktor for endpoints 1 to 4 only

## Why ADR 0019 was wrong within the hour

ADR 0019 kept FastAPI on one argument: *they know it, so they can still read
what we write*. That argument died the same morning it was made. The Server team
build hardware now. Nobody on the Python side is reading our server.

And the repo had already decided. None of this was written by this session:

| Already in the repo | Where |
| --- | --- |
| `include(":otp-server")`, pointing at `src/otp-server` | `settings.gradle.kts` |
| Ktor 3.2.0, five artifacts | `gradle/libs.versions.toml` |
| `sqlite-jdbc` — *"the two things that must survive a restart"* | `gradle/libs.versions.toml` |
| Bouncy Castle, for Argon2id | `gradle/libs.versions.toml` |
| `kotlin.jvm` and `kotlin.serialization` declared | `build.gradle.kts` |
| A 130-line design and a 1090-line plan | `docs/superpowers/` |

Every version in that catalogue was checked against Maven Central by hand on
2026-08-09 and 2026-08-11. The only thing missing was the module's source.
Choosing Python now would mean deleting all of that and re-deciding it worse.

## The decision

**Kotlin + Ktor 3.2.0 + SQLite.** The module is `:server` at `src/server/`, and
it serves the whole of `api-contract.md`, not endpoints 1 to 4.

The old `:otp-server` name goes with the old scope. It was named for sending a
text message, and it now decides who may open a locker.

### Why Kotlin, beyond what was already there

Ryan asked for the cabinet and the app to be **synced and seamless**. In one
language that is something the compiler enforces rather than something a person
remembers:

- `VGU1|<cabinet-id>|<unix-seconds>` is parsed by the app in `SessionCode.kt`
  and issued by the server. One format, one parser, one test.
- The refusal vocabulary is an enum in `Refusal.kt` and a `when` on the server.
  A refusal the app cannot show becomes a compile error, not a support ticket.
- A box number is the string `"04"` on both sides, because it is the same type
  on both sides. The leading zero has already been lost once by an `int`.

The cabinet screen is JavaScript and shares none of this. It is checked instead
— `scripts/checkqr.js` runs the shipped `qr.js` and asserts the format the
Kotlin test asserts.

### Why SQLite, not MySQL

One file. No service to install, no root password to leak — and a root password
in plain text is exactly what arrived from the other team.

It is also the only choice that satisfies a rule already written down: **the
lockout counter must survive a power cut.** A counter in memory resets when the
process dies, so anybody who can crash the server gets their guesses back. The
same goes for the QR sessions the server issues: if a restart forgets them,
every code in the air becomes forgeable.

Concurrency is a cabinet or two and a few hundred students. SQLite in WAL mode
is not the limit here and will not be.

## How the ESP32 fits

The cabinet hardware cannot be reached from outside. It takes a DHCP address on
campus Wi-Fi, it has no name, and nothing can dial it. So it **asks, and is
never told**:

```
   ESP32  ──  GET  /cabinet/commands   ──>  server     "anything for me?"
          <──  {"commands":[{"id":…,"box":"04","action":"open"}]}
   ESP32  ──  POST /cabinet/command-done ──>  server   "04 opened"
   ESP32  ──  POST /cabinet/door-closed  ──>  server   "04 is shut again"
```

Their sketch already pushes a door state to a LAN server. This keeps that shape
and adds the direction it was missing. Two consequences worth stating:

- **A door opens within one poll, not instantly.** One second is the budget. A
  student who has just scanned will accept one second; they will not accept
  five, and that number is a setting.
- **A command is taken exactly once.** The row is marked taken when it is handed
  over, so a retrying ESP32 cannot open a door twice. Rule 3 in
  `architecture.md` says an open never comes from a retry, and this is where
  that is enforced rather than hoped for.

## HTTPS, which is not optional and not easy

Both front-ends refuse plain HTTP with no flag to turn it off —
`Http.kt` line 87, `net.js` line 83. That was deliberate and it stays.

So the server serves TLS from the first commit. In development it is a
self-signed certificate, generated once into `config/dev-cert/` and **reused**
— regenerating per run would change the fingerprint every time and make trusting
it impossible. The folder is git-ignored.

Trusting it costs one step per device, once: install the `.crt` on the phone and
on the cabinet screen. Debug builds of the app carry a network security config
that accepts a user-installed CA; **release builds do not**, so this convenience
can never reach a student's phone.

## What this costs

**Nothing from their FastAPI app survives except the shape.** `LK-LIB-03`
naming, and the fact that the ESP32 talks to a server on the LAN. Both are
in ADR 0019 and both are kept.

**Kotlin is more code than FastAPI for the same endpoints.** Roughly twice, and
that is the honest price of the type safety bought above.

**One student maintains three build targets.** Mitigated only by them being one
toolchain: `./gradlew :app:assembleDebug` and `./gradlew :server:run` on the
same wrapper, no pip, no venv, no MySQL service to remember to start.

## Checked

Nothing yet. The first evidence will be the app completing a registration
against this server over HTTPS, and after that P5-10 — four refused attempts,
read from our own log.
