# scripts

Build, release and helper scripts.

## Rules

1. Every script has a one-line comment at the top saying what it does and when to run it.
2. A script never contains a key, token or password. It reads them from the environment.
3. Add a row here when you add a script.

| Script | What it does | When to run it |
| --- | --- | --- |
| `checkboard.py` | Reads every roadmap file and checks the counts, the IDs and the dependency links agree | Every time you tick a task, before you commit. It exits non-zero if the board is wrong |
| `cabinet-sim/` | Builds the Blender cabinet and opens its doors. **Never evidence** — [ADR 0008](../docs/adr/0008-cabinet-simulator.md) | When you need to see the cabinet without having one |
