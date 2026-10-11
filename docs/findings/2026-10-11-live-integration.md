# Live app/server integration, 2026-10-11

The server's merged app contract is reachable at `https://giakhanh.tail2fb87d.ts.net/api/v1`. The [raw probe](2026-10-11-connectivity-retest.raw.log) contains timestamps, exact statuses, and response bodies. This was an unauthenticated transport and contract audit; it did not open a door.

| Check | Observed result | What it proves |
| --- | --- | --- |
| HTTPS 443 | Health, Swagger, ReDoc, and OpenAPI returned 200; all 26 active method/path pairs appeared in OpenAPI | Public HTTP routing and advertised routes |
| Receiver routes | `/cabinets`, `/settings`, `/me`, `/parcels`, `/bookings` returned 401 `TOKEN_EXPIRED` without a token | The routes require receiver auth; their success responses remain untested |
| Google auth | An invalid ID token returned 400 `GOOGLE_INVALID` | The route is enabled; real VGU sign-in remains untested |
| Cabinet routes | Session and command reads returned 401 `CABINET_UNKNOWN` without a key | The routes require device auth; command delivery remains untested |
| WSS 443 | Notification and locker sockets upgraded with 101, then closed unauthenticated clients with 1008 | WebSocket routing works; authenticated messages remain untested |
| MQTT TLS 8443 | TLS 1.3 succeeded; unauthenticated MQTT CONNECT received `20 02 00 05` | Broker is reachable and rejects an unknown client |
| MQTT WSS 10000 | HTTP 101 with `mqtt` subprotocol; same refusal | WebSocket broker is reachable, with no publish/subscribe proof |
| Private Tailscale | This machine is in a different tailnet and has no `giakhanh` peer | Direct `100.117.120.40` access was not tested; Funnel access above does not need tailnet membership |

The local Android working tree now has a build-time `SERVER_BASE_URL` from ignored `.env`, a booking screen for read/create/cancel and account phone correction, and a free-door request when one cabinet is identified. Existing Google sign-in, parcel list/history, QR pickup, settings, and cabinet UI use the shared API transport. The local debug APK embeds the Funnel address and builds; unit tests and static network/secret checks pass. The checked APK carries no credential. No Android device was connected for an interactive test.

**Next live gate:** use a designated VGU test account on a phone to verify Google ID token exchange, returned `token` and `expires_at`, `/me`, `/settings`, `/cabinets`, booking creation/read/cancellation, and a parcel list. Then provision a designated cabinet key and MQTT credentials to its test device; observe command polling, command acknowledgement, relay actuation, and the `door-closed` state transition for one controlled drop and pickup. A queued command or an HTTP success alone is not physical-door proof. FCM delivery likewise needs a real phone and server credentials.

The old phone-number OTP/code controls still appear in the app although endpoints 1 and 2 were retired from the target contract. The phone's typed pickup-code page intentionally does not call device-only `/cabinet/collect-by-code`; that operation belongs at the cabinet with its key. Resolve the legacy entry controls before a release. Offline reconciliation also needs its own end-to-end test.
