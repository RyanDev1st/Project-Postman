# Architecture — the parts and how they connect

Plain description of the whole system. Edit this file when reality changes.

Product: a **parcel drop-off locker**. A shipper leaves a parcel in a box. The receiver picks it up with a phone. See [ADR 0003](../adr/0003-parcel-locker-product.md).

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

## Open questions

Move a line out of here when it is answered, and write the answer in the right doc.

- Does the cabinet have its own network connection — SIM or Wi-Fi? Nothing here works without one. (blocks P0-03, and it is the most urgent question in the project)
- How does the receiver get the notification — push through the app, Zalo, or SMS? (blocks P0-06)
- How long does a QR session code live, and how often does the screen refresh it? (blocks P0-07)
- Does the cabinet have a camera after all? Not needed for this design, but it would allow a no-app pickup later. (affects a future ADR only)
