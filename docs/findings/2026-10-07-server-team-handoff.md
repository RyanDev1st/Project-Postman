# API team findings: Funnel, broker, and app contract

The API implementation is on the server repository's separate branch [`integration/app-contract-2026-10-07`](https://github.com/TonyStark1616/VGU-Smart-Locker/tree/integration/app-contract-2026-10-07), with [draft PR #1](https://github.com/TonyStark1616/VGU-Smart-Locker/pull/1). It is not merged or deployed. Its deployment note lists migration, Google/FCM credentials, cabinet-key setup, and the still-missing ESP32 command bridge and two offline routes. The live results below therefore describe the existing server, not that branch.

The app repository's separate `integration/live-server-adapter-2026-10-07` branch also fixes the cabinet browser's `/api/v1` URL join, passes the courier's typed label number with a drop, and adapts the local command stand-in to `box_number` and `purpose`. This stand-in prints simulated relay actions; it is not evidence that an ESP32 door moved. The Android and cabinet deployment addresses remain unset in tracked configuration while the public API contract is incomplete.

The server branch's focused local tests pass and its Python 3.11 Docker image builds; the image's generated OpenAPI includes the new Google sign-in and cabinet command routes. This is build evidence only. The public server has not deployed that image, and production credentials, notifications, and physical doors remain untested.

**Current retest, 16:20–16:26 UTC:** [raw retest output](2026-10-07-connectivity-retest.raw.log) shows `GET /api/v1/health` HTTP 200 with `api`, `redis`, and `mqtt` true; `/docs`, `/redoc`, and `/openapi.json` are HTTP 200. The OpenAPI document now has **30 paths**. Both app WebSocket paths upgrade with HTTP 101 and then close the unauthenticated probe with code 1008 (`Unauthorized`), which is expected without credentials. MQTT TLS 8443 and MQTT WSS 10000 also accept transport connections and reject unauthenticated CONNECT with CONNACK `20 02 00 05`. The earlier port-443 outage and WebSocket upgrade failure are no longer observed. Authenticated messaging is untested.

The server remains **not ready for an app/door test**: OpenAPI lists 15 of 26 non-retired app-contract method/path pairs and lacks the other 11, including every `/cabinet/*` command/session/drop route and `/devices`. The live `GET /api/v1/cabinets` twice returned HTTP 500 without a token, while other protected reads returned a normal 401. Present routes have incompatible fields: `expires` versus `expires_at`, booking `parcel_size` versus `size` and a different response, pickup `qr_session_code` versus `session_code`, and settings without `settings_version`. Refusals observed as `{"detail":"Authentication required"}` lack the top-level `code` both clients use. The current [app contract](../reference/api-contract.md) has the server team's completion list and test gate. The draft FastAPI PR is not deployed.

The sections below preserve earlier observations and hypotheses for comparison; they do not describe the current transport state. The Tailscale console screenshot shows the Windows 11 machine `giakhanh` connected with Funnel enabled; it does not show individual Funnel ports or Docker service health.

**Earlier outage retests at 15:41–15:42 and 15:52 UTC:** [raw retest output](2026-10-07-connectivity-retest.raw.log) shows HTTP 502 for `/api/v1/health`, `/docs`, `/redoc`, `/openapi.json`, and all tested port-443 WebSocket upgrades on both sweeps. A separate health check at 15:51 UTC also returned 502. Public MQTT TLS on 8443 and MQTT WSS on 10000 still accepted handshakes and returned unauthenticated CONNACK `20 02 00 05`. At that time the port-443 upstream path was unavailable from outside; the response could not identify whether the backend, Nginx, or Funnel forwarding failed. This outage was no longer observed at 16:20 UTC.

The IT test laptop's Tailscale status is `Running` but has zero peers and a different tailnet DNS suffix (`tail25fa89.ts.net` versus server `tail2fb87d.ts.net`). It cannot directly query the server's private `100.117.120.40` address from its current tailnet. The host-side checks above must be run on `giakhanh` or a device joined to that tailnet; public Funnel probes are the only current remote evidence.

