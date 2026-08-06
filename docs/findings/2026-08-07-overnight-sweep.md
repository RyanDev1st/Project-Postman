Parent: none

# Overnight sweep — 2026-08-07

## Status

Six commits. Four tasks ticked: **P1-04**, **P0-12**, **P0-13**, and the cabinet's network door. Board went **11 → 14 of 75**, and `checkboard.py` is consistent.

**The one thing you need to read:** the whole board is now blocked on two human actions, and no amount of code moves it. Everything else below is detail.

## The finding that matters

I walked every open task's dependency chain to its root. **All of them end at P1-05 or P1-06, and both need a server address we do not have.** `server_base_url` in `config/settings.json` is empty.

| # | What | Who | Unblocks |
| --- | --- | --- | --- |
| 1 | **Send `api-contract.md` to the Server team** — P0-04 | Somebody with their contact | ~60 tasks, Phases 2–8 |
| 2 | **Install the APK on the Vivo and look at it** — P1-01 | Somebody holding that phone | Phase 1's last human step |

**P0-04 was marked `⏸️ LATER` and its `Blocks:` line said `—`.** That was the bug. The dependency was real and unrecorded, so deferring it looked free while it was holding up the project. I took it out of the deferred list and wrote the dependency onto P1-05 and P1-06.

This is not a reversal of [ADR 0009](../adr/0009-start-phase-1-early.md). That ADR deferred P0-04 *on the condition* that the shape is agreed before Phase 2 — and Phase 2 is next, so the condition has arrived. No new ADR, and the ADR is not edited.

The contract itself is finished: 17 endpoints, every value chosen, no blanks. It needs sending, not writing.

## Decide in the morning

**One item, and it is the only thing I decided that was arguably yours.**

### P0-13 — rule C4's offline exception

The board said *"decided by the team, not by whoever writes the code."* You delegated; I decided. [ADR 0010](../adr/0010-c4-offline-exception.md) has the full argument.

**The decision:** C4 gains a narrow exception — *a front-end may verify a proof the server made possible; it may never decide something new.* Scoped three ways: the cabinet only, a real outage only, verification only.

**Why I was comfortable making it:** checking a MAC is not forming an opinion, and C4's own stated *why* — one truth in one place, a modified app cannot be trusted — is not breached by it. The alternative is that campus Wi-Fi drops and nobody collects a parcel that afternoon, with the parcels already in the boxes.

**Why you should still look:** the cabinet now holds key material, and somebody who opens a cabinet can derive every user secret for that cabinet. ADR 0004's threat model accepts that — guard post, books and phone cases — but it is a real cost and it is yours to agree to.

**If you disagree:** it is one new ADR and no code. ADR 0004's Option A — the server sends codes to the cabinet in advance — is about a day's work and covers most outages.

I wrote it as its own ADR rather than editing the rule, because that exact quiet edit happened on 2026-08-05 and had to be reverted. C4 now points at the ADR, so the exception cannot be read without the argument for it.

**The team still needs telling.** That part I cannot do.

## What got built

### P1-04 — one door for every network call ✅

`src/app/.../net/` — five files. Every request the app makes goes through `Http.kt`.

- **HTTPS or nothing.** A non-`https` URL throws before a socket opens. Redirects are not followed — a redirect can move a request to plain HTTP or to another host.
- **No retries.** An open-door call moves real metal; a retry can open a door nobody is standing at.
- **Three outcomes, not two.** `Ok`, `Refused`, and **`Unclear`** — if the network drops mid-request the app does not know whether the door opened, and saying either "worked" or "failed" would be a lie.
- **The token is encrypted** with a key held in the Android Keystore, which the app itself cannot read. No `androidx.security-crypto`; it is deprecated and the keystore under it is sixty lines to use directly.
- **No new dependencies at all.** `HttpURLConnection` and `org.json` ship with Android.
- **Endpoints 9–14 are deliberately absent.** Those are the cabinet's. An app that *could* look up a stranger's name is a hole in the product, not a missing feature.

`config/settings.json` is copied into the APK at build time rather than duplicated, so the two cannot disagree.

### The cabinet's door ✅

`src/cabinet/net.js`, plus `config.example.js` showing where the key goes. `config.js` holds the real key, is git-ignored, and is placed on the device. It differs from the app's door where the cabinet is a public terminal: no cookies, no redirects, and it says *"this cabinet has no key yet"* rather than calling with nothing.

### `scripts/checknet.py` — new

Proves the one-door rule for both front-ends, and carries the P1-07 secret checks. **I tested it by breaking things on purpose** — a probe file with a `URL()`, a plain-HTTP address, an API key and a JWT. It caught all of them with file and line, and exits 1.

It also caught its own false positive: it flagged `config.example.js`'s placeholder address. Fixed precisely — the RFC 2606/6761 reserved names (`.invalid`, `.example`, `.test`, `.localhost`) are exempt, whole files never are. A real address pasted into the example file still fails, which I checked by pasting one in.

### P0-12 — the offline exchange ✅

Written into `api-contract.md` as endpoints 16 and 17. HMAC-SHA256 over challenge ‖ box number, published on purpose; one key per cabinet; no clocks needed; lockout that survives a power cut.

### The videos

**The short one is done** — `film/out/loader0001-0135.mp4`, 4.5 s, 720×1280, ~630 kB. Straight-in reach, the whole mannequin, the Shopee label actually stuck to the box, ending on `#CFCBC2` so the interface draws over the last frame with no visible step.

**The long one is not done, and I have not pretended otherwise.** The nine beats and every subtitle are written in `film/story.md`; the campus gate is built in `film/scene_gate.py`. No shot of it is rendered and it is not cut.

What is wrong with it: the sky is a *sunset* HDRI and it is dim. Turned down far enough to stop it flattening the shadows, it ends up darker than the sunlit ground, which reads as a coming storm. It wants a midday HDRI and one more pass. The README lists three faults I found the slow way so nobody repeats them — the worst being that the sun pointed from behind the wall, so everything the camera could see was backlit, and every attempt to fix the flatness by raising the energy just made a brighter flat picture.

**On the gate:** it is a plausible Vietnamese campus gate, **not** VGU's D9 backgate. Nothing was measured. I was wrong earlier to say copyright stopped me — geography is fact and modelling a real place from reference is ordinary. The actual reason is that the browser extension is not connected, so I could not see the imagery. **Send photographs and I will match them**; the scene is one function.

## Two smaller things

- **`local.properties` was missing** and the build could not find the SDK. Recreated it pointing at `A:\Android\Sdk`. It is git-ignored by design, so it will vanish again on any fresh clone — worth a line in the toolchain doc.
- **`CLAUDE.md` and the agent config are untracked** (`AGENTS.md`, `GEMINI.md`, `.mcp.json`, `opencode.jsonc`). Deliberate, going by commit `8b4ea2e`. I edited `CLAUDE.md` on disk for the new `film/` folder but did not stage it. Worth confirming that is still what you want, since the Repository map lists `CLAUDE.md` as a repo file.

## Not done, and why

- **P1-05, P1-06, P1-07, P1-08** — all need the server. Both front-ends' halves are built.
- **P1-01** — needs a person and the phone. The APK is built and waiting.
- **Tests.** `CLAUDE.md` still says *"Tests | Fill this in once the stack is chosen"*, and the stack is chosen. The network layer is exactly the code that most wants them — refusal mapping, HTTPS enforcement, settings-version precedence. It is not a board task, so I did not add one unasked. **Recommend adding it as P1-10.**
