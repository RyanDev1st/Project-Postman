# Google sign-in and receiver account contract

**Target for the Android app and the public FastAPI `/api/v1` server, 2026-10-11.** This is the single handoff for Google sign-in, first-account creation, and receiver session identity. The broader [API contract](api-contract.md) covers bookings, parcels, and cabinets. A route appearing in OpenAPI does not prove a real Google account can sign in.

## One flow, three identities

1. The **Web OAuth client ID** identifies this locker application to Google. It is a public identifier, not a student account and not a password. The Android build requests a Google ID token for this client ID; the server uses the *same* ID as the token's expected `aud`.
2. Google's signed **`sub`** identifies the selected Google account. The server keys the receiver account on `sub`, not on the email address, display name, phone number, or OAuth client ID. A first valid sign-in creates the receiver account; a later sign-in with the same `sub` returns to it.
3. The server issues its own opaque **`token`** for that receiver account. The app sends `Authorization: Bearer <token>` on subsequent receiver calls. The server resolves that session to a user ID and scopes `/me`, bookings, parcels, and history to that user.

```text
VGU account on phone -> Android Credential Manager -> Google ID token
Google ID token -> POST /api/v1/auth/google -> verify -> find/create user by sub
Server token -> app secure store -> Bearer token on receiver API calls
```

The app never receives the Google password. A student's email such as `student@student.vgu.edu.vn` is a profile attribute, not a credential or database key. Firebase Authentication is not part of this flow.

## Configuration and ownership

| Owner | Required configuration | Purpose |
| --- | --- | --- |
| Team/VGU Google Cloud project | One **Web application** OAuth client and one **Android** OAuth client for package `vn.edu.vgu.smartlocker` with the signing certificate SHA-1 of each test/release build | Google can issue an ID token to the app for the Web client audience |
| Android build | `GOOGLE_CLIENT_ID=<Web client ID>` in ignored root `.env`; rebuild after changing it | Passed to `GetSignInWithGoogleOption` |
| Public server | The **same** `GOOGLE_CLIENT_ID`; `GOOGLE_ALLOWED_DOMAINS=vgu.edu.vn` unless VGU supplies a narrower approved set | Verify token audience and VGU Workspace membership |

The Web client's Authorized JavaScript origins and redirect URIs may both be blank for this native ID-token flow. Do not enter the Funnel URL or `/auth/google` as a redirect: the app posts the token itself. The team must confirm the OAuth consent audience and that a designated VGU account may use the client. The current local Android ID belongs to a different Google/Firebase-linked project; replace it with the agreed team-owned ID before the positive sign-in test. Do not send an OAuth client secret, Google password, service-account JSON, or raw ID token to the app team or into this repo. Firebase `google-services.json` and server `GOOGLE_APPLICATION_CREDENTIALS` concern notifications/distribution, not this login; using a team-owned notification project is a separate decision. [Google's Android ID-token setup](https://codelabs.developers.google.com/sign-in-with-google-android) distinguishes the Android and Web OAuth clients.

## HTTPS API contract

**Normal sign-in and first-account creation:** `POST /api/v1/auth/google`, `Content-Type: application/json`, no Bearer header.

```json
{"id_token":"<Google ID token>","full_name":"Optional profile name"}
```

`id_token` is required. `full_name` is optional and may seed a new account's display name; it never establishes identity or overrides an existing name. The phone does not send an email, student ID, or phone number to prove who signed in.

**200 response:**

```json
{"token":"<opaque app session token>","expires_at":"2026-10-12T00:00:00+00:00"}
```

`expires_at` is an ISO 8601 UTC timestamp. The app stores `token` securely and uses it as a Bearer token. A newly created Google account may have an empty phone number: `GET /api/v1/me` returns `{"full_name":"...","phone_number":""}`. The receiver supplies a phone number only when booking a box through `POST /api/v1/bookings`; no SMS or Firebase sign-up is required. `POST /api/v1/auth/refresh` exchanges a valid Bearer session for a new one; `POST /api/v1/auth/logout` ends it.

| HTTP | Top-level `code` | Meaning |
| --- | --- | --- |
| 501 | `GOOGLE_OFF` | Server Web client ID is missing |
| 400 | `GOOGLE_INVALID` | ID token cannot be verified, has the wrong audience, or conflicts with an already linked account |
| 403 | `GOOGLE_DOMAIN` | Verified token lacks an allowed VGU Workspace `hd` claim |
| 401 | `TOKEN_EXPIRED` | Only for the optional existing-session linking request when its Bearer token is invalid |

Clients switch on `code`; a server `message` is optional and is not shown raw. Do not log ID tokens or app session tokens.

## Server validation and account ownership

Verify Google's signature using Google's published keys, `iss`, `aud == GOOGLE_CLIENT_ID`, and `exp` **before** reading claims or touching a user row. Use the Google verifier's key cache; do not trust a decoded but unverified JWT. Require a nonempty `sub`. For VGU access, require `hd == "vgu.edu.vn"` or `hd` ending in `.vgu.edu.vn` (including the dot); reject missing `hd`, personal Gmail, and lookalike domains such as `notvgu.edu.vn`. [Google identifies `hd` as the Workspace membership claim](https://developers.google.com/identity/sign-in/android/backend-auth); an email suffix alone is not proof.

Do **not** require the email domain to equal `hd`. A managed account may have an alias such as `student@student.vgu.edu.vn` while Google reports `hd=vgu.edu.vn`; either allowed `hd` value admits the account. Email may be stored as contact/display data, but changing it must not change account ownership. Without an existing authenticated app session, find the receiver by verified `sub` or create a new one. Do not silently attach an unlinked receiver record merely because its email matches; migration/linking needs an explicit authenticated session or a separately reviewed procedure. With an optional valid Bearer session, link `sub` only to that session's user and refuse if it already belongs to a different user.

After sign-in, protected receiver routes must resolve the opaque session to one active user and query only that user's data. A client-provided email, user ID, or phone number must not select whose parcels or bookings are returned.

## Acceptance checks before calling login ready

1. The team-owned Web ID is identical in the Android build and server environment; the Android OAuth client includes the installed APK's package and signing SHA-1. No personal project is used.
2. On a real phone, a designated VGU Workspace account signs in; `/auth/google` returns `token` and `expires_at`; `/me` returns that same account. Repeating sign-in with the same `sub` does not create a second user.
3. A VGU student email succeeds when its signed `hd` is either `student.vgu.edu.vn` **or** the parent `vgu.edu.vn`. Personal Gmail and an absent/outside `hd` receive `GOOGLE_DOMAIN`.
4. An invalid, expired, or wrong-audience ID token receives `GOOGLE_INVALID`. A token for one user cannot read another user's `/me`, booking, or parcels.
5. A new account with no phone can read `/me` and then provide its number on its first booking. Changing an email alone does not transfer that account or its parcels.

The 2026-10-11 [transport retest](../findings/2026-10-11-live-integration.md) only proved that `/auth/google` is deployed and rejects a deliberately invalid token. It did not verify a positive VGU sign-in or any authenticated account-isolation check.

## Server changes exposed by this contract

The current FastAPI `google_identity` also requires `email.endswith("@" + hd)`, which can reject a VGU email alias even when the signed `hd` is allowed. Remove that extra equality requirement and test both allowed `hd` forms above. The current no-session branch also links an unbound user by matching email; replace that with `sub` lookup/create, or specify and test an explicit migration path. These are source-code findings, not claims that the live host has already been corrected.
