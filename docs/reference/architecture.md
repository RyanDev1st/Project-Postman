# Architecture — the parts and how they connect

Plain description of the whole system. Edit this file when reality changes.

Product: a **parcel drop-off locker**. A shipper leaves a parcel in a box. The receiver picks it up with a phone. See [ADR 0003](../adr/0003-parcel-locker-product.md).

## System overview

A **swimlane block diagram**. Columns are who owns what. Blocks are stages. Arrows cross the boundaries between owners.

Check this chart against what the team expects. If one arrow is wrong, the plan under it is wrong.

```
╔════════════════════════════╦═══════════════════════════════╦════════════════════════════╗
║  RECEIVER                  ║  CABINET — on campus, online  ║  SERVER TEAM               ║
║  student, own phone        ║  screen · boxes · on Wi-Fi    ║  API + database, not us    ║
╠════════════════════════════╬═══════════════════════════════╬════════════════════════════╣
║                            ║                               ║                            ║
║   ┌──────────────────┐     ║   ┌──────────────────────┐    ║   ┌────────────────────┐   ║
║   │    PHONE APP     │     ║   │   CABINET SCREEN     │    ║   │     API SERVER     │   ║
║   │   « this repo »  │     ║   │    « this repo »     │    ║   │  the only door     │   ║
║   └──────────────────┘     ║   └──────────────────────┘    ║   └─────────┬──────────┘   ║
║                            ║   ┌──────────────────────┐    ║             │              ║
║                            ║   │   BOXES  +  LOCKS    │    ║   ┌─────────▼──────────┐   ║
║                            ║   │  « hardware team »   │    ║   │      DATABASE      │   ║
║                            ║   └──────────────────────┘    ║   └────────────────────┘   ║
║                            ║                               ║                            ║
╟────────────────────────────╫───────────────────────────────╫────────────────────────────╢
║                            ║                               ║                            ║
║  BLOCK A — REGISTER        ║                               ║                            ║
║  P2-01 … P2-07             ║                               ║                            ║
║                            ║                               ║                            ║
║   phone number ────────────╫───────────────────────────────╫──▶ send one-time code      ║
║   one-time code ◀──────────╫───────────────────────────────╫─── by SMS                  ║
║   code back ───────────────╫───────────────────────────────╫──▶ token, stored in the    ║
║                            ║                               ║    phone's secure store    ║
╟────────────────────────────╫───────────────────────────────╫────────────────────────────╢
║                            ║                               ║                            ║
║                            ║  BLOCK B — DROP               ║                            ║
║                            ║  P3-01 … P3-07                ║                            ║
║                            ║                               ║                            ║
║                            ║  shipper types phone no. ─────╫──▶ find the receiver       ║
║                            ║  masked name ◀────────────────╫─── « Nguyễn V. A*** »      ║
║                            ║  shipper confirms ────────────╫──▶ pick a free box         ║
║                            ║  door opens ◀─────────────────╫─── open box 04             ║
║                            ║        │                      ║                            ║
║                            ║        ▼                      ║                            ║
║                            ║  parcel goes in               ║                            ║
║                            ║  door shuts ──────────────────╫──▶ parcel recorded         ║
║                            ║                               ║    « no sensor - ADR 6 »   ║
╟────────────────────────────╫───────────────────────────────╫────────────────────────────╢
║                            ║                               ║                            ║
║  BLOCK C — TELL            ║                               ║                            ║
║  P4-01 … P4-05             ║                               ║                            ║
║                            ║                               ║                            ║
║   « parcel in box 04 » ◀───╫───────────────────────────────╫─── push notice             ║
║   My parcels screen        ║                               ║                            ║
╟────────────────────────────╫───────────────────────────────╫────────────────────────────╢
║                            ║                               ║                            ║
║  BLOCK D — PICK UP         ║  the two paths meet here      ║                            ║
║  P5-01 … P5-09             ║                               ║                            ║
║                            ║                               ║                            ║
║   ── D1 · SCAN THE QR ──   ║   shows a QR that changes ◀───╫─── session code            ║
║   phone reads the screen ◀─╫───────────────────────────────╫    « which cabinet, when » ║
║   token + session ─────────╫───────────────────────────────╫──▶ is this user owed a     ║
║                            ║                               ║    box here?               ║
║                            ║                               ║                            ║
║   ── D2 · TYPE THE CODE ── ║   receiver types it ──────────╫──▶ is this code good,      ║
║   « code from the notice » ║   « no app needed »           ║    once, and not expired?  ║
║                            ║                               ║                            ║
║                            ║   door opens ◀────────────────╫─── open box 04             ║
║                            ║   door shuts ─────────────────╫──▶ marked collected        ║
║                            ║                               ║                            ║
╚════════════════════════════╩═══════════════════════════════╩════════════════════════════╝

   « this repo »  = the IT team builds it        ──▶  a request
   « hardware »   = the hardware team builds it  ◀──  an answer
   Free for students. No payment anywhere in this chart.
```

