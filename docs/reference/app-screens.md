# The phone app — every screen

Parent: none

What the receiver's app is made of. Every screen, what leads where, and what each one must never show.

**Wireframes and flow only.** No colour, no type scale, no icons, no motion. The API shape is not agreed yet, and styling a screen the contract may still reshape is work done twice. Task **P1-09**.

Product is [ADR 0003](../adr/0003-parcel-locker-product.md). The cabinet screen is a different front-end and a different document — it shares nothing with this but the API.

## What the app is for

**This is not a browsing app.** The receiver opens it about twice per parcel: once when the notice arrives, once standing at the cabinet. Most sessions are under twenty seconds, one-handed, sometimes walking.

Everything below follows from that. The current parcel is a screen, not a row in a list. The button that matters is the biggest thing on it.

## The screens

| # | Screen | Reached from | Leads to |
| --- | --- | --- | --- |
| 1 | **Phone number** | first open, or a dead token | 2 |
| 2 | **One-time code** | 1 | 3 |
| 3 | **Waiting** — the home screen | 2, the notice, or reopening the app | 4, 6, 7 |
| 4 | **Scan** | 3 | 5, or back to 3 |
| 5 | **Opened** | 4, or 6 | 3 |
| 6 | **Type the code** | 3, or 4 when scanning fails | 5, or back to 3 |
| 7 | **History** | 3 | — |

Seven screens. Two are register, one is home, three are the pickup, one is history.

```
   1 Phone number ──> 2 One-time code ──┐
                                        v
   notice tapped ──────────────────> 3 Waiting ──> 7 History
                                     │      │
                              scan ──┘      └── can't scan
                                     │             │
                                     v             v
                                4 Scan ──────> 6 Type the code
                                     │             │
                                     └──> 5 Opened <┘
                                            │
                                            v
                                        3 Waiting
```

## 1 — Phone number

```
+----------------------------+
|                            |
|   VGU Smart Locker         |
|   Tu gui hang thong minh   |
|                            |
|   Your phone number        |
|  +----------------------+  |
|  | 09xx xxx xxx         |  |
|  +----------------------+  |
|                            |
|  +----------------------+  |
|  |     SEND ME A CODE   |  |
|  +----------------------+  |
|                            |
|  We text you a code to     |
|  check it is your number.  |
+----------------------------+
```

- The number keypad opens by itself. Nobody should have to switch keyboards.
- Says **why** a code is coming before it sends one. A code arriving unexplained reads as a scam.
- Endpoint 1. Task **P2-01**.

## 2 — One-time code

```
+----------------------------+
|  <                         |
|   Enter the code            |
|   We sent it to 09xx xxx 89 |
|                            |
|    [_] [_] [_] [_] [_] [_] |
|                            |
|   Send it again in 0:42    |
|                            |
+----------------------------+
```

- The number shown back is **masked**, so somebody glancing over a shoulder learns nothing new.
- Resend is a countdown, not a button that can be hammered.
- A wrong code says *"That code is not right. Check the message and try again."* — never how many tries are left, and never whether the number is registered.
- Endpoint 2. Tasks **P2-02**, **P2-03**, **P2-06**.

## 3 — Waiting — the home screen

**One parcel fills the screen.** This is the decision that shapes the app: at the cabinet, the receiver wants one button, not a list to read.

```
+----------------------------+          two or more waiting:
|                        [H] |          +----------------------------+
|                            |          |  2 parcels waiting     [H] |
|   A parcel is waiting      |          +----------------------------+
|                            |          | Cabinet A1 - Box 04        |
|   Cabinet A1               |          | Arrived 14:32            > |
|   Box 04                   |          +----------------------------+
|   Arrived 14:32            |          | Cabinet A1 - Box 11        |
|                            |          | Arrived Monday           > |
|  +----------------------+  |          +----------------------------+
|  |    SCAN TO OPEN      |  |          |  +----------------------+  |
|  +----------------------+  |          |  |    SCAN TO OPEN      |  |
|                            |          |  +----------------------+  |
|   Can't scan? Use a code   |          +----------------------------+
+----------------------------+
```

