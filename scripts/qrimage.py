"""Draw a cabinet QR, using the encoder the cabinet screen actually ships.

Not a check on its own. `checkqr.py` reads what this draws back with an
unrelated decoder, and `checkscan.py` puts what this draws in front of the
app's camera. Both need the same picture, and a second encoder would mean two
pictures that must agree and one day will not.

Needs `node` and Pillow. Neither ships; both are check-time tools.
"""

from __future__ import annotations

import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VENDOR = ROOT / "src/cabinet/vendor/qrcode.js"

# The vendored file is a browser bundle - `var QRCode = function(t){...}(...)`.
# Evaluating it and naming the result something else is the whole trick: a
# local `const QRCode` would collide with the `var` the bundle declares.
ENCODE_JS = """
const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
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


def modules(payload: str) -> dict:
    """The black-and-white grid, straight out of the cabinet's own encoder."""
    helper = ROOT / "build" / "qrimage-encode.js"
    helper.parent.mkdir(parents=True, exist_ok=True)
    helper.write_text(ENCODE_JS, encoding="utf-8")
    try:
        out = subprocess.run(
            ["node", str(helper), str(VENDOR), payload],
            capture_output=True, text=True,
        )
    finally:
        helper.unlink(missing_ok=True)
    if out.returncode:
        raise RuntimeError(f"the cabinet encoder failed: {out.stderr.strip()[:200]}")
    return json.loads(out.stdout)


def draw(payload: str, scale: int = 8, quiet: int = 4):
    """One code, as a greyscale image.

    The quiet zone is part of the code, not decoration. Without it a decoder
    has no edge to find and the read fails - on a screen as surely as on paper.
    """
    from PIL import Image

    qr = modules(payload)
    size, rows = qr["size"], qr["rows"]
    side = (size + quiet * 2) * scale
    im = Image.new("L", (side, side), 255)
    px = im.load()
    for y, row in enumerate(rows):
        for x, cell in enumerate(row):
            if cell == "1":
                for dy in range(scale):
                    for dx in range(scale):
                        px[(x + quiet) * scale + dx, (y + quiet) * scale + dy] = 0
    return im, qr["version"]


def tiled(code, width: int, height: int, across: int = 3):
    """The same code, repeated across a camera frame.

    The emulated camera crops and turns whatever image it is given, and does
    not say by how much. A single code centred on the frame came back with a
    finder pattern off the edge twice, at two different sizes. Repeating it
    means a whole one lands inside the crop wherever the crop is.
    """
    from PIL import Image

    frame = Image.new("L", (width, height), 255)
    side = code.width
    for row in range(across):
        for col in range(across):
            frame.paste(
                code,
                ((width - side) * col // (across - 1),
                 (height - side) * row // (across - 1)),
            )
    return frame


if __name__ == "__main__":
    # Draw one code to a file, for a person to point a phone at.
    #
    #   python scripts/qrimage.py "VGU1|vgu-back-gate|1786690000" out.png [scale]
    #
    # Any string works, including one that is not ours - which is how the
    # "a stranger's QR is ignored" case gets something to be ignored.
    import sys

    if len(sys.argv) < 3:
        print(__doc__)
        print('  python scripts/qrimage.py "<payload>" <out.png> [scale]')
        raise SystemExit(2)
    scale = int(sys.argv[3]) if len(sys.argv) > 3 else 12
    image, version = draw(sys.argv[1], scale=scale, quiet=4)
    image.convert("RGB").save(sys.argv[2])
    print(f"v{version}  {image.width}x{image.width}px  {sys.argv[2]}  {sys.argv[1]}")
