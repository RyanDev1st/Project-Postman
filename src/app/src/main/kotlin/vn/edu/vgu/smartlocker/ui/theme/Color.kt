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
    val accent: Color,      // the colour, wherever it is large or on a card
    val accentDeep: Color,  // the filled button
    val accentInk: Color,   // small accent text on the ground
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
    hair = Color(0x211EBAE8),  // rgba(150,186,232,.13)
    lip = Color(0x33B0D0FF),   // rgba(176,208,255,.20)
    glass = Color(0x0EFFFFFF), // rgba(255,255,255,.055)
    glass2 = Color(0x15FFFFFF),// rgba(255,255,255,.085)
    glassEdge = Color(0x21A0C4F0), // rgba(160,196,240,.13)
    glassLip = Color(0x3DB0D0FF),  // rgba(176,208,255,.24)
    ink = Color(0xFFE8EDF4),
    ink2 = Color(0xFF8E9AA8),
    ink3 = Color(0xFF798591), // 5.23:1 on the ground, 4.50:1 on a card
    accent = Color(0xFF7FA8FF),
    accentDeep = Color(0xFF3D74F0),
    accentInk = Color(0xFF7FA8FF),
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
 * Light leads burnt orange — chosen over a second blue because the cool steel
 * ground has nothing to push against from another blue, and a warm control on
 * cold metal is the reason the scheme exists.
 *
 * Three accent values because orange is lighter than a red at the same chroma
 * and loses contrast against the mid-steel ground: `#C2410C` is 3.70:1 on the
 * ground (large type and fills only), 5.18:1 on a white card, and the small
 * accent text that sits on the ground needs `--accent-ink` `#98380A` at 5.17:1.
 * The button fill is deliberately not darker: at `#98380A` the largest orange
 * area on the screen goes brown.
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
    accent = Color(0xFFC2410C),
    accentDeep = Color(0xFFB83E0B),
    accentInk = Color(0xFF98380A),
    free = Color(0xFF0B6B49), // 4.67:1 on the light ground, 6.53:1 on white
    refuse = Color(0xFFB3261E),
    shadow = Color(0x38181C24), // rgba(12,24,36,.22)
    bezel = Color(0xFFAFBAC4),
    recessIn = Color(0x260E2030),  // rgba(14,32,48,.15)
    recessLit = Color(0xF2FFFFFF), // rgba(255,255,255,.95)
    dark = false,
)

val DarkTokens = Dark
val LightTokens = Light
