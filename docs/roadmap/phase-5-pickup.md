# Phase 5 — Pick up

**Goal:** the receiver walks to the cabinet and the right door opens — by scanning, or by typing a code.

**Progress: 0 / 9.**

**This is the product.** Everything in Phases 0 to 4 exists to make this phase possible.

Two ways in. **Scanning is the main path** — P5-01 to P5-07. **Typing a code is the backup** — P5-08 and P5-09, for a flat battery or a phone left at home. They are not equally safe, and the backup carries rules the main path does not need.

## Tasks

- [ ] **🟡 DOING — P5-01** — The cabinet screen shows the QR session code
      - Owner: Claude · Needs: P0-07, P1-06 · Blocks: P5-02
      - Verify: the real cabinet shows a QR, and it changes on the agreed timer
      - Notes: the QR says *which cabinet, at what moment*. It is not a key. See `architecture.md` section 6.
        **Built and checked by machine; not ticked.** The screen draws
        `VGU1|<cabinet-id>|<unix-seconds>`, redraws every 30 s and counts down
        from 60, per P0-07. `node scripts/checkqr.js` runs the shipped `qr.js`
        and proves the payload, the timings, and that a cabinet with no id
        draws nothing rather than sending a phone nowhere.
        `python scripts/checkqr.py` draws the code and reads it back with
        `zxing-cpp`, an unrelated decoder — 3 payloads, all exact.
        What is missing is the Verify itself: **there is no real cabinet**, and
        this task also needs P1-06, which is 🔴 BLOCKED on a server address.
        Nothing here waits on the server — the payload is a place and a clock
        reading, and the screen knows both — so this stays DOING, not BLOCKED.

- [ ] **🟡 DOING — P5-02** — The app scans the QR on the cabinet screen
      - Owner: Claude · Needs: P5-01, P2-04, P4-04 · Blocks: P5-03
      - Verify: a real phone reads the real cabinet screen in daylight **and** in a dark corridor
      - Notes: **Built and checked by machine; not ticked.** The scan screen used
        to be a drawing of a scanner — a dark box, four corners, a sweep, and no
        camera behind any of it. It has a camera now (CameraX), a reader
        (ML Kit), and rules about what it is willing to believe
        (`pickup/SessionCode.kt`). One code is acted on and the rest of the
        thirty a second are dropped, so a scan can only ever do one thing once.
        The scanner button in the Home bar was wired to nothing and now opens
        the screen; that is why it looked finished and was not.
        Checked: `:app:testDebugUnitTest` — 11 rules about the string, including
        that the cabinet's own `qr.js` still writes the format this reads.
        `:app:connectedDebugAndroidTest -Pemulator` — 4 tests, ML Kit reading
        the cabinet encoder's real QR on a device with **no Google Play
        Services installed at all**, which is the open question in
        [ADR 0018](../adr/0018-ml-kit-and-play-services.md).
        Seen on the emulator: the viewfinder shows live camera, the corners and
        sweep sit over it. Not seen: a read through the lens — the emulator's
        virtual room points its camera at a television, not at the wall the QR
        was hung on.
        What is missing is the Verify itself, and no machine here can stand in
        for it: **a real phone, a real cabinet screen, daylight and a dark
        corridor.** Until P5-03 there is no server, so a good code moves the
        screen to "Opened" and opens nothing. That is the demo saying what it
        read, not a door reporting that it moved.

- [ ] **P5-03** — The server checks the user, then opens their box
      - Owner: _unassigned_ · Needs: P5-02 · Blocks: P5-04, P5-05, P5-06, P5-07, P5-08, P6-05, P7-03
      - Verify: the correct door physically opens, and the server log shows which user and which box

- [ ] **P5-04** — The door closes and the parcel is marked collected
      - Owner: _unassigned_ · Needs: P5-03, P0-10 · Blocks: P6-01, P6-02, P6-03, P7-01, P7-02
      - Verify: take the parcel out, close the door, and the parcel leaves "My parcels" and is written as collected in the server log
      - Notes: same as P3-05, in reverse. No sensor — [ADR 0006](../adr/0006-no-sensor.md)

- [ ] **P5-05** — Plain messages for "code expired" and "no parcel for you here"
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: both cases show their own plain sentence on a real phone
      - Notes: an expired code is normal, not a failure. The message says "scan again", with no alarm

- [ ] **P5-06** — An unclear result never shows success
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: cut the network mid-request on a real phone. The app says it could not confirm and tells the user to check the box. It never shows "opened"

- [ ] **P5-07** — One scan opens at most one door
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: scan once on a real phone and count the open requests in the server log. There is exactly one
      - Notes: proven from the log, not from the screen

- [ ] **P5-08** — Type the code on the cabinet instead of scanning
      - Owner: _unassigned_ · Needs: P5-03, P0-11 · Blocks: P5-09, P6-06
      - Verify: with the phone switched off, the code from the message is typed on the real cabinet and the right box opens
      - Notes: the whole point is that it works with no phone. Test it with the phone genuinely off, not just backgrounded

- [ ] **P5-09** — The typed code works once, expires, and locks out after wrong tries
      - Owner: _unassigned_ · Needs: P5-08 · Blocks: —
      - Verify: three checks, all on the real cabinet — (1) a used code refuses, (2) an old code refuses, (3) repeated wrong codes lock that box, and the screen says when it unlocks
      - Notes: a wrong code must never say whether that code exists. "Not right" and "not right yet" read the same

## Safety rules for this phase

Tick these with the phase. A door opening is a physical act.

- [ ] A door opens only from a scan the user made. Never from a retry, a timer, or a screen refresh
- [ ] One scan = at most one open request. Proven from the server log
- [ ] An unclear result is said plainly. The app never shows a false success
- [ ] A photograph of the cabinet screen opens nothing without the receiver's logged-in account
- [ ] The typed code is the one thing that **is** a key. It works once, it expires, and guessing locks the box

## Exit check

- [ ] All nine tasks ticked
- [ ] All five safety rules ticked
- [ ] Someone who is not on this team collected a real parcel, with no help
- [ ] It worked in daylight and in a dark corridor
- [ ] Counts updated in [README.md](README.md)
