"""Prove that no screen can draw a sentence it does not have.

Both front-ends now default to Vietnamese, and that turned a soft failure
into a hard one on each of them:

  * **The app.** `values/` holds Vietnamese and is the fallback for every
    other locale. A name that exists only in `values-en/` has nothing to fall
    back to, so `stringResource` throws and the screen dies - on every phone
    that is not set to English, which is all of them. Task P2-16.
  * **The cabinet screen.** `strings.js` draws `!key!` for a name it does not
    hold, loudly rather than falling back, so a key missing from one language
    is a shout on a public screen. Task P3-12.

Android's own `MissingTranslation` lint looks the other way round and would
see neither. This reads the files and compares the names.

The app half is also `StringsMatchTest`, and that is on purpose rather than
by accident: the test is what the **build** refuses to pass, and this is what
a person runs across **both** front-ends before a commit. They read the same
two files, so they cannot drift apart. The cabinet screen has no build to
hang a test on, so this script is the only check it gets.

    python scripts/checkstrings.py

Exit code 0 means clean. Anything else means read the output.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "app" / "src" / "main" / "res"
CABINET = ROOT / "src" / "cabinet"

failures: list[str] = []


def check(condition: bool, said: str) -> bool:
    print(f"  {'OK  ' if condition else 'FAIL'} {said}")
    if not condition:
        failures.append(said)
    return condition


def android_names(path: Path) -> set[str]:
    if not path.is_file():
        failures.append(f"no strings file at {path}")
        return set()
    return set(re.findall(r'<string name="([^"]+)"', path.read_text(encoding="utf-8")))


def cabinet_dicts(path: Path) -> dict[str, set[str]]:
    """The two key sets out of strings.js, without running any JavaScript.

    Split on the language headers rather than parsed: this file has no build
    step and never will (ADR 0009), so there is no bundler to ask.
    """
    text = path.read_text(encoding="utf-8")
    out: dict[str, set[str]] = {}
    parts = re.split(r"\n    (vi|en): \{", text)
    for i in range(1, len(parts), 2):
        language, body = parts[i], parts[i + 1]
        out[language] = set(re.findall(r'^\s*"([^"]+)":', body, re.M))
    return out


def main() -> int:
    print("The app - values/ is Vietnamese and is the fallback for everything")
    vi = android_names(RES / "values" / "strings.xml")
    en = android_names(RES / "values-en" / "strings.xml")
    check(bool(vi) and bool(en), f"both files read: {len(vi)} Vietnamese, {len(en)} English")
    check(not (en - vi), f"every English name has a Vietnamese one: missing {sorted(en - vi)}")
    check(not (vi - en), f"and the other way round: missing {sorted(vi - en)}")
    check(
        not (RES / "values-vi").exists(),
        "values-vi/ is gone - the default IS Vietnamese now",
    )

    print("\nThe cabinet screen - a missing key draws its own name, loudly")
    strings = CABINET / "strings.js"
    words = cabinet_dicts(strings)
    check(set(words) == {"vi", "en"}, f"two languages found: {sorted(words)}")
    if set(words) == {"vi", "en"}:
        check(
            not (words["en"] - words["vi"]),
            f"every English key has a Vietnamese one: missing {sorted(words['en'] - words['vi'])}",
        )
        check(
            not (words["vi"] - words["en"]),
            f"and the other way round: missing {sorted(words['vi'] - words['en'])}",
        )

        # Every key the screen actually asks for.
        #
        # The markup is read by attribute; the code is searched for each
        # defined key as a quoted literal rather than by call shape. Several
        # are chosen inside a ternary - `trouble(full ? "a" : "b")` - and a
        # pattern that only matched `f("key")` reported four live keys as
        # dead, which is the sort of false alarm that gets a check ignored.
        markup = (CABINET / "index.html").read_text(encoding="utf-8")
        asked = set(re.findall(r'data-t(?:-placeholder|-label)?="([^"]+)"', markup))

        code = "".join(
            (CABINET / name).read_text(encoding="utf-8")
            for name in ("drop.js", "qr.js", "screen.js")
        )
        asked |= {key for key in words["vi"] if f'"{key}"' in code}

        check(bool(asked), f"the screen asks for {len(asked)} keys")
        check(
            not (asked - words["vi"]),
            f"and holds every one of them: missing {sorted(asked - words['vi'])}",
        )

        unused = words["vi"] - asked
        check(not unused, f"nothing is defined and never drawn: {sorted(unused)}")

        check(
            '<html lang="vi">' in (CABINET / "index.html").read_text(encoding="utf-8"),
            "the page declares itself Vietnamese",
        )

    print()
    if failures:
        print(f"FAILED - {len(failures)} of them")
        for line in failures:
            print(f"  - {line}")
        return 1
    print("All checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
