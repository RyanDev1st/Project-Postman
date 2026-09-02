# Phase 3 — Shipper drop

**Goal:** a shipper walks up to the cabinet, finds the receiver, and a box opens.

**Progress: 0 / 13.**

This whole phase is the **cabinet screen**, not the phone app. The shipper installs nothing.

**Remember what this screen is.** Anyone can walk up to it. It has no login. Every screen on it is readable by a stranger.

**This phase splits in two, by [ADR 0009](../adr/0009-start-phase-1-early.md).** The screen half — P3-01, P3-02, P3-03, P3-06, P3-07 — is a web page and can be built and ticked in a browser now. The metal half — **P3-04 and P3-05, where a door actually opens and closes** — still needs a real cabinet, and stays unticked until P0-16 delivers one. The simulator can drive them but cannot tick them, per [ADR 0008](../adr/0008-cabinet-simulator.md).

## Tasks

- [ ] **P3-01** — 🟡 DOING — Cabinet home screen with one button: "Deliver a parcel"
      - Owner: Claude · Needs: P1-06 · Blocks: P3-02, P3-07, P3-12
      - Verify: the home screen runs full-screen in a browser and survives being left on all day without the layout breaking
      - Notes: P0-04 and P0-05 dropped from the Needs by [ADR 0009](../adr/0009-start-phase-1-early.md). The Verify said *the real cabinet*; there is none, and P0-16 is deferred. Left on all day is still the real test — a screen that leaks memory overnight fails the same way in a browser as on a cabinet
      - Notes: 2026-08-17 — built and seen at 1024x600, the size in [ADR 0021](../adr/0021-build-for-the-proposed-hardware.md). **The all-day half has not been run**, so it is not ticked

- [ ] **P3-02** — 🟡 DOING — Type a receiver's phone number on the cabinet keypad
      - Owner: Claude · Needs: P3-01 · Blocks: P3-03
      - Verify: a number typed on the real cabinet reaches the server, and the log shows it
      - Notes: big keys. A shipper is holding a parcel with one hand
      - Notes: 2026-08-17 — the keypad works and the number reached the server, because the server answered with a name only it could know. **The server log line itself was not kept**, so the Verify is half-run

- [ ] **P3-03** — 🟡 DOING — Show the masked name, and the shipper confirms it
      - Owner: Claude · Needs: P3-02 · Blocks: P3-04, P3-06, P3-08
      - Verify: a real registered number shows a masked name such as `Nguyễn V. A***`, and no full name or phone number appears anywhere on screen
      - Notes: P0-08 dropped from the Needs by [ADR 0009](../adr/0009-start-phase-1-early.md) — the exact mask is deferred, so we build the shape we proposed. **The masking itself is not deferred.** The second half of the Verify stands whatever format is agreed later
      - Notes: 2026-08-17 — the screen showed `Nguyen V. M***` and nothing else; the typed number lives on the step before and is wiped on the way out. **The number used was a seeded demo receiver, not a real registered person**, which is the half of the Verify still owed

- [ ] **P3-04** — The server picks a free box and opens it
      - Owner: _unassigned_ · Needs: P3-03 · Blocks: P3-05, P3-06
      - Verify: the server log shows which box was chosen, and that door physically opens
      - Notes: the cabinet does not choose the box. It asks. Rule 2 in `architecture.md`

- [ ] **P3-05** — The door closes and the parcel is recorded
      - Owner: _unassigned_ · Needs: P3-04, P0-10 · Blocks: P4-01, P6-04, P6-07
      - Verify: closing the real door writes one parcel row in the server log, with the box number and the time
      - Notes: there is no sensor — [ADR 0006](../adr/0006-no-sensor.md). The door closing is the evidence, and the risk of a driver leaving with the parcel is accepted and written down there

- [ ] **P3-06** — Plain messages for "number not registered" and "no free box"
      - Owner: _unassigned_ · Needs: P3-03, P3-04 · Blocks: —
      - Verify: both cases show their own plain sentence on the real cabinet screen
      - Notes: 2026-08-17 — both sentences are written in `drop.js`, and so is a third for an answer the screen cannot read. **Neither case has been made to happen**, so nothing here is checked
      - Grew 2026-09-02 by [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md): "number not registered" is no longer one sentence but a ladder - P3-09 offers a near miss, P3-10 lists who is expecting a parcel, P3-11 sends the shipper to ABO. This task keeps the "no free box" half

- [ ] **P3-07** — The cabinet says plainly when it is working offline
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: —
      - Verify: with the network unplugged, the screen says so, and warns that the name cannot be checked before the driver commits
      - Notes: offline the drop still works — see P6-07 — but the name check is skipped, so the driver must be told to read the number twice

