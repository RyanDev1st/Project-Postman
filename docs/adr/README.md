# docs/adr — Architecture Decision Records

**Shape:** one short file per non-obvious decision. It records the **why**, not the how. Name: `NNNN-short-title.md`, numbered in order.

**Lifecycle:** immutable once `accepted`. To change a decision, write a **new** ADR that supersedes the old one, and set the old one's status to `superseded by NNNN`.

**Write an ADR when:**
- the choice was not obvious, and a future team member would ask "why did they do that?"
- two reasonable options existed and you picked one
- you accepted a known downside on purpose

**Do not write an ADR for:** naming, formatting, or anything the code already shows.

Copy [`0000-template.md`](0000-template.md) to start. Add a row to `docs/README.md` in the same change.
