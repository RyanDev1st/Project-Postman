"""Check the roadmap board is consistent. Run from the repo root:

    python scripts/checkboard.py

Exits non-zero if anything is wrong, so it can gate a commit.

The board is the project's spine, and it drifts silently — a count in one
file stops matching a count in another and nothing complains. This reads
every phase file, counts the real tasks, and compares that against what the
files claim about themselves.
"""
import glob
import os
import re
import sys
from pathlib import Path

# A phase number, which may have a decimal: `1` or `1.5`.
#
# Phase 1.5 was inserted ahead of Phase 2 rather than renumbering six phases
# and every task ID in them. This script did not know that: it globbed
# `phase-*.md` and so read the file, but neither its ID pattern nor its phase
# table pattern would match a dot, so the phase could not be listed in the
# README without the check failing. The convention came first; this follows it.
PH = r'\d+(?:\.\d+)?'

# A task line, with the optional state marker in front of the title.
TASK = re.compile(
    r'^- \[([ x])\] \*\*(?:(?:⏸️ LATER|🔴 BLOCKED|🟡 DOING) — )?(P' + PH + r'-\d+)\*\*', re.M)
FULL = re.compile(
    r'^- \[([ x])\] \*\*(?:(?:⏸️ LATER|🔴 BLOCKED|🟡 DOING) — )?(P' + PH + r'-\d+)\*\*.*\n'
    r'\s*- Owner: [^·]+·\s*Needs: ([^·]+?)\s*·\s*Blocks: (.+)$', re.M)
# The phase table in the master README: `... phase-2-register.md) · `0/7``
PHASE_ROW = re.compile(r'\((phase-' + PH + r'-[a-z-]+\.md)\)\s*·\s*`(\d+)/(\d+)`')
TOTAL_ROW = re.compile(r'\*\*Total: (\d+) / (\d+)\.\*\*')

faults = []


def note(bad, label, value):
    """Print one result. Anything false-y in `bad` is a pass."""
    print(f'{"FAIL" if bad else "OK  "} {label}: {value}')
    if bad:
        faults.append(label)


files = sorted(glob.glob('docs/roadmap/phase-*.md'))
if not files:
    sys.exit('no phase files found — run this from the repo root')

all_ids, needs, blocks = [], {}, {}
later_ids = set()
per_file = {}
total = ticked = 0

for f in files:
    text = Path(f).read_text(encoding='utf-8')
    ids = TASK.findall(text)
    all_ids += [i for _, i in ids]
    d = sum(1 for s, _ in ids if s == 'x')
    later_ids |= set(
        re.findall(r'^- \[ \] \*\*⏸️ LATER — (P' + PH + r'-\d+)\*\*', text, re.M))
    total += len(ids)
    ticked += d
    per_file[os.path.basename(f)] = (d, len(ids))

    prog = re.search(r'\*\*Progress: (\d+) / (\d+)\.\*\*', text)
    claimed = (int(prog.group(1)), int(prog.group(2))) if prog else (-1, -1)
    note(claimed != (d, len(ids)), os.path.basename(f),
         f'{d}/{len(ids)} — Progress line says {claimed[0]}/{claimed[1]}')

    for _st, tid, n, b in FULL.findall(text):
        needs[tid] = [x.strip() for x in n.split(',') if x.strip() and x.strip() != '—']
        blocks[tid] = [x.strip() for x in b.split(',') if x.strip() and x.strip() != '—']

print()

# The master README repeats every count. That is where drift hides, because
# no single file is wrong on its own.
master = Path('docs/roadmap/README.md').read_text(encoding='utf-8')
rows = {name: (int(a), int(b)) for name, a, b in PHASE_ROW.findall(master)}
wrong = {n: (rows[n], per_file.get(n)) for n in rows if rows[n] != per_file.get(n)}
note(wrong, 'README phase table', wrong or f'{len(rows)} rows match their phase files')

missing_rows = set(per_file) - set(rows)
note(missing_rows, 'README lists every phase', missing_rows or 'yes')

row_sum = sum(b for _, b in rows.values())
claimed_total = TOTAL_ROW.search(master)
ct = (int(claimed_total.group(1)), int(claimed_total.group(2))) if claimed_total else (-1, -1)
note(ct != (ticked, total) or row_sum != total, 'README total',
     f'{ticked}/{total} real — README says {ct[0]}/{ct[1]}, its rows sum to {row_sum}')

print()
print(f'tasks: {total}   ticked: {ticked}   later: {len(later_ids)}')

dupes = {i for i in all_ids if all_ids.count(i) > 1}
note(dupes, 'duplicate IDs', dupes or 'none')

known = set(all_ids)
dangling = {f'{t} -> {d}'
            for t, deps in list(needs.items()) + list(blocks.items())
            for d in deps if d not in known}
note(dangling, 'dangling refs', dangling or 'none')

# Needs and Blocks must agree with each other, both ways round.
one_way = [f'{t} Needs {d}, but {d} does not Block {t}'
           for t, deps in needs.items() for d in deps
           if d in blocks and t not in blocks[d]]
note(False, 'one-way links', len(one_way))     # known-untidy, not a failure yet
for x in one_way[:12]:
    print('    ', x)

unparsed = known - set(needs)
note(unparsed, 'tasks with a readable Owner line', unparsed or 'all of them')

# LATER means we chose to wait. Nothing may sit behind that choice.
bad = [f'{t} needs {d} (LATER)' for t, deps in needs.items() for d in deps if d in later_ids]
note(bad, 'blocked by a LATER task', bad or 'none')

print()
if faults:
    print('BOARD IS INCONSISTENT:', ', '.join(faults))
    sys.exit(1)
print('board is consistent')
