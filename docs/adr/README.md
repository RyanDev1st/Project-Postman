# docs/adr — Architecture Decision Records

**Shape:** one short file per non-obvious decision. It records the **why**, not the how. Name: `NNNN-short-title.md`, numbered in order.

**Lifecycle:** an ADR is the best record of *why*, and it is allowed to be corrected. It is not immutable, because the reason for a decision keeps arriving after the decision is written — a number gets measured, a domain turns out to have two forms, an idea gets tried and fails. Decided by Ryan, 2026-09-03.

| The decision | What to do |
| --- | --- |
| Sharpened, corrected, or filled in — and **nothing is built on it yet** | **Amend the file in place**, and add a dated note at the top saying what changed and why. Never a quiet edit |
| Reversed, or changed after code, screens or the contract already follow it | **Write a new ADR** that supersedes it, and set the old one's status to `superseded by NNNN` |

The dated note is the part that is not optional. A reader six months from now has to be able to see that the page changed and what it used to say — that is the whole value of the file. An amendment with no note is how a decision quietly becomes something nobody agreed to.

**Write an ADR when:**
- the choice was not obvious, and a future team member would ask "why did they do that?"
- two reasonable options existed and you picked one
- you accepted a known downside on purpose

**Do not write an ADR for:** naming, formatting, or anything the code already shows.

Copy [`0000-template.md`](0000-template.md) to start. Add a row to `docs/README.md` in the same change.
