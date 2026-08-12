# 0013 — A real map on Home, and where its key lives

- **Status:** accepted
- **Date:** 2026-08-12
- **Deciders:** Ryan

## Context

The Home card showed a map drawn at build time. The geometry was real — OSM
ways, and a real pedestrian route from Valhalla — but it was **baked**. It
drew the same picture of the same campus wherever the phone was, and the "you
are here" dot was a fixed point, not a position. Ryan's words: it "defaults to
that singular scene".

A real map needs a Maps API key, and the key has to reach the APK, because the
SDK reads it from the manifest and no server of ours sits in front of it. The
repo rule says no key in the repository. Those two facts have to be reconciled
rather than traded off.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| A — keep the baked plan | No key, no permission, no network, works offline | Shows one scene forever. This is the thing being fixed |
| B — Google Maps SDK, key in `local.properties` | The map Ryan asked for; display is not billed; key never enters git | Ryan must create the key; a clone without one needs a fallback |
| C — Google Maps SDK, key committed | Everyone can build it at once | A key in git is a key in git. Not open for discussion |
| D — OpenStreetMap raster tiles | Free, no key, no Google account | Ryan asked for Google; tile policy discourages app use; another library |

## Decision

We choose **B**, with **A kept as the fallback**.

The key is read from `local.properties`, which git already ignores, and
substituted into the manifest as `${mapsApiKey}` at build time. `BuildConfig
.MAPS_API_KEY` carries the same value so the app can ask whether it has one.
Blank — a fresh clone, or CI — the Home card draws the old plan instead.

The map is **lite mode**: one styled bitmap, not a GL surface. The card is
112dp tall inside a scrolling screen, and a full map there swallows the scroll
and spends a frame budget on something the size of a stamp.

The route line stays **baked**. Displaying a map is free; asking Google for
directions is the part that is billed. The gate does not move, so the walk is
computed once and shipped.

## Why

A key that must ship in the APK is not the same kind of secret as a server
token: it authorises nothing on its own, and it is restricted in the Google
Cloud console to this package name and this signing certificate, so a copy
lifted out of the APK does not work anywhere else. The rule the repo actually
needs to hold is **no key in the repository**, and `local.properties` holds
that line without a build server or a secrets manager the team does not have.

## What we accept

- A build made without a key silently shows the old plan. It is a worse map,
  not a broken screen, but it does mean two possible pictures on one card and
  a tester has to be told which they are looking at.
- The route is fixed. If the cabinet moves, someone edits `route.json` and
  the numbers in `LiveMap.kt`, and ships.
- Location is `ACCESS_COARSE_LOCATION` only. The blue dot is accurate to a few
  hundred metres, which answers "which way to the gate" and nothing more.
- The key is per-signing-certificate. The release build needs its own SHA-1
  added in the console, or the map goes blank on the day we ship.

## Affects

- Unblocks nothing on the board — the map is design work, not a P-task.
- Needs from Ryan: a Maps API key, restricted to package
  `vn.edu.vgu.smartlocker` and debug SHA-1
  `77:38:57:27:A1:6D:B6:55:5A:AB:43:D5:BA:DA:B9:AF:A0:0E:E6:0D`, pasted into
  `local.properties` as `MAPS_API_KEY=...`.
- Changes `docs/reference/architecture.md` — the app now has one Google
  dependency it did not have.
