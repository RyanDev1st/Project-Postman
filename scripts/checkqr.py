"""Read the cabinet's QR back with a decoder that knows nothing about us.

    python scripts/checkqr.py

Exit code 0 means clean. Anything else means read the output.

WHY BOTH HALVES

`scripts/checkqr.js` proves the shipped `qr.js` builds the right payload. It
cannot prove the picture is readable - it never draws one, and an encoder that
agreed with itself would pass.

This half draws the code with the vendored encoder and reads it back with
`zxing-cpp`: an unrelated library, and the same engine family a phone camera
uses. If the string that comes out is the string that went in, the screen
works for the only reader that matters.

WHY NOT A MATRIX COMPARISON

Comparing our matrix against another encoder's was tried and thrown away. Two
encoders disagree about how to split a string into segments, which is a
choice the standard leaves open, so two valid codes for one string need not be
the same picture. They differed by 154 modules and both were correct.
Decoding is the only test that means anything.

zxing-cpp is not vendored - it is a check-time tool, not shipped code. Without
it this script says so and stops rather than passing quietly.
"""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VENDOR = ROOT / "src/cabinet/vendor/qrcode.js"

# What a cabinet actually puts on screen, plus the shapes that stress the
# encoder: a longer id, and the biggest moment a 32-bit clock will ever show.
PAYLOADS = [
    "VGU1|vgu-test-01|1755158400",
    "VGU1|vgu-back-gate-02|1755158430",
    "VGU1|vgu-library-ground-floor-north|2147483647",
]

ENCODE_JS = """
const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
// Browser bundle: `var QRCode = function(t){...}(...)`. Evaluate it and hand
// back the global it declares. Local name differs so it cannot collide.
const lib = eval(src + '\\n;QRCode');
const qr = lib.create(process.argv[3], { errorCorrectionLevel: 'M' });
const size = qr.modules.size, d = qr.modules.data, rows = [];
for (let y = 0; y < size; y++) {
  let r = '';
  for (let x = 0; x < size; x++) r += d[y * size + x] ? '1' : '0';
  rows.push(r);
}
console.log(JSON.stringify({ version: qr.version, size, rows }));
"""


def main() -> int:
    try:
        from PIL import Image
        import zxingcpp
    except ImportError as missing:
        print(f"cannot run: {missing.name} is not installed")
        print("  python -m pip install zxing-cpp pillow")
        print("\nThis is a check-time tool, not shipped code. Nothing is broken;")
        print("the check simply did not run.")
        return 2

    if not VENDOR.exists():
        print(f"cannot run: {VENDOR.relative_to(ROOT).as_posix()} is missing")
        return 2

    helper = ROOT / "build" / "checkqr-encode.js"
    helper.parent.mkdir(parents=True, exist_ok=True)
    helper.write_text(ENCODE_JS, encoding="utf-8")

    problems = []
    for payload in PAYLOADS:
        out = subprocess.run(
            ["node", str(helper), str(VENDOR), payload],
            capture_output=True, text=True,
        )
        if out.returncode:
            problems.append(f"  {payload}\n      encoder failed: {out.stderr.strip()[:160]}")
            continue

        qr = json.loads(out.stdout)
        size, rows = qr["size"], qr["rows"]

        # A quiet zone is part of the code, not decoration. Without it a
        # decoder has no edge to find and the read fails on a real screen.
        quiet, scale = 4, 8
        side = (size + quiet * 2) * scale
        im = Image.new("L", (side, side), 255)
        px = im.load()
        for y, row in enumerate(rows):
            for x, cell in enumerate(row):
                if cell == "1":
                    for dy in range(scale):
                        for dx in range(scale):
                            px[(x + quiet) * scale + dx, (y + quiet) * scale + dy] = 0

        result = zxingcpp.read_barcode(im)
        got = result.text if result else None
        if got != payload:
            problems.append(f"  {payload}\n      read back as {got!r}")
        else:
            print(f"  v{qr['version']} {size}x{size}  reads back exactly  {payload}")

    helper.unlink(missing_ok=True)

    if problems:
        print("\nthe cabinet QR does not read back:")
        print("\n".join(problems))
        print("\nnot clean")
        return 1

    print(f"\ncabinet QR: {len(PAYLOADS)} codes drawn and read back by zxing-cpp, all exact")
    return 0


if __name__ == "__main__":
    sys.exit(main())
