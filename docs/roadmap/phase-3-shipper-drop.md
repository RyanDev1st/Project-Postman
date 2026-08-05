# Phase 3 — Shipper drop

**Goal:** a shipper walks up to the cabinet, finds the receiver, and a box opens.

**Progress: 0 / 7.**

This whole phase is the **cabinet screen**, not the phone app. The shipper installs nothing.

**Remember what this screen is.** Anyone can walk up to it. It has no login. Every screen on it is readable by a stranger.

## Tasks

- [ ] **P3-01** — Cabinet home screen with one button: "Deliver a parcel"
      - Owner: _unassigned_ · Needs: P0-04, P0-05, P1-06 · Blocks: P3-02, P3-07
      - Verify: the real cabinet shows the home screen, and it survives being left on all day

- [ ] **P3-02** — Type a receiver's phone number on the cabinet keypad
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: P3-03
      - Verify: a number typed on the real cabinet reaches the server, and the log shows it
      - Notes: big keys. A shipper is holding a parcel with one hand

- [ ] **P3-03** — Show the masked name, and the shipper confirms it
      - Owner: _unassigned_ · Needs: P3-02, P0-08 · Blocks: P3-04, P3-06
      - Verify: a real registered number shows a masked name such as `Nguyễn V. A***`, and no full name or phone number appears anywhere on screen

- [ ] **P3-04** — The server picks a free box and opens it
      - Owner: _unassigned_ · Needs: P3-03 · Blocks: P3-05, P3-06
      - Verify: the server log shows which box was chosen, and that door physically opens
      - Notes: the cabinet does not choose the box. It asks. Rule 2 in `architecture.md`

- [ ] **P3-05** — The shipper closes the door and the parcel is recorded
      - Owner: _unassigned_ · Needs: P3-04 · Blocks: P4-01, P6-04
      - Verify: closing the real door writes one parcel row in the server log, with the box number and the time

- [ ] **P3-06** — Plain messages for "number not registered" and "no free box"
      - Owner: _unassigned_ · Needs: P3-03, P3-04 · Blocks: —
      - Verify: both cases show their own plain sentence on the real cabinet screen

- [ ] **P3-07** — What the cabinet does when it cannot reach the server
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: —
      - Verify: with the network unplugged, the cabinet says it is offline and refuses to start a drop. No door opens
      - Notes: an offline cabinet must never guess. It cannot know whose parcel this is

## Safety rules for this phase

Tick these with the phase. They are not style preferences.

- [ ] A door opens only after the shipper confirms the name. Never on the number alone
- [ ] One confirm = at most one door opening. Proven from the server log, not assumed
- [ ] No full name, no phone number, and no parcel list ever appears on the cabinet screen
- [ ] The cabinet never picks a box by itself, even when the server is slow

## Exit check

- [ ] All seven tasks ticked
- [ ] All four safety rules ticked
- [ ] A real parcel was dropped into a real box by someone who is not on this team
- [ ] Counts updated in [README.md](README.md)
