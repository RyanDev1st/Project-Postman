# The server

The API, the database, and every rule that decides whether a door opens.
Kotlin and Ktor, with SQLite — [ADR 0020](../../docs/adr/0020-the-server-is-kotlin.md).
It serves the contract in [api-contract.md](../../docs/reference/api-contract.md).

## Run it

```bash
./gradlew :server:run
```

First time, on an empty database, add the two cabinets the front-ends already
name and print their keys:

```bash
LOCKER_SEED=1 ./gradlew :server:run
```

It prints where the database went, where the certificate went, and every
address it can be reached on:

```
listening on https://localhost:8443
        or https://192.168.1.19:8443
```

## Three things to do once

### 1. Tell the app where the server is

`config/settings.json` ships with `server_base_url` blank, and the app says so
plainly rather than timing out. Fill it in with the address the server printed:

```json
"server_base_url": "https://192.168.1.19:8443"
```

For the Android emulator the host is always `https://10.0.2.2:8443`.

**It is left blank in git on purpose.** It is different on every machine, and a
LAN address is not something to commit. Expect this file to stay modified
locally; do not commit your copy.

The Gradle build copies that file into the APK, so a change needs a rebuild —
`./gradlew :app:assembleDebug`.

### 2. Trust the development certificate

The server serves HTTPS with a certificate it generated itself, once, into
`config/dev-cert/` — which git ignores, because the private key is in there.
Both front-ends refuse plain HTTP and there is no flag to turn that off, so
every device has to be told about that certificate once.

**On a phone:** send yourself `config/dev-cert/locker.crt` — that file is the
public half and carries no key — then Settings → Security → Encryption &
credentials → Install a certificate → CA certificate.

Debug builds accept it; **release builds do not**, so this can never reach a
student's phone. A real cabinet needs a real certificate.

**On the cabinet screen:** install the same file in the browser's certificate
store.

**On the ESP32:** paste it into the sketch as the root CA. See
[cabinet-firmware.md](../../docs/reference/cabinet-firmware.md).

### 3. Give the cabinet screen its key

```bash
./gradlew :server:run --args="cabinet add vgu-back-gate 'VGU Back Gate' 20"
```

That prints the key **once**. Put it in `src/cabinet/config.js` — which git
ignores — as `KEY`, along with `SERVER_BASE_URL`. The server keeps only a hash
and cannot show it again; if it is lost or leaked, replace it:

```bash
./gradlew :server:run --args="cabinet rotate vgu-back-gate"
```

## Sending a real text message

Without `SPEEDSMS_TOKEN` the server uses a demo provider that **prints the
one-time code to its own console** instead of sending anything. That is enough
to register and test, and it is chosen only when no token is set — never as a
quiet fallback when a real send fails.

With a token, codes go by SMS through SpeedSMS:

```bash
SPEEDSMS_TOKEN=... ./gradlew :server:run
```

## Check it

```bash
python scripts/checkserver.py
```

Starts a fresh server over real TLS, registers two phones, drops a parcel,
collects it by scan, watches the hardware take the open command, and then tries
to steal the parcel four ways — an invented code, a code from the wrong
cabinet, somebody else's token, and the same code twice. Every one must be
refused by the server. 29 checks.

It verifies the certificate properly rather than skipping the check, which is
how it found that Ktor's default key size is one Android and Chrome refuse.

## Settings

| Variable | Means | Default |
| --- | --- | --- |
| `PORT` | Port to listen on | `8443` |
| `LOCKER_DB` | Where the SQLite file goes | `data/locker.db` |
| `LOCKER_CERT_DIR` | Where the certificate lives | `config/dev-cert` |
| `LOCKER_SEED` | `1` creates the two demo cabinets on an empty database | off |
| `SPEEDSMS_TOKEN` | Send real SMS instead of printing codes | unset |

Everything else — timeouts, code lengths, lockout counts — comes from
`config/settings.json`, which is the same file the app and the cabinet read.
One copy, edited by a person.

## Layout

| Path | What is in it |
| --- | --- |
| `Main.kt` | Routes, TLS, wiring, and the `cabinet` setup commands |
| `Db.kt`, `Schema.kt` | SQLite, and every table |
| `Ids.kt` | Random values, hashing, constant-time compare |
| `Phone.kt` | Vietnamese numbers, one stored form, and name masking |
| `Refusals.kt` | Every way the server may say no |
| `auth/` | One-time codes, tokens, SMS providers, endpoints 1–4 |
| `parcels/` | Endpoints 5–8, and `Collect.kt` — the rule a door turns on |
| `cabinet/` | Endpoints 9–14 and 18, sessions, boxes, and the hardware queue |
