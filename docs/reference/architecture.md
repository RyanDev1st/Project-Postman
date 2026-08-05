# Architecture — the parts and how they connect

Plain description of the whole system. Five parts. Edit this file when reality changes.

```
   ┌─────────────┐                    ┌──────────────┐        ┌────────────┐
   │  Phone app  │ ──── HTTPS ──────> │  API server  │ ─────> │  Database  │
   │  (IT team)  │ <─── answer ─────  │(Server team) │        │  users     │
   └─────────────┘                    └──────────────┘        │  lockers   │
        │                                    │                │  points    │
        │ camera                             │ email          │  history   │
        ▼                                    ▼                │  orders    │
   card barcode                        candidate password     └────────────┘
                                             │
                                             ▼
                                     ┌────────────────┐
                                     │ Locker hardware│
                                     └────────────────┘
```

## 1. Phone app — this repo

Runs on Android and iOS. It shows screens, takes taps, scans barcodes, and asks the server for everything else.

**The app decides nothing.** It does not decide who you are, which locker is yours, whether a locker is free, or whether the mode allows an open. It asks. This keeps one truth in one place, and it stops a modified app from opening a locker it should not.

## 2. API server — Server team

The only door between the app and the data. The app sends a request; the server sends an answer. What the app may send is fixed in [api-contract.md](api-contract.md).

The server also does the work the app must never do:
- check the password
- match a scanned card code to a user
- pick a free locker of the right size
- decide if the current mode allows an open
- command the locker hardware

## 3. Database — Server team

Remembers everything: users, lockers, locker points, open and lock history, orders, school events, and the current mode of each point.

## 4. Login and token

The user types the password **once**. The server answers with a token — a temporary pass. The app stores that token safely and shows it with every later request. The password is never sent again and never stored on the phone.

A token expires. When it does, the server refuses, and the app sends the user back to login. Once, with a message. Never in a loop.

## 5. Extras

| Part | Where it runs | What it does |
| --- | --- | --- |
| Barcode scanner | In the app | Reads a student card, or a code from the VGU Library app |
| Email sender | On the server | Sends the login name and password to candidates and parents |
| Locker hardware | At the locker point | Opens the metal door when the server tells it to |

## The rules that come out of this shape

1. **One door.** Every network call in the app goes through one file. See task P1-04.
2. **The server is the truth.** If the app and the server disagree, the server wins. Refresh, do not guess.
3. **An open is physical.** Never send an open request from a retry, a timer, or a screen refresh. Only from a direct tap.
4. **Unclear is not success.** If the network drops mid-request, the app does not know if the locker opened. It must say so.
5. **No secrets in the app.** Anything shipped inside the app can be read by anyone who has the app.

## Open questions

Move a line out of here when it is answered, and write the answer in the right doc.

- Does the API send each locker point's location, or does the app hold a fixed list? (blocks P3-04)
- One barcode format for student and lecturer cards, or two? (blocks P5-03)
- How does the receiver get the parcel open code — email, SMS, or in-app? (blocks P7-05)
