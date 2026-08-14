# ADR 0017 — No line when it is not a walk

- **Status:** accepted
- **Date:** 2026-08-14
- **Decided by:** Ryan, from a phone 50 km from campus
- **Reverses:** one row of [ADR 0016](0016-precise-location-and-live-routing.md)
  — what the map *draws* past 3 km. The rest of 0016 stands.

## The problem

ADR 0016 gave the card three answers and got the words right. Ryan, holding
the app 50 km away, saw the caption say **"40 km away · too far to walk"** over
a map showing the **walking route around the campus back gate** — a red line
down streets he was 50 km from.

> *"mate map still bad, doesnt show the right location it shows exactly this
> always ... its easy and obvious to see that this map is wrong"*

The picture was the same at forty kilometres as at forty metres. He was right
that it looked broken, and he was right about why: nothing on it had anything
to do with where he was.

## What 0016 got wrong

That ADR's own table said, for the far case, *drawn: the approach to the
gate*. The reasoning given for not framing both points was:

> from thirty kilometres away a box containing both you and the cabinet is a
> view of a province with two invisible dots in it

Two things wrong with that.

**The dots are not invisible.** Both markers are drawn at a fixed size on the
screen — a circle layer and the location puck. They are exactly as big at
forty kilometres as at four hundred metres. The claim was never checked; it
was asserted, and it was false.

**A red line on a map means "this is your way".** A caption that is not true
is easy to catch, because a sentence reads as a claim. A picture does not, and
so a drawn route is the more dangerous of the two. Drawing the baked approach
because it was the only line to hand made the map assert something nobody had
decided to say.

## The decision

**Past 3 km the map draws no line at all.** It shows two things — where you
are and where the cabinet is — and frames the camera on both. The gap between
them on screen is the fact, and it says the distance better than the caption
does.

Revised, the row now reads:

| Situation | Drawn | Said |
| --- | --- | --- |
| Route from you | your real route | `6 min walk · 536 m` |
| Farther than 3 km | **you and the cabinet, no line** | `38 km away · too far to walk` |
| No fix, or the call failed | the approach to the gate | the baked figures |

The last row is unchanged and deliberately so: with no fix there is nothing
else to show, and the baked line is honest there — it says where the gate is
and claims nothing about where anyone started.

## What this costs

- **The far view is not useful for getting anywhere.** It is not meant to be.
  At that distance the useful facts are *the locker is at the back gate* and
  *you are nowhere near it*, and the arrow on the card still hands the journey
  to a maps app that can actually route a drive.
- **The camera can sit at a wide zoom on a 112dp card**, which is a lot of
  grey. Grey with two marks in the right places beats a street map of a place
  the person is not.

## Verified

On the emulator at `10.7769,106.7009` — Ho Chi Minh City, 38 km from the gate:
no route line drawn, the gate dot and the phone's own marker both in frame,
caption `38 km away · too far to walk`. At `11.1059,106.6142`, by the campus,
the live route still draws: `6 min walk · 536 m`.

Not verified: how the "you are here" puck looks. On this machine the emulator
runs software GL and the puck draws as a black blob, the same texture failure
that produced the dark blobs in the glass-bead check. A real GPU is needed to
see it, which means Ryan's phone.
