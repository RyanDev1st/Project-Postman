# 0022 — The hashing key lives outside the database file

- **Status:** accepted
- **Date:** 2026-08-21
- **Deciders:** Ryan, IT team

## Context

Every credential this server holds is stored as a hash, never as itself: the
login token, the one-time code that registers a phone, the pickup code that
opens a box, and the cabinet key. That is the right shape, and it was wrong
anyway for half of them.

Hashing protects a secret only while the secret is too long to enumerate. A
pickup code is **six digits**. There are a million of them, and until today
the digest was a bare SHA-256, so the whole table could be built and searched
in about a second on this laptop. Anybody who got hold of `data/locker.db`
could read every live pickup code out of it and open the boxes those parcels
were in, for the next forty-eight hours. The same is true of the one-time
codes that log people in.

This was written down in the audit of 2026-08-18 and left open. It was closed
on 2026-08-21 with an HMAC.

Measured, on the real database, on the day:

| The attacker holds | Result |
| --- | --- |
| `data/locker.db` | all 1,000,000 codes tried in 2 s, **nothing found** |
| `data/locker.db` **and** `config/pepper.key` | code recovered in 5 s |

## Decision

**`Ids.hash` is an HMAC-SHA256 under a 32-byte key, and that key is
deliberately not in the database.**

- The key is `config/pepper.key`, git-ignored, or `LOCKER_PEPPER` in the
  environment. A real deployment uses the variable and has no file.
- **Not in `data/`.** That folder is the thing that gets copied - onto a
  laptop to look at a bug, into a backup, onto a disk that is thrown away. A
  key beside the database travels in every one of those copies and buys
  nothing. `scripts/backup.py` copies the database and says so out loud.
- One key for the whole server, not one per row. A per-row salt stops one
  table serving every row; it does not stop the person holding the table,
  because the salt is in the row next to the hash. Only a secret they do not
  have does that.
- Passwords are unaffected and stay on Argon2id - see ADR 0012. Argon2 is
  slow by design, which is the other way to make enumeration expensive, and
  it is the right one where the input is chosen by a person.

## Consequences

**Losing the key is worse than losing a password file.** Every hash ever
written was made under it and cannot be re-derived - one-way is the point. If
it is lost: every phone is logged out, every code in the air is dead, and
every cabinet has to be rotated by hand. **Back it up separately from the
database.** The two together in one folder is the state this ADR exists to
prevent, so putting the backups beside each other undoes it exactly.

**It was a one-time break.** Migration 3 deletes the tokens and one-time codes
written under the old scheme, because nothing a caller could type would ever
match them again. Everyone registers once more. Cabinet keys had to be
rotated by hand, and were, on 2026-08-21.

**The hot path had to be fixed once.** Calling `Mac.getInstance` per request
halved read throughput at 300 concurrent - 5,488 requests a second down to
2,589 - because each call walks the JCA provider list under a lock. One `Mac`
per thread, keyed once, brought it back: the hash itself costs **287 ns**,
measured over a million calls, which is 0.2 % of one core at the rates this
server sees.

**The test scripts now need the key.** `checkloop.py` and `stress.py` recover
codes by brute force, and cannot without it. That is the fix working, not a
cost of it: they run on the machine that owns both, which is the access a
person debugging their own server already has.

## Alternatives considered

**Leave it and rely on the file being secret.** The file is a database on a
Raspberry Pi in a corridor, and it is the one artefact anybody debugging will
copy to a laptop. "Nobody will get the file" is the assumption every leaked
database was built on.

**Longer pickup codes.** Ten digits would be ten billion guesses, which is
still an evening. It also makes a person read ten digits off a screen and
type them, to fix a problem the key fixes for nothing.

**Argon2id for the short codes too.** It would work, and it costs about 50 ms
per verification by design. That is fine on a login screen and wrong on a path
the cabinet hits for every box it opens.
