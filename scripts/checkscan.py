"""Show the app a live cabinet code and watch what it does.

    python scripts/checkscan.py

Exit code 0 means the app gave the right answer. Anything else means read the
output.

WHAT THIS IS FOR

P5-02's Verify is a real phone reading a real cabinet screen, in daylight and
in a dark corridor, and no machine here can stand in for that. What a machine
*can* do is run the rest of the loop: a real camera stream, the real ML Kit
detector, the real rules in `pickup/SessionCode.kt`, and the real screen change
at the end of it. That is what this drives.

WHAT IT CHECKS NOW, AND WHY IT CHANGED

This used to prove that scanning the back gate opened box 04 and that scanning
the library said "Nothing waiting here". **The app decides neither of those any
more.** Which door opens is the server's answer, from `Collect.byScan`, and the
list of parcels the app used to check against is gone - it ran on the caller's
phone and protected nothing.

So what is left on this emulator is the half that is still the app's: it reads
a code, judges whether it is worth sending, and sends it. There is no server
address in this build, so the send cannot succeed - and the rule that matters
is what the app does then. **It must say it could not tell, and it must never
show the door screen.** That is rule 4 in architecture.md and task P5-06, and
an emulator can prove it exactly.

To watch a door actually open, run `scripts/checkserver.py`: that drives the
same rules through the real server, over TLS, including the four attacks.

HOW THE TIMING WORKS

Nothing here sets a clock or waits on a stopwatch. The code is dated a few
minutes ahead, so the app first reads it and refuses it - a code from the
future is not a code this cabinet issued a moment ago - and keeps looking. The
instant the clock reaches it, the same picture becomes a live code and the app
answers. The app's own freshness rule picks the moment, which is also the
point: a run that passes has exercised the refusal as well as the read.

WHAT IT DOES NOT PROVE

No lens, no glass, no lit screen, no corridor. The camera is handed a picture
file. See `docs/roadmap/phase-5-pickup.md`.

REQUIREMENTS

An emulator AVD, `node`, Pillow and zxing-cpp. The app must build for x86_64,
so the APK is the `-Pemulator` one - build it first.
"""

from __future__ import annotations

import re
import shutil
import sys
import tempfile
import time
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import emulator as phone
import qrimage

ROOT = qrimage.ROOT
APK = ROOT / "src/app/build/outputs/apk/debug/app-debug.apk"
COMPONENT = "vn.edu.vgu.smartlocker/.MainActivity"

# Written twice, on purpose. The copy under build/ is for a person to look at
# when a run fails. The copy the emulator is handed must sit at a path with no
# space in it: this repo lives in "Project Postman", and given that path the
# emulated camera opened, created its capture session, and then delivered
# nothing but black - no error anywhere, in the log or on screen.
FRAME = ROOT / "build" / "checkscan-frame.png"
CAMERA_FRAME = Path(tempfile.gettempdir()) / "checkscan-frame.png"

# The cabinet the app believes a parcel of yours is in - MainActivity's
# YOUR_PARCELS. Any other cabinet id should be refused instead.
CABINET = "vgu-back-gate"
# Stands in for the 32 random bytes the server puts in a real code.
RANDOM_FIELD = "hQ2wE5rT8yU1iO4pA7sD0fG3hJ6kL9zX"

# Long enough to cold boot, install, sign in and reach the scan screen, with
# room to spare. The code is live for 90 seconds after this, so overshooting
# costs nothing and undershooting costs a whole run.
SETUP_SECONDS = 260


def wording(name: str) -> list[str]:
    """One string resource, in every language the app has.

    Read from the app rather than written out here. The first version of this
    check looked for "No parcel for you here" and reported that the app never
    answered, while the app was answering "Nothing waiting here." on screen the
    whole time - a check that fails when the app is right is worse than no
    check. Copying words is how that happens, so these are not copied.

    Sorted so the base `values/` comes first and English is what gets printed.
    """
    found = []
    for xml in sorted(ROOT.glob("src/app/src/main/res/values*/strings.xml")):
        hit = re.search(rf'<string name="{name}">(.*?)</string>',
                        xml.read_text(encoding="utf-8"))
        if hit:
            found.append(hit.group(1))
    return found


# The sentence for "we do not know what happened". A live code with no server
# behind it must land here, never on the door screen.
UNCLEAR = wording("unclear_result")
OPENED = ["is open", "đã mở"]
SCAN_SCREEN = ["scan the cabinet", "quét màn hình tủ"]
NO_CAMERA = ["no camera", "không có máy ảnh"]


