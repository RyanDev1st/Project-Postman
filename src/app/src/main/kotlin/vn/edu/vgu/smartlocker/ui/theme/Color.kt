package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The token sets, ported from the design mock-up's source of truth
 * (`docs/designs/mockup/screens.css`, lines 46–151).
 *
 * **Dark leads.** The light scheme is a second design, not an inversion, and
 * it breaks the 4-to-8 L* rule on purpose: white on `#D3DBE3` is +13 L*.
 * A card cannot go higher than white, so on the light side the separation is
 * made by pulling the *ground* down to a real mid-steel.
 *
 * Surfaces are **picked, never mixed**. Mixing the ground with ink dilutes
 * the blue bias by four fifths and lifts a panel 38 L* — a second background.
 * The picked values sit 4–8 L* up and stay more saturated for being lifted,
 * which is what a real material does when a light finds it.
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
    ground = Color(0xFF090B0E),
    ground2 = Color(0xFF0F141A),
    surface = Color(0xFF161D27),
    surface2 = Color(0xFF10161E),
    field = Color(0x8C02050A), // rgba(2,5,10,.55)
    hair = Color(0x2196BAE8),  // rgba(150,186,232,.13)
    lip = Color(0x33B0D0FF),   // rgba(176,208,255,.20)
    glass = Color(0x0EFFFFFF), // rgba(255,255,255,.055)
    glass2 = Color(0x15FFFFFF),// rgba(255,255,255,.085)
    glassEdge = Color(0x21A0C4F0), // rgba(160,196,240,.13)
    glassLip = Color(0x3DB0D0FF),  // rgba(176,208,255,.24)
    ink = Color(0xFFE8EDF4),
    ink2 = Color(0xFF8E9AA8),
    ink3 = Color(0xFF798591), // 5.23:1 on the ground, 4.50:1 on a card
    accent = Color(0xFFF2F7FF),     // 18.3:1 on the ground, brighter than ink
    accentDeep = Color(0xFFE7EEFA),  // the slab, pulled just under the marks
    accentInk = Color(0xFFF2F7FF),
    onAccent = Color(0xFF0A0E14),    // 16.6:1 on the slab
    free = Color(0xFF3FD69A),
    refuse = Color(0xFFFF9B92),
    shadow = Color(0x9E000000), // rgba(0,0,0,.62)
    bezel = Color(0xFF171E25),
    recessIn = Color(0x8C000000), // rgba(0,0,0,.55)
    recessLit = Color(0x12B4D6FF),// rgba(180,214,255,.07)
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
    ground = Color(0xFFD3DBE3),
    ground2 = Color(0xFFE8EEF3),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF3F7FB),
    field = Color(0xFFC6D0DA), // 13 points DOWN from the ground — a recess must
    hair = Color(0x1A0E2030),  // rgba(14,32,48,.10)
    lip = Color(0xE6FFFFFF),   // rgba(255,255,255,.9)
    glass = Color(0xB3FFFFFF), // rgba(255,255,255,.7)
    glass2 = Color(0xE0FFFFFF),// rgba(255,255,255,.88)
    glassEdge = Color(0xF2FFFFFF), // rgba(255,255,255,.95)
    glassLip = Color(0xFFFFFFFF),
    ink = Color(0xFF0F161B),
    ink2 = Color(0xFF4C5862),
    ink3 = Color(0xFF54616C), // 4.54:1 on the ground, 6.36:1 on a card
    accent = Color(0xFF0A121B),      // 13.5:1 on the ground, 18.8:1 on a card
    accentDeep = Color(0xFF14202C),  // the slab, lifted just off the marks
    accentInk = Color(0xFF0A121B),
    onAccent = Color(0xFFF4F8FE),    // 15.5:1 on the slab
    free = Color(0xFF0B6B49), // 4.67:1 on the light ground, 6.53:1 on white
    refuse = Color(0xFFB3261E),
    shadow = Color(0x380C1824), // rgba(12,24,36,.22)
    bezel = Color(0xFFAFBAC4),
    recessIn = Color(0x260E2030),  // rgba(14,32,48,.15)
    recessLit = Color(0xF2FFFFFF), // rgba(255,255,255,.95)
    dark = false,
)

val DarkTokens = Dark
val LightTokens = Light
