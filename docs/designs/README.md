Here lives design wireframes of the mobile app for this project.

## What is here

| Folder | What it is |
| --- | --- |
| `wireframe/` | Early wireframes |
| `mockup/` | The working mock-up every screen was drawn in. `build.py` assembles it; `map.py` regenerates the campus plan from `route.json` |

## The typeface

**Geist** and **Geist Mono**, named in [DESIGN.md](../../DESIGN.md) — Geist for everything, Geist Mono for numbers, because box numbers, times and codes have to line up and change in place without the row reflowing.

Both live here as `.woff2` for the mock-up, which inlines them as base64. The app ships the same two faces converted to `.ttf` in `src/app/src/main/res/font/`, because Android cannot read woff2. They are **variable** fonts, one `wght` axis from 100 to 900, so one file covers every weight.

Geist is Vercel's, under the **SIL Open Font License 1.1**, which permits bundling it in an application. Keep that licence with the files if they ever move.
