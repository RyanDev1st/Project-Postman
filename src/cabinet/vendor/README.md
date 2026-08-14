Parent: ../../../docs/reference/architecture.md

# Vendored code on the cabinet screen

Third-party files copied into the repo, with where each came from and why it
is a copy rather than a link. Nothing here is ours. Do not edit these files —
to change one, replace it and update this table.

## Why vendored and not linked

The cabinet screen has **no build step**. It is `<script src>` tags in one
HTML file, because nobody can say yet what machine it runs on ([ADR
0009](../../../docs/adr/0009-start-phase-1-early.md)).

It also may not reach a CDN. `scripts/checknet.py` fails if any file in
`src/cabinet/` except `net.js` touches the network, and a cabinet in a lobby
must draw the one thing a person walked up to it for whether or not the
internet is up.

## What is here

| File | Package | Version | Size | Licence |
| --- | --- | --- | --- | --- |
| `qrcode.js` | [node-qrcode](https://github.com/soldair/node-qrcode) | 1.5.1 | 23,468 bytes | MIT (`qrcode.LICENSE`) |

- **Source:** `https://cdn.jsdelivr.net/npm/qrcode@1.5.1/build/qrcode.js`
- **sha256:** `ba588dfaf738bf8980e5da3b680ab1ce3f205af7577454c16f9c0506fe744df4`
- **Fetched:** 2026-08-14

### Why this library

Checked on the day, not remembered. Against `davidshimjs/qrcodejs`, which has
the most stars in this category by a wide margin:

| | stars | npm downloads/week |
| --- | --- | --- |
| `qrcode` (node-qrcode) | 8,158 | **20,825,876** |
| `qrcodejs` | **14,301** | 10,394 |

The star leader is installed by roughly one project for every two thousand
that install this one. Stars record what was popular when a category was
young; downloads record what is being built with now.

### Why 1.5.1 and not 1.5.4

**1.5.4 is the newest release, and it has no browser build.** So do 1.5.3.
Both ship CommonJS modules only and need a bundler, which this screen does
not have. 1.5.1 is the last release carrying `build/qrcode.js`, a standalone
file that declares a `QRCode` global — which is what a `<script src>` tag
needs.

That pin is a real cost and it is written down here so the next person does
not spend an afternoon rediscovering it. If the screen ever gains a build
step, move to the current release and delete this note.

### What was checked before committing it

- **No network or `eval` surface.** Grepped for `XMLHttpRequest`, `WebSocket`,
  `fetch(`, `importScripts`, `eval(`, `new Function` — none present.
- **It encodes correctly.** Four payloads were encoded with this exact file,
  rendered to bitmaps, and read back with `zxing-cpp` — an unrelated decoder,
  and the same engine family a phone camera uses. All four decoded to the
  original string, byte for byte.

  A matrix-by-matrix comparison against `segno` was tried first and rejected:
  the two disagree because node-qrcode optimises its segment split, which is
  a legal choice an encoder is free to make. Two valid QR codes for one
  string need not be the same picture. Decoding is the test that means
  anything.
