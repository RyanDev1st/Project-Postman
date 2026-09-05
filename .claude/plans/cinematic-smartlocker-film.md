# Cinematic Smart Locker Product Film

## Goal
Replace the rejected floating-screenshot montage with a directed 35–40 second product film. The film will communicate the complete drop-to-collection contract through physical action, motivated cuts, and restrained UI inserts.

## Shot design
1. **Cold open / scale (0–4s)** — dark, low-key three-quarter cabinet beauty shot. Slow push toward the cabinet, screen wakes; no explanatory title.
2. **Drop arrives (4–8s)** — parcel and courier mannequin in the cabinet simulator. Medium profile shot; parcel crosses frame and enters the assigned compartment. Cut on hand withdrawal.
3. **Kiosk interaction (8–13s)** — close over-shoulder crop of the genuine cabinet screen, with a subtle hand/tap cue and shallow vignette. Number entry progresses as a consequence of the drop.
4. **Receiver notification (13–17s)** — genuine Android home screen presented as a phone held in a dark physical-looking environment, notification/home state changing via a clean match cut rather than a floating slide.
5. **Approach (17–22s)** — cabinet hero with an animated human-scale silhouette/phone foreground, lateral camera drift; the receiver enters the same visual world.
6. **Scan (22–28s)** — over-shoulder composition: phone scanner in foreground and cabinet QR in background. Scan line is only a restrained practical highlight; no label.
7. **Release (28–34s)** — cut to cabinet close-up and genuine opened-door state; mechanical light/door action carries the reveal. Use the dedicated door-open asset where a real Blender animation is unavailable.
8. **Collection / end (34–39s)** — phone opened state and parcel silhouette/hand-off suggestion, then quiet cabinet/product lockup. Product name only at the end.

## Visual rules
- One coherent charcoal/steel environment with warm parcel/cabinet key and cool phone accent.
- No step labels, badges, diagrams, bullet copy, ADR/task references, or arbitrary floating panels.
- Screens are inserts or objects in a scene, not the scene itself.
- Use hard cuts, match cuts, and short motivated dissolves only where action supports them.
- Keep text to the product name at the end.
- Preserve the existing MP4s as failed prototypes; write a new output.

## Implementation
- Add a new Python/Pillow/NumPy renderer, reusing genuine captures and the cabinet hero/door-open assets.
- Build scene-specific compositing helpers: film grain, lens vignette, practical light pools, perspective phone/kiosk placement, rack-focus-like blur, and human/hand silhouettes that connect devices to physical space.
- Use the existing cabinet simulator film shot as the physical drop reference if its rendered output is available; otherwise create a disciplined silhouette approximation rather than another isolated UI card.
- Add restrained generated sound design only if local FFmpeg can synthesize clean impacts, tap, latch, and notification cues without generic music. Video remains valid without audio if sound design would feel artificial.
- Render a short representative frame sheet and inspect it before producing the full MP4.
- Verify output with ffprobe and report exact duration, codec, resolution, frame rate, and audio status.

## Checks
- Run the renderer successfully.
- Inspect representative frames from each shot for composition and continuity.
- Run ffprobe on the final output.
- Do not claim professional quality without visual inspection.
