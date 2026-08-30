package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The token sets, ported from the design mock-up's source of truth
 * (`docs/designs/mockup/screens.css`, lines 46–151).
 *
 * **One hue, hue 210, and very little of it.** Every neutral in both schemes
 * is that same cold steel, two points of channel spread between red and
 * blue. Nothing here is a pure grey and nothing is a pure white: the light
 * card is `#FAFBFC`, so it carries the same trace of steel as the ground it
 * sits on.
 *
 * **Small steps.** Ground to card is +4.3 L points in dark and +3.5 in light.
 * Measured against the two published systems this is drawn from - Radix Slate
 * steps 2.9 in its dark scheme, Vercel's Geist 6.3 in its dark - that is the
 * whole range. It used to be **14.1** in light, which is not a step but a
 * stage: the ground had been pulled down to 85.9% L so a pure white card
 * would pop off it, and the result read as a grey backdrop with paper on it
 * rather than as one material.
 *
 * The saturation came down with it. These neutrals ran 20 to 28 percent;
 * Radix Slate, which exists to be a cold grey, runs 6 to 20. At 28 percent
 * the ground was a blue, and a blue ground is a colour - which breaks the
 * rule below before any component gets a chance to. ADR 0025.
 *
 * Amber is identical in both schemes: a door being open is a fact about the
 * world, not about the phone's settings.
 *
 * **Colour means something.** Amber says *this door is yours*, [LockerTokens.free]
 * says a box is empty, [LockerTokens.refuse] says something was turned down.
 * Nothing else is coloured: the accent is the end of the value scale, near
 * white on the dark ground and near black on the pale one.
 *
 * It used to be a hue, and a different hue in each scheme — `#7FA8FF` dark,
 * `#C2410C` light. That cost the product twice. A brand nobody could picture,
 * because the only colour on screen changed when the sun went down. And a
 * burnt orange 25 degrees off the door light, which the note below defended
 * by darkness alone and which reads on a phone as one warm family with the
 * one colour the product has to teach. ADR 0024.
 *
 * The cold bias is amber's, not ours. `#FFB200` is hue 42 and warm, so on a
 * cool field it is the warmest thing on the screen and it carries. On a warm
 * field it goes muddy, which is exactly what happened to the burnt orange.
 */

/** A full token set for one scheme. Names carry over from the mock-up. */
data class LockerTokens(
    val ground: Color,      // the app background
    val ground2: Color,     // one step off the ground — the map's paper
    val surface: Color,     // a raised card
    val surface2: Color,    // the second surface, for Material's own slots
    val field: Color,       // set INTO the ground — inputs, code boxes, chips
    val hair: Color,        // a card's border, and every rule
    val lip: Color,         // the lit top edge of a card
    val glass: Color,       // floating chrome — the nav, the app-bar buttons
    val glass2: Color,      // a brighter pane — the selected tab
    val glassEdge: Color,   // the glass pane's stroke
    val glassLip: Color,    // the light catching the top of a pane
    val ink: Color,         // text
    val ink2: Color,        // second-rank text
    val ink3: Color,        // captions and disabled
    val accent: Color,      // the emphasis, wherever it is large or on a card
    val accentDeep: Color,  // the filled button
    val accentInk: Color,   // small accent text on the ground
    val onAccent: Color,    // what sits ON accentDeep - a label, a switch knob
    val free: Color,        // a box nobody has taken
    val refuse: Color,      // a wrong code, a faulty box
    val shadow: Color,      // what a raised thing casts
    val bezel: Color,       // the phone's own frame, inside the app
    val recessIn: Color,    // the ground's shade thrown inward
    val recessLit: Color,   // the lit return along a recess's bottom lip

    /**
     * Which scheme this set is. Carried here because glass is the one
     * material whose recipe changes rather than its colours — a lit pane
     * over a dark ground, a tinted one over a pale ground — and asking
     * `isSystemInDarkTheme()` for that answers the phone's question, not
     * the app's. With the in-app toggle those two disagree, and the nav
     * and the app-bar beads were left in the wrong scheme.
     */
    val dark: Boolean,
)

// --- the door light -------------------------------------------------------

/**
 * The colour of a locker indicator, and the only saturated colour that means
 * one thing: **this door is yours**.
 *
 * Identical in both schemes on purpose. Two places only: the highlight on the
 * cabinet, and the Opened screen, which it fills.
 *
 * Amber is a surface, never ink: `#FFB200` measures 1.81:1 on white. When it
 * has to sit on pale ground it sits on its own dark ground `#241A00` (11.5:1)
 * as a pill or a bar — see [LockerTokens.amberGround].
 */
