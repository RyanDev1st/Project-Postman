Parent: none

# Design language audit — why the app reads cheap

## Status

Done, measured, not yet fixed. Four faults found. One is an accessibility
failure, one is a hierarchy inversion, two are missing systems.

The glass is not the problem any more. It is a measured port of
`rdev/liquid-glass-react` and it behaves like the source. The cheapness that
is left comes from everything around it.

**The finding in one line:** nothing in this app agrees with anything else in
it. There are three body sizes half a point apart, twelve corner radii, and
forty-two spacing values. No single one of those is visible. Their absence of
agreement is.

## Scope

Every Kotlin file under `src/app/src/main/kotlin` — 39 files, comments
stripped before measuring so a number in prose never counts as a number in
code. Measured: `dp` literals, `RoundedCornerShape` radii, the type scale in
`Type.kt`, and the colour tokens in `Color.kt` against WCAG 2.2 SC 1.4.3.

## Evidence

### 1. `ink3` fails WCAG AA in both schemes — the one outright defect

Contrast ratios, computed to WCAG 2.2 (sRGB relative luminance):

| scheme | token | on ground | on surface | required |
| --- | --- | --- | --- | --- |
| dark | `ink` `FFE8EDF4` | 16.75 | — | 4.5 OK |
| dark | `ink2` `FF8E9AA8` | 6.88 | — | 4.5 OK |
| dark | **`ink3` `FF5C6874`** | **3.46** | **2.98** | **4.5 FAIL** |
| light | `ink` `FF0F161B` | 13.04 | — | 4.5 OK |
| light | `ink2` `FF4C5862` | 5.21 | — | 4.5 OK |
| light | **`ink3` `FF7A8792`** | **2.63** | **3.68** | **4.5 FAIL** |

`ink3` is not decoration. It carries the nav bar's unselected labels
(CABINET, SETTINGS at 9.5sp) and the small chips — small text, so 4.5:1
applies, not the 3:1 large-text allowance. It is used in 8 files.

Washed-out secondary text is one of the most reliable tells of an unfinished
interface, and here it is also a standards failure.

Solved values that clear 4.5:1 against both ground and surface, moving along
each token's own hue:

| scheme | now | proposed | on ground | on surface |
| --- | --- | --- | --- | --- |
| dark | `FF5C6874` | `FF798591` | 5.23 | 4.50 |
| light | `FF7A8792` | `FF54616C` | 4.54 | 6.36 |

### 2. Type: a title set smaller than body text

Measured from `Type.kt`, in size order, with the ratio to the size below it:

| style | size | line | ratio to prev | lh/fs |
| --- | --- | --- | --- | --- |
| labelSmall | 9.5 | 13.0 | — | 1.37 |
| labelMedium | 10.0 | 14.0 | **1.053** | 1.40 |
| labelLarge | 12.5 | 17.0 | 1.250 | 1.36 |
| bodyMedium | 14.0 | 20.0 | **1.120** | 1.43 |
| **titleLarge** | **14.5** | 20.0 | **1.036** | 1.38 |
| **bodyLarge** | **15.0** | 21.0 | **1.034** | 1.40 |
| headlineMedium | 25.0 | 28.5 | **1.667** | 1.14 |
| headlineLarge | 34.0 | 35.4 | 1.360 | 1.04 |
| displaySmall | 40.0 | 40.8 | 1.176 | 1.02 |
| displayMedium | 76.0 | 66.9 | 1.900 | 0.88 |
| displayLarge | 122.0 | 102.5 | 1.605 | 0.84 |

The leading curve is correct and stays — tight at display (0.84), loose at
body (1.43) is exactly right, and no change is proposed to it.

Two faults in the sizes:

- **`titleLarge` (14.5sp) is smaller than `bodyLarge` (15sp).** The app bar
  brand and the user's name are set smaller than reading text. That is a
  hierarchy inversion: the role named "title" is the third-largest text role
  in the file. Nothing downstream can recover a hierarchy the scale does not
  have.
- **Four sizes crowd 12.5–15 at ratios of 1.034–1.120**, then the scale leaps
  1.667 to reach 25. Half a point apart is below the threshold at which a
  reader can tell two sizes apart, so those four roles do not read as four
  ranks — they read as one rank rendered inconsistently. Then there is
  nothing at all between "small" and "heading".

