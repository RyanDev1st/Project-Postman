# ADR 0016 — Precise location, and a route from where you are

- **Status:** accepted
- **Date:** 2026-08-14
- **Decided by:** Ryan (asked directly, options put to him)
- **Reverses:** the removal of `ACCESS_FINE_LOCATION`, made in the change that
  brought in the live map
- **Related:** [ADR 0014](0014-openfreemap-not-google.md) — OpenFreeMap tiles,
  no key, no account, no card

## The problem

Ryan, 2026-08-14: *"the map doesnt pick up my location correctly and path the
way correctly."*

He was right, and it was not a bug. The walk on the Home card is a **baked**
line: eleven points fetched once from a router in August, running from a fixed
spot on campus to the back gate. It is drawn the same way wherever the phone
is. The blue dot showed where the person actually was, and the camera framed
only the baked line — so anyone not standing on that exact spot saw a path
beginning somewhere they had never been, and often their own dot nowhere on
screen.

## What made it hard

The app asked only for `ACCESS_COARSE_LOCATION`, and that was **on purpose**.
The manifest said so:

> The location component works from a coarse fix, and the one thing that would
> change is an accuracy nobody looking at this card can see.

That reasoning was correct for what the map did at the time: draw a fixed line
and a dot. A coarse fix is good to one to three kilometres, and on a 112dp card
a dot two kilometres out still looks like it is roughly here.

It stops being correct the moment the walk is routed **from** the fix. The walk
to the gate is 543 m. A start point up to 3 km out does not produce a slightly
wrong route; it produces a route down different streets, and the card would
then be wrong in exactly the way this work exists to fix.

So there was no way to do what was asked without precise location, and the
earlier decision could not simply be overridden in silence.

## The decision

**Ask for precise location, and route the walk live from where the person is.**

Put to Ryan as three options — precise plus live routing, stay coarse and draw
no path from the person, or only fix the camera. He chose the first, and said
he would be testing **off campus, elsewhere in Vietnam**, which decided the
shape of the rest.

1. **`ACCESS_FINE_LOCATION` returns to the manifest.** Both permissions are
   requested together, so the system dialog shows *Precise* and *Approximate*
   side by side. Asking for fine alone removes the second and takes the choice
   away. Approximate still gets the blue dot and the gate; it does not get a
   route.
2. **Routing is Valhalla at `valhalla1.openstreetmap.de`,** over OSM data
   (ODbL) — the same service that produced the baked line, so the live route
   and the fallback cannot disagree about where a path runs. No key, no
   account, no card, which is the bar this project holds a service to.
3. **The route call gets its own connection, not the app's `Http`.** That door
   attaches the receiver's bearer token to every request. A third-party router
   has no business holding a token that opens lockers. HTTPS only: the request
   body is where a person is standing.
4. **Three answers, not one.** The card says a different thing for each, and
   the map draws to match:

   | Situation | Drawn | Said |
   | --- | --- | --- |
   | Route from you | your real route | `7 min walk · 543 m`, its own numbers |
   | Farther than 3 km | the approach to the gate | `38 km away · too far to walk` |
   | No fix, or the call failed | the approach to the gate | the baked figures |

5. **Past 3 km, no routing call is made at all.** The straight-line distance is
   checked first. A forty-minute line across a province is worse than drawing
   nothing — it describes a journey nobody is making — and it also spares a
   public service a request that was never going to be useful.

## What this costs

- **The app now asks for precise location.** That is a real escalation and the
  reason this is an ADR rather than a commit. It is asked for on the Home
  screen when the map first draws, not on first open, and refusing it leaves
  the app working: the map still shows the gate and the way in.
- **Where the person is standing goes to a third party** on every Home visit
  within walking range. Not to the Server team, not stored, not logged — but it
  does leave the phone, and it goes to a public instance run by OSM
  volunteers.
- **A runtime dependency on a service nobody pays for.** Every failure path
  falls back to the baked line, so the card is never blank and never wrong;
  the worst case is the behaviour we had yesterday.
- **The fix is the last one the phone happens to hold,** not a fresh one.
  Turning on the GPS to draw a 112dp card would cost battery for accuracy the
  card cannot show. A stale fix from across town is caught by the 3 km check.

## What would reverse it

Any of these, and this ADR should be replaced rather than edited:

- The Server team gives cabinets real coordinates and a route of their own.
- The public Valhalla instance asks us to stop, or starts refusing us.
- Ryan decides precise location is not worth it — Option 2 in the question he
  was asked is still a small change: drop the routing call, keep the camera
  framing both points, and say the distance.
