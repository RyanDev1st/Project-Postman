# scripts

Build, release and helper scripts.

## Rules

1. Every script has a one-line comment at the top saying what it does and when to run it.
2. A script never contains a key, token or password. It reads them from the environment.
3. Add a row here when you add a script.

| Script | What it does | When to run it |
| --- | --- | --- |
| `checkboard.py` | Reads every roadmap file and checks the counts, the IDs and the dependency links agree | Every time you tick a task, before you commit. It exits non-zero if the board is wrong |
| `checknet.py` | Checks each front-end talks to the server through one door, and holds no secret | Before you commit anything that touches the network. Exceptions carry an ADR and are printed on every run |
| `checkqr.js` | Runs the cabinet's shipped `qr.js` in a stub browser and checks the payload, the timers, and that a cabinet with no id draws nothing | When you change the cabinet screen's QR |
| `checkqr.py` | Draws the same code and reads it back with `zxing-cpp`, a decoder that knows nothing about us | With `checkqr.js`. Needs `zxing-cpp` and Pillow; says so and stops if they are missing |
| `checkserver.py` | Starts the real server over real TLS and walks a parcel from a shipper to a receiver, then tries to steal it four ways. 29 checks | Before you commit anything that touches the server. It verifies the certificate properly, which is how it caught a key size Android refuses |
| `checkscan.py` | Puts a live cabinet code in front of the app's camera on an emulator, and checks the app says it *could not tell* rather than showing a door that never opened | When you change the scanner. **Not** P5-02's Verify — there is no lens and no cabinet, and that check needs a real phone |
| `emulator.py` | Boots an emulator, taps things, and reads what is on the screen. Not a check | Never directly. `checkscan.py` uses it |
| `qrimage.py` | Draws a QR with the encoder the cabinet actually ships. Also a command: `python scripts/qrimage.py "<payload>" out.png` | `checkqr.py` and `checkscan.py` both use it, so both draw the same picture. Run it directly to make a code for a real phone to scan |
| `appicon.py` | Draws every launcher icon size from `appicon-logo.png`, plus a 512x512 for the store listing | When the mark changes. Never edit one of the generated PNGs by hand - the next run overwrites it |
| `checktest.py` | Eight checks that a real-phone test can even start: the certificate names this laptop's address, the build points at it, the server answers, and the browser is allowed to call it | Right before a phone test, and any time the laptop joins a different wifi |
| `checkloop.py` | The whole parcel journey against the real endpoints - register, drop, work the cabinet, list, scan, collect - with no phone and no cabinet in the room | Before and after any change to the server. It is the fastest way to find out whether the rules still hold |
| `stress.py` | How much the server carries: throughput and latency at 50, 200 and 500 requests in flight. `--writes` hammers the write path instead | When a change could touch how much load it takes. **Hold the connection open** - a fresh one per request measures TLS, not the server |
| `testbuild.py` | Writes the server address a real phone can reach into `config/settings.json`, and prints the build and upload commands | Before sending a build to testers, and every time the laptop joins a different wifi |
| `cabinet-agent.py` | Stands in for the cabinet's ESP32: polls for open commands, says it worked, and waits for a person to confirm the door shut | When you want the whole loop on one laptop. **Not evidence a door moved** - it prints what a cabinet would do, it does not move metal |
| `cabinet-sim/` | Builds the Blender cabinet and opens its doors. **Never evidence** — [ADR 0008](../docs/adr/0008-cabinet-simulator.md) | When you need to see the cabinet without having one |