**Earlier retest at 15:09–15:10 UTC:** public MQTT transport became reachable on both ports, but the API reported `mqtt:false` and public app WebSockets returned 403. The table below records that earlier, last-readable API state; it is not the current port-443 result. No login, authenticated MQTT publish, or door action was attempted.

| Retest probe | Exact observation | Interpretation / server-side next check |
| --- | --- | --- |
| `GET /api/v1/health` | HTTP 200; `services.mqtt:false`, `redis:true`, `api:true` | The answering API worker is still disconnected from MQTT. Check its broker credentials, connection logs, and whether four workers share one MQTT client ID. |
| `GET /openapi.json` | HTTP 200, 31,059 bytes, SHA-256 `c346cf6aabeb6e697902d847a9b5830d15a97f76a0002e91feda611a699a765f`, 21 paths; zero app-contract routes | The public API contract is unchanged from the first probe. The port fixes did not deploy the app routes. |
| TLS + unauthenticated MQTT CONNECT on `:8443` | TLS 1.3 handshake succeeds; CONNACK bytes `20 02 00 05` | Broker reachable through Funnel; return code `05` means **not authorized**, expected with no credentials. This does not prove authenticated publish/subscribe works. |
| MQTT WSS on `:10000` | HTTP 101, `mqtt` subprotocol; unauthenticated MQTT CONNACK bytes `20 02 00 05` | Broker WebSocket listener and Funnel upgrade now work. Authenticated MQTT flow still needs a credentialed server-side test. |
| WSS `:443/ws/notifications/probe`, `/ws/locker/LOCKER-001`, and invalid `/ws/doesnotexist` | All HTTP 403 with empty body and Nginx response headers | App WebSocket upgrades remain rejected; matching 403 on an invalid path suggests a common upgrade/proxy/routing failure, but cannot identify the hop remotely. Compare direct backend, Nginx, and Funnel upgrades as below. |
| Ordinary GET of `/ws/notifications/probe` and `/ws/doesnotexist` | Both HTTP 404 JSON `{"detail":"Not Found"}` | This is expected for WebSocket-only routes; it does not prove the WebSocket upgrade path works. |

## What to check on `giakhanh`

| Observation | What it establishes | Next check on the Windows host |
| --- | --- | --- |
| HTTPS `/api/v1/health`, `/docs`, `/redoc`, and `/openapi.json` work | DNS, public Funnel port 443, and the API HTTP path work from outside | Keep this as the working control while testing the other ports. |
| Health repeatedly returns `"mqtt": false` | The API process answering us is not connected to MQTT | Inspect `docker compose ps` and backend/Mosquitto logs; test broker reachability and authentication inside Docker. |
| `wss://.../ws/notifications/1` and `/ws/locker/LOCKER-001` return 403 with empty bodies | The public WebSocket upgrade is rejected; neither path reaches an accepted connection | Compare upgrades to `ws://127.0.0.1:8000/...`, `ws://127.0.0.1:80/...`, and public `wss://.../...` in that order; inspect Nginx access/error logs and the deployed app revision. |
| Earlier 8443 TLS EOF (now fixed) | The first probe could not complete a handshake; the retest completed TLS and reached Mosquitto | Preserve the working Funnel mapping. Test authenticated MQTT from the backend container. |
| Earlier 10000 WSS 502 (now fixed) | The first probe could not upgrade; the retest got HTTP 101 and an MQTT CONNACK | Preserve the working Funnel mapping. Test authenticated MQTT over WSS only if a client needs it. |

Run these read-only checks on `giakhanh` in PowerShell, from the server repository. Redact credentials from any logs sent back:

```powershell
tailscale funnel status --json
docker compose ps
docker compose logs --tail=100 backend mqtt nginx
curl.exe -sS http://127.0.0.1:8000/api/v1/health
docker compose exec backend python -c "import socket; socket.create_connection(('mqtt',1883),5).close(); print('broker TCP reachable')"
```

