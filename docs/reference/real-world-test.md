Parent: architecture.md

# The real-world test

One person, one phone, one laptop. The laptop is the server and the cabinet.
The phone is the app, installed the way a student would install it.

This is the first run where nothing is a simulator except the metal.

## Status

Written 2026-08-17. Not yet run. When it has been run, the result goes in
`docs/findings/YYYY-MM-DD-real-world-test.md` with what was actually seen -
not here.

## What this proves, and what it does not

| Real | Not real |
| --- | --- |
| The app, built and installed from a link, like a student | **No door moves.** `cabinet-agent.py` prints what a cabinet would do |
| A real Android phone, real camera, real wifi | One person plays both the shipper and the receiver |
| The server, over HTTPS, on a LAN address | The one-time code arrives in the laptop terminal, not by SMS |
| The cabinet screen at 1024x600, the size in [ADR 0021](../adr/0021-build-for-the-proposed-hardware.md) | **No notice arrives on the phone.** Push is not built - Phase 4 |

**The parcel notice does not exist yet.** Nothing in the app sends or receives
a push; there is no Firebase Messaging in the build. The receiver finds the
parcel by opening the app, which asks the server. Do not wait for a
notification. Step 6 below is written that way on purpose.

## Before you start

```
python scripts/checktest.py     is a phone test even possible right now
python scripts/checkloop.py     does the whole journey still work, with no phone
```

`checktest.py` is eight checks, each one a way this test dies without saying
why. Every failure prints its own fix. Do not start until it says `ready for a
phone`.

`checkloop.py` is the journey itself — register, drop, work the cabinet,
list, scan, collect — against the real endpoints with no device in the room.
If it fails, the fault is in the rules and no amount of tapping will show you
anything a terminal has not already said. Run it first; it takes seconds.

The one worth understanding: **the certificate names one address.** It is
written once and then reused. Join a different wifi, the laptop's address
changes, the certificate does not, and the phone refuses the connection on a
hostname mismatch. On the phone that looks like a dead server. Check 1 catches
it; the fix is to delete `config/dev-cert` and start the server again.

## Setup - four things running on the laptop

Four terminals. Leave them all open.

**1. The server.** The origins matter: a browser origin is scheme, host **and**
port, and without the port the screen is refused with no useful message
(BUG-010).

```
LOCKER_CABINET_ORIGINS="127.0.0.1:8137,localhost:8137" ./gradlew :server:run
```

Read its first lines. `sms  DEMO - codes print here` means the one-time code
appears in **this** terminal. That is where you read it from.

**2. The cabinet screen.**

```
python -m http.server 8137 --directory src/cabinet
```

Open `http://127.0.0.1:8137/index.html` in a browser and put the window at
**1024x600**. That is the screen in the bill of materials, and the layout was
measured at exactly that size. Full-screen on a bigger monitor is not the same
test.

**First, teach the browser the certificate — or the screen will say the server
cannot be reached.** The page is served over plain HTTP but it calls the
server over HTTPS, and that certificate is self-signed. The browser refuses it
silently: every call fails, the screen says *"The server could not be
reached"*, and the server log stays completely empty, which reads exactly like
a dead server. Seen on Ryan's screen on 2026-08-17, with the server running
perfectly well the whole time.

Once, in the same browser:

    open https://127.0.0.1:8443/health
    Advanced  ->  Continue to 127.0.0.1 (unsafe)

You should see `{"ok":true}`. Now go back to the cabinet screen. It works for
as long as that browser remembers, which is the session.

To stop doing it every time, install `config/dev-cert/locker.crt` into
**Windows -> Manage user certificates -> Trusted Root Certification
Authorities**. Same file the phone gets, and the same one-time job.

**3. The cabinet's electronics.**

```
python -u scripts/cabinet-agent.py
```

This stands in for the Pi and the ESP32: it asks the server for open commands,
says the relay fired, and then waits for you to say the door shut. It is not
evidence a door moved - it is evidence the server asked for one.

**4. The build, on the phone.**

```
python scripts/testbuild.py
./gradlew :app:assembleDebug
./gradlew :app:appDistributionUploadDebug
```

`testbuild.py` writes this laptop's address into the build. Skip it and the
app installs, opens, looks perfectly correct, and reaches nothing.

Then, once per phone: install `config/dev-cert/locker.crt` under **Settings >
Security > Encryption & credentials > Install a certificate > CA
certificate**. The server's certificate is self-signed, so nothing trusts it
until somebody says so. A debug build accepts a certificate installed by hand;
a release build never will, which is correct.

## The run

Ten steps. At each one, the middle column is what to look at - not the screen
you just tapped, but the place the evidence actually is.

| # | Do this | Look here | Pass |
| --- | --- | --- | --- |
| 1 | Open the app on the phone | The phone | It opens and asks for a phone number. **There is no Google or VGU button any more** - both skipped the login entirely (BUG-011) |
| 2 | Type your real number, tap send | **The server terminal** | A six-digit code is printed there |
| 3 | Type that code in, **and your name** | The phone | The app reaches Home |
| 3b | - | - | The name field is new. Nothing else in the app ever asks for one, and without it the cabinet shows the shipper `***` (BUG-012). It is taken only the first time |
| 4 | On the cabinet screen, tap "Deliver a parcel", type **your own number**, tap Find | The cabinet screen | A masked name, such as `Nguyen V. M***`. **No full name. No phone number.** |
| 5 | Tap "Yes, open a box" | **The cabinet-agent terminal** | `RELAY nn ON`, then `told the server: ok` |
| 6 | In the agent terminal, press `d` for a drop | The agent terminal | It reports the door shut |
| 7 | Open the app again on the phone | The phone | The parcel is listed, with the box number from step 5 |
| 8 | Tap to collect, and point the camera at the QR **on the cabinet screen** | The phone | The code is read |
| 9 | - | **The cabinet-agent terminal** | `RELAY nn ON` again, same box |
| 10 | In the agent terminal, press `c` for a collect | **The server terminal** | The parcel is written off |

**Step 8 is the one to watch.** The QR on the screen is not a key. It says only
*which cabinet, at what moment* - and it is redrawn every 30 seconds. Identity
comes from being signed in on the phone. A photograph of that screen, sent to
somebody else, must open nothing. If you want to test that, take a photo and
try it from a signed-out phone; it should refuse.

## Afterwards

```
python scripts/checkserver.py
```

Then look in the database at what the run left behind:

```
sqlite3 data/locker.db "select id, box_number, state from parcels order by rowid desc limit 3;"
sqlite3 data/locker.db "select box_number, state from boxes where state != 'free';"
```

A finished collect leaves the parcel `collected` and the box `free`. A parcel
still sitting in `opening` means step 10 never landed - that is BUG-008's
territory, and the timeout gives it back after 120 seconds.

## What to write down

For each step: what you did, what you saw, on which phone. Not "it worked".

The things most worth catching, because none of them are visible from the
laptop:

- The masked name in step 4. Was any part of a real name or number readable by
  somebody standing behind you?
- How long step 2 took. A student who waits 30 seconds for a code assumes the
  app is broken.
- Whether the QR in step 8 read on the first try, and from how far away.
- Anything the app said that a person outside this team would not understand.
