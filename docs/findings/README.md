# docs/findings

**Shape:** a dated snapshot — a test round, an audit, a triage, an inspection. Name: `YYYY-MM-DD-<topic>.md`.

**Lifecycle:** immutable. Never edit a finding after its date. Same topic, newer date → write a fresh file and `git mv` the old one to `docs/legacy/`.

**Required layout:**

```
Parent: <relative-path>   (or: Parent: none)

## Status
## Scope
## Evidence      — commands run and what came out
## Next          — numbered list
```

**Write a finding only when the snapshot itself has lasting value** — a device test round, a performance measurement. If the investigation just changes how the system works, update a `reference/` doc instead, or write an ADR. Do not let this folder rot into a graveyard.