def make_frame(payload: str) -> bool:
    """The picture the camera will be given, checked before it is used."""
    import zxingcpp

    code, version = qrimage.draw(payload, scale=9, quiet=6)
    read = zxingcpp.read_barcode(code)
    if not read or read.text != payload:
        phone.say(f"the code does not decode before it is even shown: {read and read.text!r}")
        return False
    FRAME.parent.mkdir(parents=True, exist_ok=True)
    qrimage.tiled(code, 1280, 1280).convert("RGB").save(FRAME)
    shutil.copyfile(FRAME, CAMERA_FRAME)
    if " " in str(CAMERA_FRAME):
        phone.say(f"the camera path still has a space in it: {CAMERA_FRAME}")
        return False
    phone.say(f"camera picture: v{version}, {code.width}px codes tiled 3x3, reads back exactly")
    return True


def sign_in() -> bool:
    """The demo build takes any six digits - there is no server yet."""
    phone.adb("shell", "input tap 390 1088")
    time.sleep(1)
    phone.type_text("912345678")
    if not phone.tap(r"send code|gửi mã", "send code"):
        return False
    time.sleep(3)
    phone.type_text("123456")
    if not phone.tap(r"continue|tiếp tục", "continue"):
        return False
    time.sleep(4)
    return True


def ready() -> bool:
    """On the scan screen, with the camera actually delivering frames."""
    seen = phone.on_screen()
    if any(s in seen for s in NO_CAMERA):
        phone.say("the camera was refused, so this run proves nothing about reading")
        return False
    if not any(s in seen for s in SCAN_SCREEN):
        phone.say("never reached the scan screen")
        return False
    if phone.dark_patch():
        phone.say("on the scan screen, and the viewfinder is black - no frames are")
        phone.say("arriving, so nothing below would be about the app at all")
        return False
    return True


def usable() -> bool:
    if not APK.exists():
        print(f"cannot run: {APK.relative_to(ROOT).as_posix()} is missing")
        print("  ./gradlew :app:assembleDebug -Pemulator")
        return False
    with zipfile.ZipFile(APK) as apk:
        if not any(n.startswith("lib/x86_64/") for n in apk.namelist()):
            print("cannot run: the APK has no x86_64 libraries, so no emulator")
            print("  ./gradlew :app:assembleDebug -Pemulator")
            return False
    try:
        import zxingcpp  # noqa: F401
        from PIL import Image  # noqa: F401
    except ImportError as missing:
        print(f"cannot run: {missing.name} is not installed")
        print("  python -m pip install zxing-cpp pillow")
        return False
    return True


def main() -> int:
    if not usable():
        return 2

    # The cabinet in the code. It no longer changes the right answer - the
    # server decides that and there is no server here - but a real id is what
    # a real screen would draw, and the app still refuses a malformed one.
    cabinet = CABINET

    epoch = int(time.time()) + SETUP_SECONDS
    # The fourth field is what the server puts there. Nothing in the app
    # reads it - only the server can - but a code without one is refused
    # before it is ever sent, so the picture has to carry one.
    payload = f"VGU1|{cabinet}|{epoch}|{RANDOM_FIELD}"
    phone.say(f"code {payload} - live from {epoch - 15} to {epoch + 75}")
    phone.say(f'expecting "{UNCLEAR[0]}", and never the door screen')

    if not make_frame(payload) or not phone.boot(CAMERA_FRAME):
        return 1
    phone.install(APK, COMPONENT)

    if not sign_in() or not phone.tap(r"scan a cabinet|quét tủ", "the scan button"):
        return 1
    time.sleep(4)
    if not ready():
        return 1
    phone.say(f"on the scan screen and frames are arriving, "
              f"{epoch - 15 - int(time.time())}s until the code comes alive")

    while time.time() < epoch + 80:
        shown = phone.on_screen()
        opened = any(s in shown for s in OPENED)
        unclear = any(s.lower() in shown for s in UNCLEAR)
        if not opened and not unclear:
            time.sleep(2)
            continue
        when = f"{int(time.time()) - epoch:+d}s from the code's own timestamp"

        # The one that must never happen. A door screen here would mean the
        # app announced an open it never got an answer about.
        if opened:
            phone.say(f"the app showed the door screen at {when}")
            phone.say("  it had no server to ask, so nothing told it a door moved")
            return 1

        phone.say(f"the app acted at {when}, and said it could not tell")
        phone.say("  camera, ML Kit, the code rules and the send all ran")
        phone.say("  and an unreachable server was not drawn as success")
        return 0

    phone.say("the app never acted on the code, and the camera was working")
    phone.say("the picture it was shown is at " + FRAME.relative_to(ROOT).as_posix())
    return 1
