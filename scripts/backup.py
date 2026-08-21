"""Copy the database somewhere else, while the server is still running.

    python scripts/backup.py                 into data/backups/, keeping 7
    python scripts/backup.py --to D:/locker  onto another disk
    python scripts/backup.py --keep 30       keep a month of them

## Why not just copy the file

Because `locker.db` is not one file. SQLite in WAL mode keeps recent writes in
`locker.db-wal` until a checkpoint moves them, so a plain copy of the `.db`
taken mid-write is missing whatever has not been checkpointed - and a copy of
all three files taken one at a time is three moments stitched together, which
can be a database that never existed. SQLite's own backup API reads a
consistent snapshot from a live database and is the supported way to do this.

The copy is opened afterwards and asked `PRAGMA integrity_check`. A backup
nobody has opened is a belief, not a backup.

## What this deliberately does NOT copy

`config/pepper.key`. Every credential in the database is hashed under that
key, so the two together are worth much more than either alone - which is the
entire reason the key does not live in `data/`. Copy it once, by hand, to
somewhere the database backups do not go. See `Pepper.kt`.
"""

from __future__ import annotations

import argparse
import shutil
import sqlite3
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB = ROOT / "data/locker.db"


def take(source: Path, into: Path, keep: int) -> Path:
    into.mkdir(parents=True, exist_ok=True)
    stamp = time.strftime("%Y-%m-%d-%H%M%S")
    out = into / f"locker-{stamp}.db"
    # Two runs in the same second would otherwise silently overwrite, and the
    # older copy is the one worth keeping.
    n = 2
    while out.exists():
        out = into / f"locker-{stamp}-{n}.db"
        n += 1

    live = sqlite3.connect(f"file:{source}?mode=ro", uri=True)
    copy = sqlite3.connect(out)
    try:
        live.backup(copy)
        # The copy inherits WAL from the original, which means it is three
        # files rather than one - and the other two are written the moment
        # anybody opens it. A backup has to be one file that can be carried
        # away, so this one is switched off WAL before it is closed.
        copy.execute("PRAGMA journal_mode=DELETE")
    finally:
        copy.close()
        live.close()

    # Read-only, so opening it to check does not put a -wal beside it again.
    check = sqlite3.connect(f"file:{out}?mode=ro", uri=True)
    try:
        verdict = check.execute("PRAGMA integrity_check").fetchone()[0]
        rows = check.execute("SELECT count(*) FROM parcels").fetchone()[0]
    finally:
        check.close()
    if verdict != "ok":
        out.unlink(missing_ok=True)
        sys.exit(f"the copy did not open cleanly: {verdict}")

    size = out.stat().st_size / 1_000_000
    print(f"{out}  {size:.1f} MB  integrity ok  {rows} parcels")

    old = sorted(into.glob("locker-*.db"))[:-keep] if keep > 0 else []
    for f in old:
        f.unlink()
        # Older copies made before the WAL fix have these beside them.
        for stray in (f.with_suffix(".db-wal"), f.with_suffix(".db-shm")):
            stray.unlink(missing_ok=True)
        print(f"removed {f.name}")
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--to", default=str(ROOT / "data/backups"))
    parser.add_argument("--keep", type=int, default=7)
    args = parser.parse_args()

    if not DB.exists():
        sys.exit(f"no database at {DB}")

    into = Path(args.to).resolve()
    take(DB, into, args.keep)

    if into.is_relative_to(DB.parent) or into.drive == DB.drive:
        print()
        print("  This copy is on the same disk as the original, so it survives a")
        print("  mistake but not a dead disk. Use --to on another disk or another")
        print("  machine for the copy that matters.")
    print()
    print("  config/pepper.key is NOT in here, on purpose. Without it every hash")
    print("  in this file is unreadable. Copy it once, somewhere else.")


if __name__ == "__main__":
    main()