### The one thing to look at twice

**D1 and D2 are not the same risk.**

| | What the receiver shows | If somebody copies it |
| --- | --- | --- |
| **D1 — scan the QR** | Nothing. The cabinet shows the code, the phone reads it | **Nothing happens.** The QR only says *which cabinet, when*. Identity comes from the app login |
| **D2 — type the code** | A code from the notice | **The box opens.** The code *is* the key |

So D2 needs protection D1 does not: the code works **once**, it **expires**, and too many wrong tries locks the box for a while.

D2 exists so a flat battery or a broken app does not trap a parcel. That is worth having. It is not the main path.

```
   ┌──────────────┐                                 ┌──────────────┐
   │  PHONE APP   │                                 │ CABINET SCREEN│
   │  (receiver)  │                                 │  (shipper)    │
   │              │                                 │               │
   │  register    │                                 │  type phone   │
   │  get notice  │                                 │  pick name    │
   │  scan QR     │                                 │  show QR      │
   └──────┬───────┘                                 └───────┬───────┘
          │                                                 │
          │  HTTPS                                   HTTPS  │
          └──────────────────┐         ┌────────────────────┘
                             ▼         ▼
                        ┌──────────────────┐        ┌────────────┐
                        │    API SERVER    │ ─────> │  DATABASE  │
                        │  (Server team)   │        │  users     │
                        └────────┬─────────┘        │  boxes     │
                                 │                  │  parcels   │
                    ┌────────────┴────────────┐     │  history   │
                    ▼                         ▼     └────────────┘
            ┌───────────────┐        ┌────────────────┐
            │ Push notifier │        │ Cabinet hardware│
            │ (to the app)  │        │ (opens a door)  │
            └───────────────┘        └────────────────┘
```

## 1. Phone app — this repo

Runs on Android and iOS. Used by the **receiver**, never by the shipper.

It registers a phone number, receives the notification, lists waiting parcels, and scans the QR on the cabinet screen.

**The app decides nothing.** It does not decide who you are, which box is yours, or whether a door may open. It asks. One truth in one place — and a modified app cannot open a box it should not.

## 2. Cabinet screen — this repo

Runs on the screen fixed to the cabinet. Used by the **shipper**, and by nobody who is logged in.

It has no login. Anyone can walk up to it. Everything it can do is therefore limited on purpose:

- look up a receiver by phone number, and see a **masked** name only
- start a drop, which makes the server open one box
- report that a door closed
- display the QR that a receiver's phone scans

It cannot open a box of its own accord, list parcels, or show a full name or a phone number.

**The cabinet has its own identity.** It signs in as a device, not as a person, with a key placed on it during setup. That key never sits in this repo. See task P0-05.

## 3. API server — Server team

The only door between either front-end and the data. What each front-end may send is fixed in [api-contract.md](api-contract.md).

The server does all the work neither front-end may do:

- check the one-time code sent to a phone number
- find the receiver behind a phone number, and mask the name
- pick a free box of the right size
- command the cabinet hardware to open a door
- issue and expire the cabinet's QR session code
- decide whether a scanning user has a parcel at that cabinet
- send the notification

## 4. Database — Server team

Remembers users, cabinets, boxes, parcels, and the full open and close history.

## 5. Login and token

The receiver types a phone number **once** and enters the one-time code sent to it. The server answers with a token — a temporary pass. The app stores that token in the phone's secure store and sends it with every later request.

A token expires. When it does, the server refuses, and the app sends the user back to register. Once, with a message. Never in a loop.

## 6. The QR session code — how pickup works

This is the part that is easy to get wrong, so it is written out.

The QR on the cabinet screen is **not a key**. It carries only two facts: *which cabinet*, and *at what moment*. It refreshes on a timer.

```
   RECEIVER          SERVER            CABINET SCREEN
      │                 │                    │
      │                 │  session code ────>│  shows QR
      │                 │                    │
      │  scans QR ──────┼────────────────────│
      │                 │                    │
      │  sends {session code + my token}     │
      │────────────────>│                    │
      │                 │  "this user has    │
      │                 │   a parcel in      │
      │                 │   box 04 here"     │
      │                 │                    │
      │                 │  open box 04 ─────>│  *clunk*
      │   "box 04"      │                    │
      │<────────────────│                    │
```

Identity comes from the **token**, not from the QR. So a photograph of the cabinet screen is worth nothing on its own.

## 7. Extras

