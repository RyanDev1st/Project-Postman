"""Draw the launcher icon from the VGU parcel-locker mark. Run it when the mark changes.

    python scripts/appicon.py

Reads `scripts/appicon-logo.png` and writes every size Android asks for,
plus one 512x512 for a store listing. Needs Pillow.

**Why a script and not twelve hand-cut PNGs.** Twelve files that must agree
about a margin will stop agreeing the first time one of them is redrawn. The
margins here are one number each, written down once and applied to all.

The mark is wider than it is tall, so the number that matters is its WIDTH
against the canvas, never its height.
"""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("needs Pillow:  pip install Pillow")

ROOT = Path(__file__).resolve().parent.parent
SOURCE = Path(__file__).resolve().parent / "appicon-logo.png"
RES = ROOT / "src/app/src/main/res"

WHITE = (255, 255, 255, 255)

# Density buckets, as a multiple of mdpi.
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# An adaptive icon is 108dp and the launcher may mask it to a circle of 66dp.
# At 52% the mark is 56dp wide. That clears the circle, and it also leaves
# the mark the same amount of air as the phone's own icons: measured
# against Settings in a launcher screenshot, 60% looked like a sticker
# pressed against the edge of its tile.
ADAPTIVE_DP = 108
ADAPTIVE_WIDTH = 0.52

# The old square icon is not masked to a circle on the phones that still use
# it, so the mark can be bigger. Anything larger than this starts to look
# like a sticker with no card under it.
LEGACY_DP = 48
LEGACY_WIDTH = 0.62

# The round one is masked, so it goes back to the adaptive number.
ROUND_WIDTH = 0.52

STORE_PX = 512
STORE_WIDTH = 0.62


def mark() -> Image.Image:
    """The mark, cropped to itself.

    The file is a mark floating in a wide transparent field. Cropping to the
    pixels that are actually drawn is what makes every margin below mean the
    same thing - otherwise the margin is measured against whitespace somebody
    happened to leave in an export.
    """
    art = Image.open(SOURCE).convert("RGBA")
    box = art.getchannel("A").getbbox()
    if box is None:
        sys.exit(f"{SOURCE} is empty")
    return art.crop(box)


def draw(size: int, width: float, background: tuple | None) -> Image.Image:
    """One square icon: the mark centred, at [width] of the canvas."""
    art = mark()
    w = round(size * width)
    h = round(w * art.height / art.width)
    art = art.resize((w, h), Image.LANCZOS)

    canvas = Image.new("RGBA", (size, size), background or (0, 0, 0, 0))
    canvas.alpha_composite(art, ((size - w) // 2, (size - h) // 2))
    return canvas


def write(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
    print(f"  {path.relative_to(ROOT)}  {image.width}x{image.height}")


def main() -> None:
    if not SOURCE.exists():
        sys.exit(f"no mark at {SOURCE}")

    for bucket, scale in DENSITIES.items():
        out = RES / f"mipmap-{bucket}"
        # The foreground layer carries no background of its own. The white
        # is a colour resource behind it, so the launcher can shift the two
        # layers apart without a white rectangle sliding over the wallpaper.
        write(
            draw(round(ADAPTIVE_DP * scale), ADAPTIVE_WIDTH, None),
            out / "ic_launcher_foreground.png",
        )
        write(draw(round(LEGACY_DP * scale), LEGACY_WIDTH, WHITE), out / "ic_launcher.png")
        write(draw(round(LEGACY_DP * scale), ROUND_WIDTH, WHITE), out / "ic_launcher_round.png")

    # Not in res/. A store icon is not part of the app and would be shipped
    # inside every APK for nothing.
    write(draw(STORE_PX, STORE_WIDTH, WHITE), ROOT / "build/store-icon-512.png")


if __name__ == "__main__":
    main()
