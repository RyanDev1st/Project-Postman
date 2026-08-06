"""Draw a courier shipping label as a PNG, in the local style.

Hunyuan bakes text into the parcel texture as unreadable mush, so the label
goes on as a decal instead. Drawn, not generated: text has to stay legible.

The layout follows the SPX / Shopee stickers on real parcels here - logo top
left, thin rule lines for the address fields, one big barcode across the
bottom. Sparse and light, not a dense corporate block.

The fields are ours: box number, masked name, cabinet id. Never a full name or
a phone number - the cabinet screen is a public terminal.
"""
import os
import random

from PIL import Image, ImageDraw, ImageFont

W, H = 1000, 640
INK = (17, 17, 17, 255)
FAINT = (120, 120, 120, 255)


def font(size, bold=False):
    names = ("arialbd.ttf", "calibrib.ttf") if bold else ("arial.ttf", "calibri.ttf")
    for name in names:
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def _barcode(d, x, y, w, h, seed):
    """Code-128 look. Not scannable - a prop, not a real waybill."""
    rng = random.Random(seed)
    cur = x
    while cur < x + w - 5:
        bar = rng.choice((3, 3, 4, 7, 10))
        if rng.random() < 0.58:
            d.rectangle([cur, y, cur + bar, y + h], fill=INK)
        cur += bar + rng.choice((3, 4, 5, 8))


def _qr(d, x, y, size, seed):
    rng = random.Random(seed)
    n = 25
    c = size / n
    d.rectangle([x, y, x + size, y + size], fill=(255, 255, 255, 255))
    for r in range(n):
        for col in range(n):
            if rng.random() < 0.48:
                d.rectangle([x + col * c, y + r * c, x + (col + 1) * c, y + (r + 1) * c],
                            fill=INK)
    for fx, fy in ((0, 0), (n - 7, 0), (0, n - 7)):
        d.rectangle([x + fx * c, y + fy * c, x + (fx + 7) * c, y + (fy + 7) * c],
                    fill=(255, 255, 255, 255))
        d.rectangle([x + fx * c, y + fy * c, x + (fx + 7) * c, y + (fy + 7) * c],
                    outline=INK, width=max(2, int(c)))
        d.rectangle([x + (fx + 2) * c, y + (fy + 2) * c,
                     x + (fx + 5) * c, y + (fy + 5) * c], fill=INK)


def _shopee_mark(d, x, y, s):
    """The rounded black tile with the shopping-bag glyph."""
    d.rounded_rectangle([x, y, x + s, y + s], radius=s * 0.22, fill=INK)
    # bag body
    bx, by, bw, bh = x + s * 0.26, y + s * 0.38, s * 0.48, s * 0.40
    d.rounded_rectangle([bx, by, bx + bw, by + bh], radius=s * 0.06,
                        fill=(255, 255, 255, 255))
    # handle
    d.arc([x + s * 0.34, y + s * 0.20, x + s * 0.66, y + s * 0.52],
          start=180, end=360, fill=(255, 255, 255, 255), width=max(3, int(s * 0.07)))
    # the S
    d.text((x + s * 0.40, y + s * 0.44), "S", font=font(int(s * 0.30), True), fill=INK)


def _spx_mark(d, x, y, s):
    """SPX sets its name in heavy type rather than a picture mark."""
    d.text((x, y + s * 0.24), "SPX", font=font(int(s * 0.62), True), fill=INK)


def _rule(d, x, y, w, caption=None, value=None):
    """One address line: a faint rule with a small caption under it."""
    if value:
        d.text((x + 4, y - 34), value, font=font(30), fill=INK)
    d.line([x, y, x + w, y], fill=(60, 60, 60, 255), width=2)
    if caption:
        d.text((x, y + 6), caption, font=font(17), fill=FAINT)


def make(path, courier="SPX Express", tracking="SPXVN0451882913",
         box="04", name="NGUYEN V** A**", cabinet="VGU-A1"):
    im = Image.new("RGBA", (W, H), (255, 255, 255, 255))
    d = ImageDraw.Draw(im)

    is_shopee = courier.lower().startswith("shopee")
    if is_shopee:
        _shopee_mark(d, 44, 40, 88)
        d.text((150, 56), "Shopee", font=font(64, True), fill=INK)
    else:
        _spx_mark(d, 44, 34, 96)
        d.text((190, 62), "Express", font=font(44), fill=INK)

    # service word, top right - small, not a banner
    d.text((W - 214, 62), "STANDARD", font=font(26, True), fill=INK)

    # box number: the one field that must not be misread
    d.rectangle([W - 214, 108, W - 44, 236], outline=INK, width=3)
    d.text((W - 204, 116), "BOX", font=font(20, True), fill=FAINT)
    d.text((W - 198, 138), box, font=font(84, True), fill=INK)

    # address block - rule lines, the way the real sticker is laid out
    _rule(d, 44, 214, 620, caption="NGUOI NHAN / TO", value=name)
    _rule(d, 44, 288, 620, caption="DIA DIEM / PICKUP POINT",
          value=f"VGU Smart Locker  {cabinet}")

    _qr(d, 44, 336, 150, seed=hash(tracking) & 0xFFFF)

    _barcode(d, 226, 336, W - 270, 124, seed=hash(tracking + box) & 0xFFFF)
    d.text((226, 470), tracking, font=font(38, True), fill=INK)

    # Footer sits below both, clear of the QR block.
    d.text((44, 556), "Quet ma tren man hinh tu de nhan hang",
           font=font(20), fill=FAINT)
    d.text((44, 584), "Scan the cabinet screen to collect",
           font=font(20), fill=FAINT)

    im.save(path)
    print("wrote", path)
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
    os.makedirs(out_dir, exist_ok=True)
    return {name: make(os.path.join(out_dir, f"label_{name}.png"),
                       courier=courier, tracking=tracking, box=box, name=who)
            for name, courier, tracking, box, who in PARCELS}


if __name__ == "__main__":
    import sys
    make_all(sys.argv[1] if len(sys.argv) > 1 else "props")
