"""Draw a crisp courier shipping label as a PNG with alpha.

Hunyuan bakes text as unreadable mush, so the label goes on as a decal instead.
Drawn, not generated: text has to be legible, and a renderer does that reliably.

The data is ours - box number and masked name - so the simulator shows the
same fields the real cabinet screen will. Never a full name or phone number.
"""
import random
from PIL import Image, ImageDraw, ImageFont

W, H = 1000, 640
random.seed(4)


def font(size, bold=False):
    for name in (("arialbd.ttf", "arial.ttf") if bold else ("arial.ttf",)):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def barcode(d, x, y, w, h, seed):
    rng = random.Random(seed)
    cur = x
    while cur < x + w - 4:
        bw = rng.choice((3, 3, 5, 8))
        if rng.random() < 0.62:
            d.rectangle([cur, y, cur + bw, y + h], fill=(0, 0, 0, 255))
        cur += bw + rng.choice((3, 4, 6))


def qr(d, x, y, size, seed):
    rng = random.Random(seed)
    n = 21
    c = size / n
    d.rectangle([x, y, x + size, y + size], fill=(255, 255, 255, 255))
    for r in range(n):
        for col in range(n):
            if rng.random() < 0.5:
                d.rectangle([x + col * c, y + r * c, x + (col + 1) * c, y + (r + 1) * c],
                            fill=(0, 0, 0, 255))
    # finder squares
    for fx, fy in ((0, 0), (n - 7, 0), (0, n - 7)):
        d.rectangle([x + fx * c, y + fy * c, x + (fx + 7) * c, y + (fy + 7) * c],
                    fill=(255, 255, 255, 255))
        d.rectangle([x + fx * c, y + fy * c, x + (fx + 7) * c, y + (fy + 7) * c],
                    outline=(0, 0, 0, 255), width=int(c))
        d.rectangle([x + (fx + 2) * c, y + (fy + 2) * c, x + (fx + 5) * c, y + (fy + 5) * c],
                    fill=(0, 0, 0, 255))


def make(path, courier="SPX Express", tracking="SPXVN0451882913",
         box="04", name="NGUYEN V** A**", cabinet="VGU-A1"):
    im = Image.new("RGBA", (W, H), (255, 255, 255, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, W - 1, H - 1], outline=(0, 0, 0, 255), width=4)

    # header band
    d.rectangle([4, 4, W - 5, 104], fill=(0, 0, 0, 255))
    d.text((26, 30), courier, font=font(56, True), fill=(255, 255, 255, 255))
    d.text((W - 250, 40), "STANDARD", font=font(30, True), fill=(255, 255, 255, 255))

    # recipient block - masked, the cabinet screen is a public terminal
    d.text((26, 128), "TO", font=font(24, True), fill=(90, 90, 90, 255))
    d.text((26, 158), name, font=font(46, True), fill=(0, 0, 0, 255))
    d.text((26, 214), f"VGU Smart Locker  {cabinet}", font=font(28), fill=(0, 0, 0, 255))

    # big box number - the one thing a courier must not misread
    d.rectangle([W - 250, 124, W - 26, 296], outline=(0, 0, 0, 255), width=4)
    d.text((W - 236, 130), "BOX", font=font(26, True), fill=(90, 90, 90, 255))
    d.text((W - 232, 158), box, font=font(112, True), fill=(0, 0, 0, 255))

    d.line([16, 316, W - 16, 316], fill=(0, 0, 0, 255), width=3)

    qr(d, 26, 336, 168, seed=7)
    barcode(d, 232, 350, W - 262, 130, seed=11)
    d.text((232, 496), tracking, font=font(40, True), fill=(0, 0, 0, 255))
    d.text((232, 548), "Scan the cabinet screen to collect",
           font=font(26), fill=(70, 70, 70, 255))

    im.save(path)
    print("wrote", path, im.size)
    return path


# One label per parcel prop. Box numbers are real slots in GRID.
PARCELS = [
    ("PolyMailer", "SPX Express", "SPXVN0451882913", "04", "NGUYEN V** A**"),
    ("ShopeeBox",  "Shopee",      "SPEVN7741029556", "11", "TRAN T** B**"),
    ("BlackWrap",  "SPX Express", "SPXVN9930274158", "07", "LE M** C**"),
    ("Tube",       "Shopee",      "SPEVN2216840397", "18", "PHAM Q** D**"),
    ("YellowBag",  "SPX Express", "SPXVN5583109624", "13", "HOANG T** E**"),
    ("BubbleWrap", "Shopee",      "SPEVN6674312085", "20", "VU N** F**"),
]


def make_all(out_dir):
    """Render one label per parcel. Returns {parcel name: png path}."""
    import os
    os.makedirs(out_dir, exist_ok=True)
    paths = {}
    for name, courier, tracking, box, who in PARCELS:
        path = os.path.join(out_dir, f"label_{name}.png")
        make(path, courier=courier, tracking=tracking, box=box, name=who)
        paths[name] = path
    return paths


if __name__ == "__main__":
    import sys
    make_all(sys.argv[1] if len(sys.argv) > 1 else "props")