For WebSocket, send the same upgrade request directly to backend `:8000`, then Nginx `:80`, then Funnel `:443`. A `101 Switching Protocols` response is the expected handshake; curl may then time out because it is not a WebSocket client:

```powershell
curl.exe --http1.1 --max-time 3 -i -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' http://127.0.0.1:8000/ws/notifications/probe
```

Repeat with `http://127.0.0.1:80/...` and `https://giakhanh.tail2fb87d.ts.net/...`. If backend succeeds and Nginx fails, inspect the deployed `location /ws/` upgrade headers. If Nginx succeeds and Funnel fails, inspect Funnel's port-443 mapping. If backend itself fails, compare the deployed FastAPI routes with [the repository's `main.py`](https://github.com/TonyStark1616/VGU-Smart-Locker/blob/main/backend/app/main.py). The source version accepts these WebSocket paths; the live behavior does not. The repository's Nginx file also includes upgrade headers, so source and deployment may differ.

Two source-level issues are worth checking before changing Tailscale:

1. [The Dockerfile](https://github.com/TonyStark1616/VGU-Smart-Locker/blob/main/backend/Dockerfile) starts **four** Uvicorn workers. Each worker runs startup code that creates an MQTT client with the same `client_id` (`sls-backend-` plus `APP_ENV`) in [mqtt_service.py](https://github.com/TonyStark1616/VGU-Smart-Locker/blob/main/backend/app/services/mqtt_service.py). MQTT permits one connection per client ID, so workers can kick each other off. Temporarily run one worker and recheck health to test this hypothesis. A durable design needs one broker consumer or coordinated consumers; merely changing all IDs could process sensor messages four times.
2. [docker-compose.yml](https://github.com/TonyStark1616/VGU-Smart-Locker/blob/main/docker-compose.yml) ends the MQTT healthcheck with `|| exit 0`, which reports success even when its authentication/subscription command fails. It also waits only for `service_started`, not broker readiness. Consequently `docker compose ps` can look healthy while API health says MQTT is disconnected.

Even after WebSocket handshakes work, [websocket_manager.py](https://github.com/TonyStark1616/VGU-Smart-Locker/blob/main/backend/app/services/websocket_manager.py) stores connections in each worker's memory and [notification_service.py](https://github.com/TonyStark1616/VGU-Smart-Locker/blob/main/backend/app/services/notification_service.py) sends only to its local manager. With four workers, an HTTP request handled by one worker cannot notify a socket held by another. A single worker is a useful short-term check; multiworker delivery needs a shared message channel.

These are evidence-ranked hypotheses, not host-side diagnoses. Tailscale's [Funnel documentation](https://tailscale.com/docs/reference/tailscale-cli/funnel) confirms the allowed public ports (443, 8443, 10000), the separate HTTPS/TCP forwarding modes, and `tailscale funnel status --json`. [Funnel and Serve cannot own the same port at once](https://tailscale.com/docs/features/tailscale-funnel); the most recent configuration wins. A connected machine and Funnel badge alone do not prove each port or upstream is working. The connection guide refers to `scripts/tailscale_expose.ps1`, but that script is absent from the public repository's `main` tree, so the actual host status is the source of truth for its current mappings.

## App actions compared with the earlier 21-path live API

The IT team will preserve the existing Google identity, receiver booking, cabinet session QR, and device-key cabinet flow. This table records the **earlier 21-path API**, before the 16:20 UTC partial deployment; its missing-route statements are historical. The current 30-path route audit is in the raw retest log and the current completion list is in the app contract. No door-opening call was sent to the live server.

| IT app or cabinet action | Current call | Live server counterpart | Gap to resolve |
| --- | --- | --- | --- |
| Google sign-in button | `POST /auth/google` with Google ID token | None | Server must validate the VGU Google token and return a receiver session, or both teams must explicitly change the identity design. |
| Phone/password sign-in button | `POST /auth/password-login` | `POST /auth/login` | Same input fields, but response uses `access_token` instead of `token`, expires after 30 minutes, and there is no refresh route. This can be adapted only with an agreed session policy. |
| SMS/code buttons still present in the UI | `POST /auth/request-code`, `/auth/verify-code` | `POST /auth/otp/request`, `/auth/otp/verify` | Live OTP verification returns `verified`, not a session. The IT contract removed this sign-in path, so the UI and contract need reconciliation; do not treat a verified OTP as login. |
| Account greeting/settings | `GET /me` | `GET /auth/me` | Data fields largely match; path differs. This is the closest direct adaptation once authentication works. |
| Change password | `POST /auth/set-password` | None | Server route needed for the existing Settings button. |
| Session refresh; logout | `POST /auth/refresh`, `/auth/logout` | None | App can clear its local token, but the live server offers no refresh or token revocation. |
| Home waiting parcels; history | `GET /parcels`, `/parcels/history` | `GET /transactions/my-transactions` | One paginated transaction list would need agreed waiting/history statuses and display field mapping. The current `GET /parcels` returned 404. |
| Home free-box count | `GET /cabinets` | `GET /lockers/{unit_code}` only | No receiver-facing locker list or per-size free counts. Hard-coding one unit would mislead the UI. |
| Scan to open a box | `POST /parcels/collect` with a `VGU1|cabinet|time|random` session code | `POST /transactions/verify-qr-pickup` with a parcel-specific JWT `qr_token` | Different QR producer, lifetime, and pickup rule. A path rename would make the scanner reject every live QR and change which parcel can open. Agree the QR contract first. |
| Push notification registration | `POST /devices` with a Firebase device token | None; user WebSocket path exists but currently returns 403 | A WebSocket is not a replacement for an idle phone's push notice. Server route and delivery path needed. |
| Prepared booking, cancel, phone correction | `POST/GET/DELETE /bookings`, `PUT /me/phone` | None | These routes and their reserved-box rules need server support before booking UI can function. |
| Cabinet QR display | `GET /cabinet/session` with a device key | None | Live server generates a parcel-specific QR during delivery, not a refreshing cabinet session. |
| Cabinet Find and confirm-name buttons | `GET /cabinet/receiver`, `POST /cabinet/confirm-name` | None | Live API lacks masked-name lookup and the typo-recovery ladder. |
| Cabinet open/drop button | `POST /cabinet/drop` with device key and `receiver_ref` | `POST /transactions/deliver` with shipper JWT and recipient phone | Authentication and allocation rules differ; kiosk cannot safely call the live route by translating field names. |
| Cabinet door-closed button | `POST /cabinet/door-closed` | `POST /lockers/sensor-update` | A manual door-closed report with drop/collect purpose is not a sensor state update. |
| Cabinet command agent, fault, typed backup code, free-box list | `/cabinet/commands`, `/cabinet/command-done`, `/cabinet/fault`, `/cabinet/collect-by-code`, `/cabinet/free` | None with the same semantics | Device protocol routes or a written replacement design needed. Guest PIN endpoints are a separate mode. |
| Shared settings fetch | `GET /settings` | None | App keeps shipped settings on fetch failure, but cannot receive server corrections. |
| Type-code screen and Home map button | No server call in the current UI | Not applicable | These are local placeholders; they must not be counted as connected actions. |
| Tabs, theme, language, back, and local screen navigation | No server call | Not applicable | These already work locally and need no API mapping. |

The shipped `config/settings.json` deliberately leaves `server_base_url` blank. The repository's `config/README.md` says deployment sets the URL and it must not be committed. Setting it to the Funnel host now would make existing buttons call missing or incompatible routes. The existing [API contract](../reference/api-contract.md) is the concrete route/field proposal for the Server team to accept or negotiate. After that agreement and fixes above, build a targeted app and verify sign-in, listing, notification, cabinet flow, and pickup on a real phone and cabinet, using server logs to confirm door actions.