| Part | Where it runs | What it does |
| --- | --- | --- |
| QR reader | In the phone app | Reads the code shown on the cabinet screen |
| QR display | On the cabinet screen | Shows the current session code |
| Push notifier | On the server | Tells the receiver a parcel arrived |
| Cabinet hardware | At the cabinet | Opens one metal door when the server says so |

## The rules that come out of this shape

1. **One door.** Every network call in a front-end goes through one file. See task P1-04.
2. **The server is the truth.** If a front-end and the server disagree, the server wins. Ask again, do not guess.
3. **An open is physical.** Never send an open request from a retry, a timer, or a screen refresh. Only from a direct tap or a scan the user made.
4. **Unclear is not success.** If the network drops mid-request, the front-end does not know whether the door opened. It must say so.
5. **No secrets in a front-end.** Anything shipped inside the app or the cabinet build can be read by anyone who has it.
6. **The cabinet screen is a public terminal.** Treat every screen on it as readable by a stranger. Never show a full name, a phone number, or a parcel list.

## Settled by the team on 2026-08-05

Said by the team, in writing. These are decisions, not guesses.

- **There is no sensor.** The door closing is the evidence. See [ADR 0006](../adr/0006-no-sensor.md), which records what that costs us and why we accept it.
- **The cabinet is on Wi-Fi.** Confirmed by the team.
- **Pickup has two paths:** scan the QR, or type a code. See D1 and D2 above.
- **Free for students.** No payment, no wallet, no fee screen, anywhere.

## 8. When the network drops

The cabinet is on Wi-Fi, and campus Wi-Fi drops. Both a drop and a pickup keep working. See [ADR 0004](../adr/0004-offline-pickup.md), which recommends how.

The short version: the cabinet asks a question the phone can only answer if it holds the right secret.

```
   ①  she types her phone number on the cabinet
   ②  cabinet shows a QR — a one-off random number, nothing else
   ③  her phone reads it, mixes it with her stored secret → 6 digits
   ④  she types the 6 digits
   ⑤  cabinet works out the same 6 digits and compares → the door opens
```

Three things make this work with no network on either side:

- **Her secret is already on her phone**, put there when she registered. Her phone needs no signal either.
- **The cabinet can work out anybody's secret** from the phone number typed at the keypad, using the key it was given at setup. It stores no user list.
- **No clocks have to agree.** The cabinet supplies a fresh random number each time, so there is nothing to keep in step. This is why it is not the same as Google Authenticator, which does need clock sync.

Offline, a drop skips the name check — the cabinet cannot look anything up. It takes the number, opens a box, and the server settles up when the network returns. The screen says so, so the driver knows to read the number twice.

**What protects it:** the answer works once, the random number is thrown away after use, and wrong tries lock that box — with the count surviving a power cut. Six digits is only safe while guessing is limited.

## Settings — the numbers we guessed

Every value in this design that we chose rather than measured lives in **one settings file**, editable by a person, changeable without a new release. See [ADR 0005](../adr/0005-we-propose-they-object.md).

| Setting | Our default | Change it when |
| --- | --- | --- |
| How long a QR session code lives | 60 seconds | Users say it expires before they can scan |
| How often the cabinet screen redraws the QR | every 30 seconds | The screen flickers, or codes go stale too fast |
| How long a receiver token lives | 30 days | People are asked to register again too often |
| How long a typed pickup code lives | 48 hours | Parcels sit longer than that before collection |
| How many digits in a typed pickup code | 6 | Never, without reading [ADR 0004](../adr/0004-offline-pickup.md) first |
| Wrong tries before a box locks | 5 | Real users get locked out by accident |
| How long a box stays locked | 15 minutes | — |
| Days before an uncollected parcel is chased | 3 | Boxes fill up, or nobody complains |
| Server address | — | Moving between test and real |
| Notification route | push | SMS or Zalo turns out to be needed |

**A number is a setting. A shape is not.** Changing `60` to `120` is editing this table. Changing *"the code works once"* to *"twice"* is a design decision and needs an ADR.

The file is [`config/settings.json`](../../config/settings.json). Editing rules are in [config/README.md](../../config/README.md).

Changing a number here does **not** yet reach a phone without a new release — that is task **P1-08**. Until it is ticked, these values are frozen at whatever shipped.

## Open questions

Move a line out of here when it is answered, and write the answer in the right doc.

- How does the receiver get the notice — push through the app, Zalo, or SMS? The code in block D2 travels the same way. (blocks P0-06)
- How long does a QR session code live, and how often does the screen refresh it? (blocks P0-07)
- How long does the **typed code** in D2 live, and how many wrong tries before the box locks? Different question from the one above, different answer. (blocks P0-11)
- Does the cabinet have a camera after all? Not needed for this design. It would only matter if we ever reversed [ADR 0003](../adr/0003-parcel-locker-product.md). (no task)
