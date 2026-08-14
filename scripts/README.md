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
| `checkscan.py` | Puts a live cabinet code in front of the app's camera on an emulator and watches it read one, refuse a stale one, and open the door screen | When you change the scanner. **Not** P5-02's Verify — there is no lens and no cabinet, and that check needs a real phone |
| `qrimage.py` | Draws a QR with the encoder the cabinet actually ships. Not a check on its own | Never directly. `checkqr.py` and `checkscan.py` both use it, so both draw the same picture |
| `cabinet-sim/` | Builds the Blender cabinet and opens its doors. **Never evidence** — [ADR 0008](../docs/adr/0008-cabinet-simulator.md) | When you need to see the cabinet without having one |
