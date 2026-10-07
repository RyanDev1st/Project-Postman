# Settings

`settings.json` holds every number we guessed. One file. Both front-ends read it.

What each setting means and when to change it is the **settings table** in [architecture.md](../docs/reference/architecture.md).

## Editing it

- Change values, never key names. A renamed key falls back to the built-in default without saying so.
- Units are in the key names. Numbers only — never `"48h"`.
- Raise `settings_version` by one on every change.

## A number is a setting. A shape is not

`60` → `120` is an edit here. *"The code works once"* → *"twice"* needs an ADR.

`pickup_code_digits` looks like a plain number and is not. Six digits is safe only while `wrong_tries_before_lock` stops all-night guessing. Read [ADR 0004](../docs/adr/0004-offline-pickup.md) before touching either.

## `server_base_url` is empty on purpose

The real address is set where the app is deployed, never in this repo. It must start with `https://` and include the API prefix, for example `https://locker.example/api/v1`; the app appends paths such as `/parcels`. See the transport row in [api-contract.md](../docs/reference/api-contract.md).

## Reaching a phone — not built yet

Task **P1-08**. Design: this file ships as the fallback, the app fetches the live copy on start, higher `settings_version` wins, and a failed fetch keeps the last good copy rather than reverting to the shipped one.

Until P1-08, a change here needs a new release. **The numbers are frozen at whatever shipped.**
