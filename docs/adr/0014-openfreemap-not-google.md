# 0014 — OpenFreeMap and MapLibre, not Google

- **Status:** accepted
- **Date:** 2026-08-12
- **Deciders:** Ryan
- **Supersedes:** [0013](0013-a-real-map-and-its-key.md)

## Context

[ADR 0013](0013-a-real-map-and-its-key.md) chose Google's Maps SDK, on the
evidence that the native Android SKU is billed at nothing — `Maps SDK
6DE1-4D9C-5B67`, free cap **Unlimited**, no price in any tier. That reading
was right and it was not enough. Google will not issue a usable key until
billing is enabled on the project, and the billing profile asked for a
**₫630,000 authorisation on a personal card** before it would serve the map it
charges nothing for.

Ryan's answer: *"yeah i aint gonna apply for it. lets go with openstreet map.
Anything that works, doesnt have to be google map."*

So the real requirement was never Google. It was: a map that shows where you
actually are, without a card.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| A — Google Maps SDK (ADR 0013) | Free at any volume; the map everyone knows | Needs a card on file. Rejected by the person whose card it is |
| B — OSM's own tile servers | Free, no key | Their policy forbids distributing an app that uses them. Not open to us |
| C — MapTiler / Stadia free tier | Good styles | Needs an API key and an account. Same problem in a smaller size |
| D — bundle tiles for the campus | No network, no key, no account | Only works standing on campus, and "one baked scene" is the fault being fixed |
| E — **OpenFreeMap + MapLibre** | No key, no account, no card, no request limit, and its FAQ allows apps and commercial use | One person's free service, with no SLA. Vector tiles need a GL renderer, which is ~40 MB of native libraries |

## Decision

We choose **E**. MapLibre Native draws the map; the tiles come from
OpenFreeMap's public instance, styles `dark` and `positron` for the app's two
schemes.

The route line stays baked, as in 0013 — a routing call is the one part of a
map nobody serves for free, and the gate does not move.

The drawn plan and its projected geometry are **deleted**, not kept as a
fallback. With no key there is no "key missing" state left to fall back from,
and a second map that only appears when the first fails is a screen nobody
would ever test. It is in the history at `cedc3b2`.

## Why

It removes the blocker rather than working around it: no key means no account,
no card, nothing for a future team member to be issued, and nothing that can
be leaked out of the APK. That last point deleted a whole section of 0013.

## Evidence

- OpenFreeMap's own FAQ: "completely free: there are no limits on the number
  of map views or requests", and "Is commercial usage allowed? Yes."
- All four style endpoints answer 200: `dark`, `positron`, `bright`,
  `liberty`.
- The tile covering the VGU back gate — `z14/13044/7683`, the source's
  maxzoom, overzoomed by the renderer above that — returns **14,906 bytes** of
  real vector data. The map has something to draw where the cabinet is.

## What we accept

- **No SLA.** OpenFreeMap is run by one person and says so. If it goes down,
  the Home card is blank. It is a map on a card, not the door-opening path, so
  nothing about collecting a parcel stops working.
- **Attribution is required** and stays switched on. Not ours to hide.
- **The APK grew.** `libmaplibre.so` is 8–11 MB per ABI, about 40 MB across
  the four, and the debug APK is now 55.6 MB. Real phones only ever need
  `arm64-v8a`; an ABI filter or a split would take most of it back, and that
  is a separate change nobody has asked for yet.
- **`ACCESS_WIFI_STATE`** arrives from MapLibre's manifest. It is granted
  without a prompt and it is how the renderer knows it is online. Left in.

## Affects

- Supersedes 0013 entirely, including its key-handling scheme. There is no
  `MAPS_API_KEY`, no `local.properties` entry, and no manifest placeholder.
- `ACCESS_FINE_LOCATION` is force-removed with `tools:node="remove"`. MapLibre
  asks for it; the app only needs COARSE, and the merger had granted the
  stronger one silently.
