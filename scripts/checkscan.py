"""Show the app a live cabinet code and watch what it does.

    python scripts/checkscan.py

Exit code 0 means the app read a code and moved on. Anything else means read
the output.

WHAT THIS IS FOR

P5-02's Verify is a real phone reading a real cabinet screen, in daylight and
in a dark corridor, and no machine here can stand in for that. What a machine
*can* do is run the rest of the loop: a real camera stream, the real ML Kit
detector, the real rules in `pickup/SessionCode.kt`, and the real screen change
at the end of it. That is what this drives.

HOW THE TIMING WORKS

Nothing here sets a clock or waits on a stopwatch. The code is dated a few
minutes ahead, so the app first reads it and refuses it - a code from the
future is not a code this cabinet issued a moment ago - and keeps looking. The
instant the clock reaches it, the same picture becomes a live code and the
screen moves on. The app's own freshness rule picks the moment, which is also
the point: a run that passes has exercised the refusal as well as the read.

WHAT IT DOES NOT PROVE

No lens, no glass, no lit screen, no corridor. The camera is handed a picture
file. See `docs/roadmap/phase-5-pickup.md`.

REQUIREMENTS

An emulator AVD, `node`, Pillow and zxing-cpp. Set `ANDROID_HOME` if the SDK
is not on the A: drive. The app must build for x86_64, so the APK is the
`-Pemulator` one - build it first.
"""

from __future__ import annotations

import io
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import qrimage

ROOT = qrimage.ROOT
SDK = Path(os.environ.get("ANDROID_HOME", "A:/Android/Sdk"))
ADB = str(SDK / "platform-tools/adb.exe")
EMULATOR = str(SDK / "emulator/emulator.exe")
APK = ROOT / "src/app/build/outputs/apk/debug/app-debug.apk"

# Written twice, on purpose. The copy under build/ is for a person to look at
# when a run fails. The copy the emulator is handed must sit at a path with no
# space in it: this repo lives in "Project Postman", and given that path the
# emulated camera opened, created its capture session, and then delivered
# nothing but black - no error anywhere, in the log or on screen.
FRAME = ROOT / "build" / "checkscan-frame.png"
CAMERA_FRAME = Path(tempfile.gettempdir()) / "checkscan-frame.png"

AVD = os.environ.get("CHECKSCAN_AVD", "parity")
CABINET = "vgu-test-01"

# Long enough to cold boot, install, sign in and reach the scan screen, with
# room to spare. The code is live for 90 seconds after this, so overshooting
# costs nothing and undershooting costs a whole run.
SETUP_SECONDS = 260

# -no-window runs the emulator as the "-headless" binary. Killing only the
# other name kills nothing, the old emulator survives, and the next run
# silently reads the previous run's camera image. That happened once.
QEMU_NAMES = ("qemu-system-x86_64-headless.exe", "qemu-system-x86_64.exe",
              "emulator.exe")


def adb(*args, timeout=180):
    return subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout)


def sh(cmd, timeout=180):
    return adb("shell", cmd, timeout=timeout).stdout


