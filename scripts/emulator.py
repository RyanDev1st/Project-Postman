"""Drive an Android emulator and read what is on its screen.

Not a check. `checkscan.py` is the check; this is the half that knows about
adb, and it was split out when the two together went over the 300-line cap.

Set `ANDROID_HOME` if the SDK is not on the A: drive, and `CHECKSCAN_AVD` to
use an AVD other than `parity`.
"""

from __future__ import annotations

import io
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

SDK = Path(os.environ.get("ANDROID_HOME", "A:/Android/Sdk"))
ADB = str(SDK / "platform-tools/adb.exe")
EMULATOR = str(SDK / "emulator/emulator.exe")
AVD = os.environ.get("CHECKSCAN_AVD", "parity")

# -no-window runs the emulator as the "-headless" binary. Killing only the
# other name kills nothing, the old emulator survives, and the next run
# silently reads the previous run's camera image. That happened once.
QEMU_NAMES = ("qemu-system-x86_64-headless.exe", "qemu-system-x86_64.exe",
              "emulator.exe")


def say(msg):
    # The app's own words go through here, and some of them are Vietnamese. A
    # Windows console is cp1252 by default and raises on the first accent,
    # which would kill a run over a log line.
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def adb(*args, timeout=180):
    return subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout)


def sh(cmd, timeout=180):
    return adb("shell", cmd, timeout=timeout).stdout


def labels():
    """Every on-screen label, with where to tap it."""
    adb("shell", "uiautomator dump /sdcard/ui.xml")
    xml = adb("shell", "cat /sdcard/ui.xml").stdout
    start = xml.find("<?xml")
    if start < 0:
        return []
    try:
        root = ET.fromstring(xml[start:])
    except ET.ParseError:
        return []
    found = []
    for node in root.iter("node"):
        text, desc = node.get("text", ""), node.get("content-desc", "")
        if not text and not desc:
            continue
        box = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if box:
            x1, y1, x2, y2 = map(int, box.groups())
            found.append((text, desc, (x1 + x2) // 2, (y1 + y2) // 2))
    return found


def on_screen():
    """Everything on screen as one lowercased string, for matching against."""
    return " ".join(f"{t} {d}" for t, d, _, _ in labels()).lower()


def tap(pattern, what, timeout=25):
    """Tap the first label matching [pattern]. False if it never appears."""
    rx = re.compile(pattern, re.I)
    deadline = time.time() + timeout
    while time.time() < deadline:
        for text, desc, x, y in labels():
            if rx.search(text) or rx.search(desc):
                adb("shell", f"input tap {x} {y}")
                say(f"tapped {what}: {(text or desc)!r}")
                return True
        time.sleep(1.5)
    say(f"NOT FOUND: {what}")
    return False


def type_text(text):
    adb("shell", f"input text {text}")
    time.sleep(1)
    adb("shell", "input keyevent 111")  # escape, to drop the keyboard
    time.sleep(1)


def dark_patch(fraction=0.34) -> bool:
    """Whether the middle of the screen is essentially black.

    Used to tell "the camera is delivering nothing" from "the app did not act".
    The scan screen's viewfinder is drawn near-black on purpose, so that the
    moment before the first frame is not a white flash - which makes those two
    faults look identical, and a run that blames the wrong half is worse than
    no run at all.
    """
    from PIL import Image, ImageStat

    shot = subprocess.run([ADB, "exec-out", "screencap", "-p"],
                          capture_output=True).stdout
    if not shot:
        return False
    im = Image.open(io.BytesIO(shot)).convert("L")
    w, h = im.size
    patch = im.crop((int(w * fraction), int(h * fraction),
                     int(w * (1 - fraction)), h // 2))
    return ImageStat.Stat(patch).mean[0] < 24


def boot(camera_image: Path | None = None) -> bool:
    """A cold emulator, optionally with a picture as its back camera."""
    for name in QEMU_NAMES:
        subprocess.run(["taskkill", "/F", "/IM", name], capture_output=True)
    subprocess.run([ADB, "kill-server"], capture_output=True)
    time.sleep(6)
    if "emulator-" in adb("devices", timeout=30).stdout:
        say("an emulator is still attached; not starting a second one")
        return False

    args = [EMULATOR, "-avd", AVD, "-gpu", "swiftshader_indirect", "-no-window",
            "-no-snapshot"]
    if camera_image is not None:
        # `imagefile:` makes the back camera render this picture, frame after
        # frame. The virtual scene was tried first: it hangs the code on a wall
        # the scene's camera never turns towards, and there is no flag to aim
        # it. The path must have no space in it - see checkscan.py.
        args += ["-camera-back", f"imagefile:{camera_image}"]

    say(f"starting {AVD}" + (" with a picture as its camera" if camera_image else ""))
    subprocess.Popen(args, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    adb("wait-for-device", timeout=600)
    while sh("getprop sys.boot_completed").strip() != "1":
        time.sleep(3)
    sh("wm dismiss-keyguard")
    time.sleep(3)
    say("booted")
    return True


def install(apk: Path, component: str):
    """Install and launch. `-g` grants the permissions the app asks for."""
    say(adb("install", "-r", "-g", str(apk), timeout=600).stdout.strip()[-40:])
    sh(f"am start -n {component}")
    time.sleep(6)