- **Nothing waiting** is its own state, not an empty list: *"Nothing waiting. We will tell you when a parcel arrives."*
- The box number is large. It is what the receiver looks for on the cabinet.
- *Can't scan?* is quiet text, not a second big button. It is the back door, and it should look like one — see [api-contract.md](api-contract.md).
- `[H]` is history.
- Endpoint 5. Task **P4-04**.

## 4 — Scan

```
+----------------------------+
|  <                         |
|   [ camera viewfinder ]    |
|                            |
|    +------------------+    |
|    |                  |    |
|    |   Point at the   |    |
|    |  cabinet screen  |    |
|    |                  |    |
|    +------------------+    |
|                            |
|   Can't scan? Use a code   |
+----------------------------+
```

- **The phone scans the cabinet, never the reverse** — [ADR 0003](../adr/0003-parcel-locker-product.md). The QR only says which cabinet and when; the token proves who is holding the phone. A photograph of the screen opens nothing.
- The camera permission is asked for **here**, at the moment it is obviously needed — not on first open, before any trust exists.
- Refused permission is not a dead end: it drops to screen 6.
- Endpoints 6 and 9. Task **P5-02**.

## 5 — Opened

```
+----------------------------+
|                            |
|         Box 04             |
|        is open             |
|                            |
|   Take your parcel and     |
|   close the door.          |
|                            |
|  +----------------------+  |
|  |         DONE         |  |
|  +----------------------+  |
+----------------------------+
```

- The box number is the biggest thing on the screen. Twenty doors look alike.
- **An unclear result never shows this screen.** If the app cannot tell whether the door opened, it says so — task **P5-06**. A false success sends somebody away from their parcel.
- Task **P5-03**.

## 6 — Type the code

```
+----------------------------+
|  <                         |
|   Type the code from       |
|   your message             |
|                            |
|    [_] [_] [_] [_] [_] [_] |
|                            |
|   The code is in the        |
|   notice about your parcel. |
+----------------------------+
```

- Exists so a flat battery, a broken camera or a dark corridor does not trap a parcel.
- A wrong code says the same thing whether it is wrong, used or expired — **`CODE_REJECTED`** in [api-contract.md](api-contract.md). Anything else lets somebody at the keypad work out which codes are real.
- Endpoint 13. Tasks **P5-08**, **P5-09**.

## 7 — History

```
+----------------------------+
|  <   My parcels            |
+----------------------------+
| Box 04 - collected         |
| Tue 14:35                  |
+----------------------------+
| Box 11 - collected         |
| Mon 09:12                  |
+----------------------------+
```

- Read-only. Nothing here opens a door.
- Endpoint 7. Task **P7-01**.

## Rules that hold on every screen

1. **Never show a full name or another person's phone number.** The receiver's own masked number on screen 2 is the only phone number the app ever displays.
2. **A door opens only from a tap.** Never from a retry, a timer, or a screen coming back into view. One tap is at most one open request — task **P5-07**.
3. **An unclear result is never a success.** Say it is unclear.
4. **Every refusal has plain words.** The wording lives in [api-contract.md](api-contract.md), not invented per screen.
5. **A dead token sends the user to screen 1**, with no error message. It is not their fault and there is nothing for them to do about it.
6. **Nothing is a dead end.** Every failure offers the next thing to try, and the typed code is the floor under all of it.
7. **One hand, standing up.** Anything tappable sits in the lower two-thirds of the screen.

## What this does not decide

Colour, type, spacing, icons and motion. They wait until the API shape is agreed and the screens have stopped moving.

The cabinet screen is not here. It is a different front-end, a public terminal, with its own rules — see [architecture.md](architecture.md) rule 6.