Bringhurst (*The Elements of Typographic Style*) sets text sizes on one
constant ratio, so each step is a step a reader can actually see; Tim Brown's
"More Meaningful Typography" (A List Apart, 2011) is the same rule for
screens. Display sizes may be picked for their job rather than the ratio —
122sp is sized to be read across a corridor, not to fit a progression — and
that part is fine as it is.

`Type.kt`'s own header says the roles are "ported from the mock-up's roles
(`screens.css`)". That is the root cause: the mock-up was eyeballed, and the
port was faithful.

### 3. Corner radii: twelve values against a five-value scale

In use across the app:

```
10(2) 11(1) 12(1) 13(1) 14(3) 15(1) 17(1) 18(2) 19(1) 20(2) 26(1) 999(12)
```

`Shape.kt` defines five: `extraSmall 10, small 13, medium 15, large 17,
extraLarge 20`. So components bypass the scale — and the scale is not much of
a scale either, since 13, 15 and 17 are 2dp apart and indistinguishable at
arm's length.

`Shape.kt`'s own doc comment records the bypass: *"The hero (22), the profile
card (19) and the avatar (14) are set in place where they are drawn."*

Two rules are being broken. A radius set should be small with visible gaps
(Refactoring UI, Wathan & Schoger — "don't use a linear scale"). And nested
corners should be concentric: inner radius = outer radius − padding, which
Apple's HIG now names directly. Neither can hold when a component picks its
own number at the call site.

### 4. Spacing: 42 distinct values, 58% off any grid

```
0(3) 0.5(1) 1(19) 1.5(4)* 2(10) 3(10)* 4(4) 5(5)* 6(1)* 7(3)* 8(13) 8.5(3)*
9(8)* 10(9)* 11(9)* 12(22) 13(7)* 14(11)* 15(8)* 16(6) 17(11)* 18(10)*
19(3)* 20(5) 22(5)* 24(2) 25(1)* 26(6)* 30(2)* 32(1) 34(3)* 38(2)* 40(2)
44(1) 48(1) 52(3) 56(1) 70(3)* 112(1) 132(1) 214(2)* 999(12)*
```

`*` = off a 4dp grid. **On-grid uses: 96. Off-grid uses: 138.**

Material sets a 4dp grid, Apple an 8pt one, and both exist so that repeated
edges land on the same line without anyone checking. Padding of 9, 10 and 11
in three neighbouring components produces edges that miss each other by one
or two pixels everywhere — never enough to notice, always enough to stop the
layout looking cut from one piece.

Nathan Curtis ("Space in Design Systems", 2016) and Refactoring UI both give
the same fix: a short, non-linear set of named steps, and nothing else
allowed.

### Why this is worth doing rather than shipping more features

The aesthetic-usability effect (Kurosu & Kashimura, 1995; later popularised
by Norman) is the finding that users judge a system that looks more finished
as *working better* — the same function, rated more usable. It is why polish
is not decoration on a product a stranger has to trust with a parcel. This
app asks a receiver to believe a locker will open. Consistency is the cheapest
credibility available.

## Next

Ranked by (visible effect) × (risk of the change). The first two are small
and contained; the last two touch many files.

1. **`ink3` in `Color.kt`** — two hex values, fixes an AA failure, no layout
   moves. Values solved above.
2. **`Type.kt`** — put the text sizes on one ratio and fix the inversion.
   Ratio 1.2 (minor third) anchored at the existing body 15sp gives 10.5,
   12.5, 15, 18, 21.5, 26 — and 26 lands on the existing headline 25, so the
   large end barely moves. Collapses 11 roles to 8: label 10.5 (was 9.5 and
   10), labelLarge 12.5 (unchanged), body 15 (was 14 and 15), title 18 (was
   14.5 — the inversion), headline 26 (was 25). Displays unchanged. Leading
   curve unchanged.
3. **`Shape.kt`** — four radii with visible gaps, 10 / 16 / 22 / 28, plus the
   pill. Then remove every inline `RoundedCornerShape` that is not one of
   them, and apply the concentric rule where a card sits inside a card.
4. **Spacing** — a named scale of 2, 4, 8, 12, 16, 24, 32, 48, 64, and snap
   the 138 off-grid uses to it. Largest change, touches all 39 files, and the
   one most likely to need a look on a real phone afterwards. Do it last and
   on its own branch.

Steps 1 and 2 are the ones that change how the app feels for the least risk.
Step 4 is the one that makes it look cut from one piece.