val DoorLight = Color(0xFFFFB200)
val OnDoorLight = Color(0xFF241A00)

// --- dark, the default ----------------------------------------------------

private val Dark = LockerTokens(
    ground = Color(0xFF101214),  // 7.1% L - graphite, not black
    ground2 = Color(0xFF151719),
    surface = Color(0xFF1A1D20),  // +4.3 L points off the ground, and no more
    surface2 = Color(0xFF17191C),
    field = Color(0xFF0B0D0E),  // the one thing set INTO the ground
    hair = Color(0x14A8BACC),  // rgba(168,186,204,.08) - rules, not outlines
    lip = Color(0x1FB0C4DC),  // rgba(176,196,220,.12)
    glass = Color(0x0EFFFFFF), // rgba(255,255,255,.055)
    glass2 = Color(0x15FFFFFF),// rgba(255,255,255,.085)
    glassEdge = Color(0x21A0C4F0), // rgba(160,196,240,.13)
    glassLip = Color(0x3DB0D0FF),  // rgba(176,208,255,.24)
    ink = Color(0xFFECEDEF),  // 16.03:1 on the ground
    ink2 = Color(0xFF969DA6),  // 6.86:1
    ink3 = Color(0xFF7D858E),  // 5.02:1 on the ground, 4.53:1 on a card
    accent = Color(0xFFF4F5F6),
    accentDeep = Color(0xFFE6E8EA),  // the slab
    accentInk = Color(0xFFF4F5F6),
    onAccent = Color(0xFF101214),  // 15.28:1 on the slab
    free = Color(0xFF3FD69A),  // 10.1:1
    refuse = Color(0xFFFF9B92),  // 9.26:1
    shadow = Color(0x7A000000),  // rgba(0,0,0,.48) - one soft shadow, no rim
    bezel = Color(0xFF191C1F),
    recessIn = Color(0x66000000),  // rgba(0,0,0,.40)
    recessLit = Color(0x0FB4C8DC),  // rgba(180,200,220,.06)
    dark = true,
)

// --- light, the second design ---------------------------------------------

/**
 * Light is the same design with the value scale turned over. Ink goes dark,
 * the ground comes down to mid-steel, and the accent follows ink to the end
 * of the scale rather than flipping to a second hue.
 *
 * `accentInk` survives with the same value as `accent`. It was a third orange,
 * two points darker, bought to carry small text at 5.17:1 on the ground; at
 * 13.5:1 there is nothing left for it to fix. It is still called in four
 * places and still means *accent, small, on the ground*, so it stays as a
 * name rather than becoming a rename across four files.
 */
private val Light = LockerTokens(
    ground = Color(0xFFF0F2F4),  // 94.9% L - paper, not a stage
    ground2 = Color(0xFFE9ECEF),
    surface = Color(0xFFFAFBFC),  // +3.5 L points, and it carries the hue
    surface2 = Color(0xFFF6F7F9),
    field = Color(0xFFE5E8EB),  // 4 points DOWN from the ground - a recess must
    hair = Color(0x140E2030),  // rgba(14,32,48,.08) - rules, not outlines
    lip = Color(0x99FFFFFF),  // rgba(255,255,255,.60)
    glass = Color(0xB3FFFFFF), // rgba(255,255,255,.7)
    glass2 = Color(0xE0FFFFFF),// rgba(255,255,255,.88)
    glassEdge = Color(0xF2FFFFFF), // rgba(255,255,255,.95)
    glassLip = Color(0xFFFFFFFF),
    ink = Color(0xFF191E24),  // 14.94:1 on the ground
    ink2 = Color(0xFF5C6570),  // 5.27:1
    ink3 = Color(0xFF676F7B),  // 4.52:1 on the ground, 4.90:1 on a card
    accent = Color(0xFF13181E),
    accentDeep = Color(0xFF1D232B),  // the slab
    accentInk = Color(0xFF13181E),
    onAccent = Color(0xFFFAFBFC),  // 15.27:1 on the slab
    free = Color(0xFF0B6B49),  // 5.82:1 on the ground, 6.31:1 on a card
    refuse = Color(0xFFB3261E),  // 5.82:1
    shadow = Color(0x1F0C1824),  // rgba(12,24,36,.12) - one soft shadow
    bezel = Color(0xFFC8CED4),
    recessIn = Color(0x140E2030),  // rgba(14,32,48,.08)
    recessLit = Color(0xB3FFFFFF),  // rgba(255,255,255,.70)
    dark = false,
)

val DarkTokens = Dark
val LightTokens = Light
