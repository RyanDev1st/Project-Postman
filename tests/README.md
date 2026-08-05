# tests

Test suites. The folder shape here **mirrors `src/` exactly**. A test for `src/locker/open/` lives in `tests/locker/open/`.

Empty until the tech stack is chosen (task P0-01).

## Rules

1. Mirror `src/`. No other shape.
2. A mock is allowed in a test. A mock is never allowed in a shipping path, and it must be labelled clearly as a fixture.
3. A test that needs a real locker to open is not a unit test. It is a device test — log it in [`docs/reference/bug-log.md`](../docs/reference/bug-log.md) and in the phase's Verify table.
4. The Verify table in each phase file is the definition of `done`. Automate what you can from it.

Fill in the test command in `CLAUDE.md` under **Verification** as soon as the stack exists.