def say(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


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


def tap(pattern, what, timeout=25):
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


def on_screen():
    return " ".join(f"{t} {d}" for t, d, _, _ in labels()).lower()


def viewfinder_is_black() -> bool:
    """Whether the camera is delivering anything at all.

    The well behind the corners is drawn near-black on purpose, so that the
    moment before the first frame is not a white flash. That makes "no frames"
    and "a very dark room" look identical on screen - and a run that reports
    "the app never acted on the code" when the camera was dark the whole time
    is blaming the wrong half. This reads the middle of the viewfinder: the
    code fills it when frames are arriving, so anything this dark is no frames.
    """
    from PIL import Image, ImageStat

    shot = subprocess.run([ADB, "exec-out", "screencap", "-p"],
                          capture_output=True).stdout
    if not shot:
        return False
    im = Image.open(io.BytesIO(shot)).convert("L")
    w, h = im.size
    patch = im.crop((w // 3, h // 3, w * 2 // 3, h // 2))
    return ImageStat.Stat(patch).mean[0] < 24


def make_frame(payload: str) -> bool:
    """The picture the camera will be given, checked before it is used."""
    import zxingcpp

    code, version = qrimage.draw(payload, scale=9, quiet=6)
    read = zxingcpp.read_barcode(code)
    if not read or read.text != payload:
        say(f"the code does not decode before it is even shown: {read and read.text!r}")
        return False
    FRAME.parent.mkdir(parents=True, exist_ok=True)
    qrimage.tiled(code, 1280, 1280).convert("RGB").save(FRAME)
    shutil.copyfile(FRAME, CAMERA_FRAME)
    if " " in str(CAMERA_FRAME):
        say(f"the camera path still has a space in it: {CAMERA_FRAME}")
        return False
    say(f"camera picture: v{version}, {code.width}px codes tiled 3x3, reads back exactly")
    return True


def start_emulator() -> bool:
    for name in QEMU_NAMES:
        subprocess.run(["taskkill", "/F", "/IM", name], capture_output=True)
    subprocess.run([ADB, "kill-server"], capture_output=True)
    time.sleep(6)
    if "emulator-" in adb("devices", timeout=30).stdout:
        say("an emulator is still attached; not starting a second one")
        return False

    # `imagefile:` makes the back camera render this picture, frame after
    # frame. The virtual scene was tried first: it hangs the code on a wall the
    # scene's camera never turns towards, and there is no flag to aim it.
    say(f"starting {AVD} with the code as the camera's picture")
    subprocess.Popen(
        [EMULATOR, "-avd", AVD, "-gpu", "swiftshader_indirect", "-no-window",
         "-no-snapshot", "-camera-back", f"imagefile:{CAMERA_FRAME}"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    adb("wait-for-device", timeout=600)
    while sh("getprop sys.boot_completed").strip() != "1":
        time.sleep(3)
    sh("wm dismiss-keyguard")
    time.sleep(3)
    say("booted")
    return True


def sign_in():
    """The demo build takes any six digits - there is no server yet."""
    adb("shell", "input tap 390 1088")
    time.sleep(1)
    adb("shell", "input text 912345678")
    time.sleep(1)
    adb("shell", "input keyevent 111")
    time.sleep(1)
    if not tap(r"send code|gửi mã", "send code"):
        return False
    time.sleep(3)
    adb("shell", "input text 123456")
    time.sleep(1)
    if not tap(r"continue|tiếp tục", "continue"):
        return False
    time.sleep(4)
    return True


def main() -> int:
    if not APK.exists():
        print(f"cannot run: {APK.relative_to(ROOT).as_posix()} is missing")
        print("  ./gradlew :app:assembleDebug -Pemulator")
        return 2
    with zipfile.ZipFile(APK) as apk:
        if not any(n.startswith("lib/x86_64/") for n in apk.namelist()):
            print("cannot run: the APK has no x86_64 libraries, so no emulator")
            print("  ./gradlew :app:assembleDebug -Pemulator")
            return 2
    try:
        import zxingcpp  # noqa: F401
        from PIL import Image  # noqa: F401
    except ImportError as missing:
        print(f"cannot run: {missing.name} is not installed")
        print("  python -m pip install zxing-cpp pillow")
        return 2

    epoch = int(time.time()) + SETUP_SECONDS
    payload = f"VGU1|{CABINET}|{epoch}"
    say(f"code {payload} - live from {epoch - 15} to {epoch + 75}")

    if not make_frame(payload) or not start_emulator():
        return 1

    say(adb("install", "-r", "-g", str(APK), timeout=600).stdout.strip()[-40:])
    sh(f"am start -n vn.edu.vgu.smartlocker/.MainActivity")
    time.sleep(6)
    if not sign_in() or not tap(r"scan a cabinet|quét tủ", "the scan button"):
        return 1
    time.sleep(4)

    seen = on_screen()
    if "no camera" in seen or "không có máy ảnh" in seen:
        say("the camera was refused, so this run proves nothing about reading")
        return 1
    if "scan the cabinet" not in seen and "quét màn hình tủ" not in seen:
        say("never reached the scan screen")
        return 1
    if viewfinder_is_black():
        say("on the scan screen, and the viewfinder is black - no frames are")
        say("arriving, so nothing below would be about the app at all")
        return 1
    say(f"on the scan screen and frames are arriving, {epoch - 15 - int(time.time())}s "
        "until the code comes alive")

    while time.time() < epoch + 80:
        shown = on_screen()
        if "is open" in shown or "đã mở" in shown:
            say(f"the app acted at {int(time.time()) - epoch:+d}s from the code's "
                "own timestamp, and the door screen is up")
            say("the scan loop works: camera, ML Kit, the code rules, the screen")
            return 0
        time.sleep(2)

    say("the app never acted on the code, and the camera was working")
    say("the picture it was shown is at " + FRAME.relative_to(ROOT).as_posix())
    return 1


if __name__ == "__main__":
    sys.exit(main())
