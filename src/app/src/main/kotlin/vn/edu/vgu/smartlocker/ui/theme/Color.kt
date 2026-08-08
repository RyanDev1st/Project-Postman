package vn.edu.vgu.smartlocker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Powder-coated steel, and one door light.
 *
 * The ground is the cabinet: cool grey with a blue bias, the colour of the
 * painted metal the receiver is walking toward. It stays quiet on every
 * screen so that the one saturated colour in the app means something.
 *
 * That colour is [DoorLight]. It appears on exactly one screen - the moment
 * a door opens - and it fills it. A person holding the phone at arm's length
 * in a corridor can tell that screen from every other one without reading a
 * word.
 */

// --- steel, light ---------------------------------------------------------

val SteelInk        = Color(0xFF12181C)   // text on the ground
val SteelInkSoft    = Color(0xFF4C575F)   // second-rank text, tinted from the ground, never grey
// A real mid-steel, not a near-white. The glass card is white; if the ground
// is white too there is nothing for the card to lift off, and the depth the
// brief asks for cannot exist. Still bright enough to read in Vietnamese sun.
val SteelGround     = Color(0xFFD5DDE4)   // the app background
val SteelSurface    = Color(0xFFFFFFFF)   // a raised card
val SteelSurfaceAlt = Color(0xFFE4EAEE)   // a recessed field
val SteelOutline    = Color(0xFF97A3AB)
val SteelPrimary    = Color(0xFF1E2A32)   // the one button that matters
val SteelOnPrimary  = Color(0xFFF6F9FA)

// --- steel, dark ----------------------------------------------------------

val NightInk        = Color(0xFFE7EDF1)
val NightInkSoft    = Color(0xFF97A5AE)
val NightGround     = Color(0xFF0D1114)
val NightSurface    = Color(0xFF171E23)
val NightSurfaceAlt = Color(0xFF212B31)
val NightOutline    = Color(0xFF46525A)
val NightPrimary    = Color(0xFFC5D5DE)
val NightOnPrimary  = Color(0xFF10191F)

// --- the door light -------------------------------------------------------

/**
 * The colour of a locker indicator, and the only saturated field in the app.
 *
 * It is the same in both schemes on purpose. A door being open is a fact
 * about the world, not about the phone's settings, so it does not dim at
 * night - and the corridor it is read in is dark either way.
 */
val DoorLight   = Color(0xFFFFB200)
val OnDoorLight = Color(0xFF241A00)

// --- a free box -----------------------------------------------------------

/**
 * A door nobody has taken, on the cabinet map, and nowhere else.
 *
 * This is the app's second colour, and it exists under one condition: it can
 * never share a screen with [DoorLight]. Free doors are drawn only when the
 * receiver has nothing waiting; the amber is drawn only when they have. The
 * two states cannot both be true, so the "one saturated colour" rule survives
 * as the rule that matters - one colour at a time. See ADR 0011.
 *
 * Unlike the amber it has a light and a dark value. A free box is a fact
 * about the cabinet, not about a door standing open in a dark corridor, and
 * it is read indoors on a phone the receiver is already looking at.
 */
val FreeBoxLight = Color(0xFF1F7A5A)
val FreeBoxDark  = Color(0xFF57C79B)

// --- refusal --------------------------------------------------------------

/** A wrong code, a faulty box. Legible on both grounds. */
val RefusalLight = Color(0xFFB3261E)
val RefusalDark  = Color(0xFFFFB4AB)
