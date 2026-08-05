# Cabinet simulator

A simulated VGU locker cabinet, built in Blender from the design drawing.

**This is not evidence.** A task whose `Verify` names the real cabinet is ticked by the real cabinet, never by this. The simulator is for building; the cabinet is for proving. See [ADR 0008](../../docs/adr/0008-cabinet-simulator.md) and rule A5.

## What it is for

Answering one kind of question, over and over, in seconds: *when the server says "open box 04", does the right thing happen?*

It cannot show a jammed door, a lock that sticks, a screen washed out by sunlight, or a Wi-Fi dead spot. Those need steel.

## Running it

Needs Blender 3.0 or newer.

```bash
blender --python build_cabinet.py
```

Or paste it into Blender's **Scripting** tab and press Run. Or, with the Blender MCP addon connected, run it through `execute_blender_code`.

Then drive it from the Python console:

```python
set_screen('LOOKUP')     # shipper is typing a phone number
open_door(4)             # server opened box 04
door_states()            # {1: 'shut', 2: 'shut', 4: 'open', ...}
close_door(4)
reset()                  # every door shut, screen back to Welcome
```

## The event API

| Call | What it stands for |
| --- | --- |
| `open_door(n)` | The server told the cabinet to open box `n` |
| `close_door(n)` | Somebody shut the door — which is what records a drop or a collection ([ADR 0006](../../docs/adr/0006-no-sensor.md)) |
| `set_screen(state)` | The cabinet screen changed. States are listed in `SCREEN_STATES` |
| `door_states()` | Every door and whether it is open. What a test asserts against |
| `reset()` | Back to the start. Run between tests |
| `build()` | Rebuild from scratch. Safe to run again |

## Changing the cabinet

Every dimension is a constant at the top of `build_cabinet.py`, straight off the design sheet. Change those, not the code below them. `GRID` is the door layout, written the way the drawing reads.

## One thing the drawing gets wrong

The spec says **20 compartments**. The drawing numbers doors 01 to 20 but **06 is missing** — the control panel sits there. Count the doors and there are **19**.

The model is built to the drawing, so it has 19. Assumption **A-15** in [working-rules.md](../../docs/reference/working-rules.md) tracks getting an answer, because it changes how many parcels a cabinet holds.
