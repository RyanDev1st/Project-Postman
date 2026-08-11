# 0011 — Google sign-in, linked to a phone. No passwords, ever

- **Status:** superseded by [0012](0012-passwords-on-a-phone-account.md)
- **Date:** 2026-08-11
- **Deciders:** IT team

> **Superseded the same day.** The Google decision below is unchanged and was restated in 0012. Only "no passwords" was reversed: the case against was put to Ryan, and he chose passwords anyway. Read [0012](0012-passwords-on-a-phone-account.md) for what is true now.

## Context

The app signs a receiver in with a phone number and a one-time code. That works end to end against the reference server. Ryan asked for two more things: sign in with Google, and a "credential login system", built the simplest and safest way.

Three facts shape the answer.

- **An account without a phone number is useless.** The shipper finds the receiver by typing a phone number on the cabinet screen. A person who signed up with Google alone has no number, so nobody could ever send them a parcel. Their account would look fine and receive nothing.
- **A password is a second thing to steal.** It can be phished, guessed, or reused from a site that has already been breached. It has to be stored, rate-limited, and reset — and the reset path is a one-time code to the phone, which is the door we already have.
- **We do not own the real server.** The Server team does. Anything decided here is a proposal, under [ADR 0005](0005-we-propose-they-object.md).

## Options

| Option | Good | Bad |
| --- | --- | --- |
| **A** — Google, plus an email and password | "Credential login" in the most literal sense | Adds the one credential that gets stolen in bulk. Needs hashing, reset, lockout, and a breach plan. The reset is an SMS code, so it adds no way in that we do not already have |
| **B** — Google as a second door onto a phone-proved account. No passwords | Nothing to steal from our database. One account, one identity, always reachable by number. Small | A person with no Google account sees one button fewer. Losing the phone number still means losing the account |
| **C** — Firebase Authentication for all of it | Least code by far. Google, phone and passwords in one SDK | Moves identity out of our API contract and onto Google, which is work the Server team owns. `docs/reference/releasing.md` already says adopting it needs an ADR and a conversation with them. It also puts an SDK and a billing relationship into the app for a problem we have already solved |

## Decision

We choose **B**.

Google sign-in is added as **endpoint 19**, `POST /auth/google`. It is a second door onto an account that a one-time code has already proved, never a way to make one. A Google account nobody has linked gets `PHONE_REQUIRED`, and the app falls back to the phone number and the code.

**No passwords are added.** On the phone, the single sign-in sheet is Android's **Credential Manager**, which is also how Google sign-in is done on modern Android. Reading "credential login system" as that one sheet, rather than as a password field, is what makes this the smallest safe answer — and it is the same API that passkeys arrive on later, so adding them would not move the app's front door.

The server checks the Google ID token itself, offline: signature against Google's published keys chosen by `kid`, `alg` fixed at RS256 in our code and never read from the token, `iss`, `aud` equal to our own client id, and `exp`. It never calls Google's tokeninfo endpoint, which would put the network and a rate limit on every login. The account is keyed on Google's `sub`, never the email address, because a person can change their Gmail address and keep the account.

## Why

The safest password is one that was never stored. Phone plus one-time code already proves a person, and it proves the one fact the product needs — the number a shipper will type. Google makes getting back in faster; it does not need to make an account, and letting it would make accounts that cannot receive parcels.

## What we accept

- **A person who loses their phone number loses the account.** Google does not rescue them, because Google only ever opens an account a number already proved. Account recovery is a real gap and is not solved here.
- **The first sign-in is always the slower one.** Even with Google, a new person types a number and waits for an SMS.
- **One more thing to configure per environment.** A server with no `GOOGLE_CLIENT_ID` answers `GOOGLE_OFF`, and the app must hide the button rather than show one that cannot work.

## What Ryan has to do before this can be finished

The server side is written and tested. The app side cannot be built until an OAuth client exists, and **only the project owner can create one** — an agent must not create credentials.

1. In the Firebase console for `project-postman-575ed`, add an **Android** OAuth client with package `vn.edu.vgu.smartlocker` and the debug signing fingerprint below, and a **Web** client. The app and the server both need the **Web** client id: Credential Manager sends it as `serverClientId`, and the server compares it to `aud`.
2. Download the refreshed `google-services.json`. Today's file has `"oauth_client": []`, which is why nothing can work yet.
3. Give the server `GOOGLE_CLIENT_ID=<the Web client id>`.

Debug signing fingerprint, from `~/.android/debug.keystore` on Ryan's machine:

```
SHA1: 77:38:57:27:A1:6D:B6:55:5A:AB:43:D5:BA:DA:B9:AF:A0:0E:E6:0D
```

This is a certificate fingerprint, not a secret — it is meant to be published in the console. It is **per machine**, so a second developer's debug builds need their own added, and the release keystore (**P8-09**, still unowned) will need its own again.

## Affects

- **P2-08** — new task, the app side. Blocked on the client id above.
- **Endpoint 19** and three refusal codes in [api-contract.md](../reference/api-contract.md).
- `src/otp-server/` — `Google.kt`, `GoogleCerts.kt`, one route, and the link map in `AuthStore`.
- Reverses nothing. [ADR 0005](0005-we-propose-they-object.md) still applies: the Server team may object to the shape.
