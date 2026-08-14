# The phone app — every screen

Parent: none

What the receiver's app is made of. Every screen, what leads where, and what each one must never show.

**Flow and rules only.** This file says what each screen is for, what leads where, and what it must never show. How it looks — colour, type, motion, the cabinet render — is [DESIGN.md](../../DESIGN.md). The two are kept apart so a look can change without re-arguing the flow.

Product is [ADR 0003](../adr/0003-parcel-locker-product.md). The cabinet screen is a different front-end and a different document — it shares nothing with this but the API.

## What the app is for

**This is not a browsing app.** The receiver opens it about twice per parcel: once when the notice arrives, once standing at the cabinet. Most sessions are under twenty seconds, one-handed, sometimes walking.

Everything below follows from that. The current parcel is a screen, not a row in a list. The button that matters is the biggest thing on it.

## The screens

| # | Screen | Reached from | Leads to |
| --- | --- | --- | --- |
| 1 | **Phone number** | first open, or a dead token | 2 |
| 2 | **One-time code** | 1 | 3 |
| 3 | **Home** | 2, the notice, or reopening the app | 4, 6, 8, 9 |
| 4 | **Scan** | 3, or 8 | 5, or back |
| 5 | **Opened** | 4, or 6 | 3 |
| 6 | **Type the code** | 3, or 4 when scanning fails | 5, or back |
| 7 | ~~History~~ | — | folded into **3**, see below |
| 8 | **Cabinet** | the tab bar | 4, 6 |
| 9 | **Settings** | the tab bar | — |

Eight screens. Two are register, three are tabs, three are the pickup.

**Three tabs: Home, Cabinet, Settings.** Decided 2026-08-07.

- **History is not a screen any more.** It is a section on Home, under the parcels. It is glanced at, not visited, and on a Home with nothing waiting it is what stops the screen looking broken. A full list stays reachable behind *All*.
- **Settings takes the freed tab.** PIN code, password, notices, language, theme. It had nowhere to live before.
- Screen number 7 is retired, not reused. A number is never given to a second thing.

```
   1 Phone number ──> 2 One-time code ──┐
                                        v
   notice tapped ──────────────────> 3 Home ◄──── 9 Settings
                                     │  ▲  │            ▲
                              scan ──┘  │  └── 8 Cabinet ┤
                                     │  │        │       │
                                     v  │        v       │
                                4 Scan ─┼──> 6 Type the code
                                     │  │        │
                                     └──┴> 5 Opened
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
- Refused permission is not a dead end. The viewfinder stays, the line under it becomes *"No camera. Type the code instead"*, and the quiet **Use a code** button already on the screen is the way to 6. It does **not** jump there on its own: a screen that disappears the moment you refuse gives nobody a chance to change their mind, and a refusal is often a mis-tap.
- **A read code has three answers, and only one of them is a door.** A live code
  at a cabinet where a parcel is yours goes to screen 5. A live code at any
  other cabinet of ours stays here and says *"Nothing waiting here."* An
  expired one says *"Code expired. Scan again."* — the same sentence whichever
  cabinet it names, so a dead code never reveals where your parcels are.
  Anything that is not our format is not answered at all.
- **A refusal never stops the camera.** The sentence appears, the viewfinder
  keeps running, and the cabinet draws a new code every 30 seconds — so "scan
  again" is advice the screen makes possible to follow. Each code is acted on
  once, which is what keeps one scan to one open request.
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

## 7 — History (retired as a screen)

Folded into Home on 2026-08-07. It is the **Collected** section under the parcel cards, showing the last few, with *All* leading to the full list.

- Read-only. Nothing here opens a door.
- Endpoint 7. Task **P7-01**, unchanged — the data is the same, only where it is drawn moved.

## 8 — Cabinet

```
+----------------------------+
|  <    Back gate            |
|  Box 04                    |
|  Ready - 14:32             |
|  +----------------------+  |
|  | [ the cabinet, with  |  |
|  |   your door lit, and |  |
|  |   the screen pushing |  |
|  |   into it ]          |  |
|  +----------------------+  |
|  +----------------------+  |
|  |     SCAN TO OPEN     |  |
|  +----------------------+  |
+----------------------------+
```

- **This is where the app answers "which door?"** A render of the real cabinet, your door lit, then the screen pushes into that door. How it is drawn is [DESIGN.md](../../DESIGN.md).
- **With nothing waiting it shows free boxes instead**, in green, so the receiver can tell a shipper the cabinet has room before they walk over. No motion in this state.
- **Never shows another person's parcel.** A door that is taken is drawn as taken, with nothing about who by.
- Free-box counts need an endpoint that does not exist yet — see [api-contract.md](api-contract.md) endpoint 18.

## 9 — Settings

```
+----------------------------+
|         Settings           |
|  Minh Nguyen  0912 345 678 |
|  ACCOUNT                   |
|   PIN code            Set >|
|   Change password        > |
|  NOTICES                   |
|   Parcel arrived      [on] |
|   SMS backup         [off] |
|  APP                       |
|   Language     Tieng Viet >|
|   Theme            System >|
+----------------------------+
```

- Nothing here opens a door.
- The PIN is a local screen lock, not a parcel code. It never travels to the server.
- **Theme is a setting, not a guess.** System, Light, Dark.

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
