# The long film — direction

Sixty seconds explaining the product to somebody who has never heard of it. Shown to lecturers, to the other teams, and to whoever asks *"so what are you actually building?"*

**Subtitles, no voice.** Decided 2026-08-07. There is no good text-to-speech on this machine — the Windows SAPI voices sound like a 2010 satnav, and a synthetic voice over a careful film makes the whole thing look cheap. Type on screen is a deliberate choice; a robot voice is an apology. A real voice can be recorded over the same cut later, and the subtitle file is the script for it.

## The one thing this film has to land

**Two people who never meet.** That is the product. Everything else — the app, the QR, the boxes — exists to make that possible.

A viewer who remembers only *"the shipper leaves it, you collect it later, nobody waits for anybody"* has understood it. A viewer who remembers the technology has not.

## What it must not do

- **No narration written like an advert.** No *"revolutionising campus logistics"*, no *"seamless"*, no *"empowering students"*. Short sentences about what is happening on screen.
- **No feature list.** The film shows one parcel making one journey. Features are what the reader finds afterwards.
- **No fake precision.** No invented statistics, no *"trusted by 12,000 students"*. Nothing is deployed yet and saying otherwise in a student project is how you lose a room.
- **No claim this is real hardware.** It is a model, it says so at the end, and [ADR 0008](../../../docs/adr/0008-cabinet-simulator.md) is why.

## The scene

A campus gate, late afternoon, tropical. Built from what is known — VGU is in Bến Cát, Bình Dương; the light is hard and warm; the planting is palm and frangipani — and **not traced from anybody's imagery.**

The intent was the D9 backgate facing ABO. It is not a reconstruction of it: the browser extension that could show a map was not connected, so nothing was measured. **It is a plausible Vietnamese campus gate, not that gate.** Swap it when there are photographs — the scene is one function in `shot_story.py`.

## The shots

Nine beats. Cuts, not moves — the wooden figure has no walk, and a camera that drifts while nothing walks looks like a mistake rather than a choice. Each shot holds still or moves slowly, and the cut does the work.

| # | Shot | Subtitle |
| --- | --- | --- |
| 1 | Wide. The gate, the wall, the cabinet standing against it | *A parcel locker at the university gate.* |
| 2 | The shipper arrives, parcel under his arm | *The shipper has nobody to find.* |
| 3 | Close on the screen. He types a number | *He types the receiver's phone number.* |
| 4 | A door swings open | *The server picks a free box and opens it.* |
| 5 | The parcel goes in | *He puts the parcel in.* |
| 6 | The door shuts. Hold | *He closes the door. That is the delivery.* |
| 7 | The phone, in a hand. The box number, large | *Her phone tells her which box.* |
| 8 | She holds the phone to the cabinet screen | *She scans the cabinet. The app proves who she is.* |
| 9 | Her box opens, amber | *Her box. Only hers.* |
| — | Black | *Nobody waited for anybody.* |

**Shot 8 is the one worth being careful about.** The phone scans the cabinet, never the reverse — [ADR 0003](../../../docs/adr/0003-parcel-locker-product.md). If the film shows the cabinet scanning a phone, it teaches the wrong product to every person who watches it, and that is the misunderstanding this project keeps having to correct.

## Type

Subtitles are set in the film, not burned into the render, so wording can change without re-rendering fifty seconds. Bottom third, generous margin, single line where possible.

Sixty seconds is about 90 words. Every one of them is doing work or it comes out.
