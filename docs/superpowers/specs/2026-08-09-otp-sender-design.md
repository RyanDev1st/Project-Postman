# OTP sender server — design

Date: 2026-08-09
Status: draft, awaiting review

## Goal

A small server that sends a 6-digit one-time code by SMS so that registering or
logging in with a phone number works end to end. It implements endpoints 1 to 4
of `docs/reference/api-contract.md` exactly, so the Android app's
`Api.requestCode()` / `Api.verifyCode()` work against it with no app change.

The test phone is `0908619328` (E.164: `+84908619328`), the owner of this repo.
SMS text is in Vietnamese.

## Why these choices

| Choice | Why |
| --- | --- |
| Kotlin + Ktor, module `:otp-server` under `src/otp-server/` | The repo is Kotlin; one language, one Gradle wrapper. Module added to the root build like `:app`. |
| SpeedSMS as the first real provider | De-facto standard in VN dev articles; self-serve signup, API needs only an access token; free test SMS on registration; cheapest per SMS (~390–500đ). Abenla needs a sales process and brandname paperwork; Firebase's free mode never sends a real SMS (code shows in console) and real SMS costs $0.06+. |
| `SmsProvider` interface, two implementations | `SpeedSmsProvider` (real) and `LogSmsProvider` (demo mode prints the code to the server console). Swap or add a provider without touching the endpoints. Abenla/Firebase can slot in later. |
| In-memory store, no database | Demo scale. Codes and tokens live in maps, expire by timestamp. A restart forgets everything — acceptable for testing, noted in the roadmap task. |
| HTTPS not served | Localhost testing only. The app demands `https://` from the server it talks to, so a local run needs the app's server address overridden — see Testing. |

## Endpoints (contract #1–4)

| # | Path | Body | Success | Refusal codes |
| --- | --- | --- | --- | --- |
| 1 | `POST /auth/request-code` | `{"phone_number": "0908619328"}` | `{}` | `PHONE_INVALID`, `RATE_LIMITED`, `SEND_FAILED` |
| 2 | `POST /auth/verify-code` | `{"phone_number": "...", "code": "123456"}` | `{"token": "...", "expires_at": <epoch ms as string>}` | `WRONG_CODE` (also covers expired code, per contract wording rule) |
| 3 | `POST /auth/refresh` | `{}` + `Authorization: Bearer <token>` | `{"token": "...", "expires_at": <epoch ms as string>}` | `TOKEN_EXPIRED` |
| 4 | `POST /auth/logout` | `{}` + bearer | `{}` | `TOKEN_EXPIRED` |

Refusal body shape follows the contract: `{"code": "<CODE>"}`. The app maps
`WRONG_CODE` to its own message; codes it does not know (the rest) fall to
`UNKNOWN` and its generic sentence — acceptable for a demo server, and the
`PHONE_INVALID`/`RATE_LIMITED`/`SEND_FAILED` codes are documented in
`docs/reference/api-contract.md` as part of this change.

## Phone normalization

Accept what a Vietnamese user types or the app sends: `0908619328`,
`84908619328`, `+84908619328`. Normalize to E.164: strip spaces/dashes, strip
leading `0`, prefix `+84`, reject anything that is not exactly 11 digits after
normalization (VN mobiles: `0` + 9/10 digits). SpeedSMS receives the number
without `+`.

## Code lifecycle

1. `request-code`: check a 60-second resend cooldown per phone (`RATE_LIMITED`).
   Generate 6 digits with `SecureRandom`, store `{code, expiresAt (5 min),
   triesLeft (5), sent}` under the normalized phone.
2. Send via provider. Only if the provider confirms delivery is stored: on
   failure the entry is removed and `SEND_FAILED` returned — a code the user
   never received must not be usable.
3. `verify-code`: constant-time compare (`MessageDigest.isEqual`); wrong code
   decrements `triesLeft`; expiry or zero tries returns `WRONG_CODE` and the
   entry is deleted (single-use, no brute force).
4. Correct code: delete the entry, mint a token, return it.

SMS text (Vietnamese, ASCII-safe for SpeedSMS long-code):

```
Mã xác thực của bạn là 123456. Có hiệu lực trong 5 phút.
```

## Tokens

32 random bytes, base64, stored in memory as `token -> {phone, expiresAt}`.
Expiry 30 days. Verified constant-time on every bearer call. `refresh` mints a
new one; `logout` deletes. Never logged — mirrors rule 3 in `Http.kt`.

## Config (env vars)

| Var | Meaning | Default |
| --- | --- | --- |
| `PORT` | listen port | `8443` (http) |
| `DEMO_MODE` | `1` = log provider, no network | off |
| `SPEEDSMS_TOKEN` | SpeedSMS API access token | — |

`DEMO_MODE=1` or missing `SPEEDSMS_TOKEN` → `LogSmsProvider`; otherwise
`SpeedSmsProvider`. Demo mode still runs the full request/verify lifecycle.

## SpeedSMS integration

`POST https://api.speedsms.vn/index.php/sms/send`
- Basic auth: `Authorization: Basic base64(SPEEDSMS_TOKEN + ":x")`
- JSON body: `{"to": "84908619328", "content": "...", "type": 2}` (type 2 = long code; content is UTF-8 JSON, Vietnamese passes as-is)
- Success = HTTP 200 and JSON `status` field with code `1`.
- Only a confirmed delivery counts as sent.

## Files

```
src/otp-server/
  build.gradle.kts          kotlin("jvm") + application, Ktor server, kotlinx-json
  src/main/kotlin/otp/
    Main.kt                 engine setup, routes, wiring
    AuthStore.kt            in-memory codes + tokens, lifecycle logic
    Phone.kt                normalization/validation
    Sms.kt                  SmsProvider interface
    SpeedSms.kt             real provider
    LogSms.kt               demo provider
```
Root changes: `settings.gradle.kts` `include(":otp-server")`,
`gradle/libs.versions.toml` Ktor entries, one row in `docs/README.md`.

## Testing

1. Build: `./gradlew :otp-server:build`.
2. Demo-mode curl flow: `request-code` → code visible in server console →
   `verify-code` with that code → token returned; wrong code → `WRONG_CODE`;
   replay → `WRONG_CODE` (single-use).
3. Real SMS test: user registers at connect.speedsms.vn, pastes token, runs
   server with `SPEEDSMS_TOKEN`, calls `request-code` with `0908619328`, checks
   the SMS arrives on the phone, then verifies.
4. Android app against the server: run server in demo mode, point the app's
   settings at `http://10.0.2.2:8443` (emulator) and complete registration
   through `PhoneNumberScreen` → `OneTimeCodeScreen`.

## Out of scope (YAGNI)

- No database, no multi-instance, no rate limiting beyond the 60s cooldown.
- No HTTPS (localhost only; production server is a separate task).
- No cabinet endpoints, no parcels, no settings.
- No SMS-template approval handling — SpeedSMS long code needs none for testing.