- [ ] **P3-08** — A typed number finds its booking, and the box it is holding
      - Owner: _unassigned_ · Needs: P2-10, P3-03 · Blocks: P3-09, P3-13
      - Verify: a number with a live booking opens **that booking's box and no other**, and the screen shows the masked name. A number whose booking expired an hour ago does not open anything, and falls to P3-09
      - Notes: [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md). A number with no booking but a registered account still drops into any free box, which is what the cabinet does today - the booking is a better answer, not the only one

- [ ] **P3-09** — “Did you mean…” when the number is within two digits
      - Owner: _unassigned_ · Needs: P3-08 · Blocks: P3-10, P4-06
      - Verify: a number within two digits of exactly one live booking offers that booking's masked name. A number within two digits of **two** bookings offers **neither**, and asks for the name instead. A transposition - `...328` typed as `...382` - counts as one
      - Notes: the likeliest cause of a miss is the person who placed the order typing their **own** number wrong, and they are not standing there to be asked. Damerau-Levenshtein over the nine national digits. **Two was measured, not guessed**: it finds a one-digit typo 100% of the time and a two-digit typo 100% of the time, and wrongly offers a student to somebody else's parcel 0.02% of the time, where one digit finds the two-digit typo only 15% of the time. **More than one candidate means no candidate** - that rule, not the tolerance, is the safety net, because students buy SIMs in batches and adjacent numbers are real. Rate-limited, or it becomes a way to sweep the campus a digit at a time

- [ ] **P3-10** — The shipper types the name off the label
      - Owner: _unassigned_ · Needs: P3-09 · Blocks: P3-11, P4-06
      - Verify: typing `Nguyen Van Phong` finds Nguyễn Văn Phong and **not** Nguyễn Văn Phúc, with accents and case taken off. A name matching two live bookings resolves to neither. **No list is ever returned**, and no phone number appears in the response body, not only on the screen
      - Notes: this was a masked **list** until 2026-09-02, and the list was measured and dropped. `Nguyễn Văn Phong` and `Nguyễn Văn Phúc` both mask to `Nguyễn V. P***`, and two masks collide in **56%** of full cabinets - so it failed at its one job in most of them, and a shipper picking between two identical rows is a coin flip that ends with a parcel in a stranger's reserved box. Typing inverts it: the server holds the full names, answers yes or no, and shows a list to nobody. It still solves the case it was built for, because a receiver who mistyped their **number** has the right **name** on the label

- [ ] **P3-11** — Send it to ABO when nobody matches
      - Owner: _unassigned_ · Needs: P3-10 · Blocks: —
      - Verify: a number matching nobody shows a plain sentence telling the shipper to contact the recipient and leave the parcel at **ABO**, the grocery store facing the campus back gate. No door opens
      - Notes: also the answer when every box is taken, which a cabinet full of live bookings can cause

- [ ] **P3-12** — The cabinet screen speaks Vietnamese
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: —
      - Verify: with no browser locale set the cabinet opens in **Vietnamese**, every string on every screen is Vietnamese, and one toggle switches the whole screen to English and back without a reload
      - Notes: `index.html`, `drop.js` and `screen.js` are hardcoded English behind `<html lang="en">`, with no extraction layer at all. This is a strings pass, not a default flip, and it belongs beside P3-08 to P3-11 because it edits the same three files
- [ ] **P3-13** — The shipper confirms the box number too
      - Owner: _unassigned_ · Needs: P3-08 · Blocks: —
      - Verify: after a box is chosen the screen shows a panel naming it - *“Box 07. Put the parcel in box 07 and close it.”* - and no door moves until it is accepted. **Not right** opens nothing and releases the box
      - Notes: two confirmations, and they are not the same question: the first is *have I got the right person*, the second is *am I about to walk to the right door*. Twenty doors in four rows are easy to misread at arm's length with a parcel under one arm, and a parcel in the wrong open box is collected by the wrong student. Same shape as the receiver's confirm panel at P2-13, for the same reason
## Safety rules for this phase

Tick these with the phase. They are not style preferences.

- [ ] A door opens only after the shipper confirms. Never on the number alone
- [ ] One confirm = at most one door opening. Proven from the server log, not assumed
- [ ] No full name, no phone number, and no parcel list ever appears on the cabinet screen - including the P3-10 list, which is masked names and nothing else
- [ ] While online, the cabinet never picks a box by itself, even when the server is slow

## Exit check

- [ ] All thirteen tasks ticked
- [ ] All four safety rules ticked
- [ ] A real parcel was dropped into a real box by someone who is not on this team
- [ ] Counts updated in [README.md](README.md)
