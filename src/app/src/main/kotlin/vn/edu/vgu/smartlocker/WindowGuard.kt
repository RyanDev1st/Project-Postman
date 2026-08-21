package vn.edu.vgu.smartlocker

import android.view.WindowManager
import androidx.activity.ComponentActivity

/**
 * Two things the window itself has to refuse.
 *
 * **A tap that something else is covering.** Android lets one app draw on
 * top of another, and a transparent overlay over a button turns "allow
 * this notification" into "open box 07". This app has exactly one control
 * that moves metal, so an obscured touch is never worth acting on;
 * `filterTouchesWhenObscured` makes the framework drop it before any
 * screen sees it. It is one line at the root rather than one per button,
 * because the next button somebody adds would not have it.
 *
 * **A screenshot of somebody's parcels.** The window contents show which
 * boxes are theirs and, on the typed-code path, a code that opens one.
 * `FLAG_SECURE` keeps that out of screenshots, screen recordings and the
 * thumbnail the recents list keeps after the app is closed.
 *
 * `FLAG_SECURE` is release-only, and that is a deliberate trade rather
 * than an oversight: the design-parity loop works by screenshotting debug
 * builds, and a flag that made every one of those come out black would
 * have been removed within the day. Debug builds never hold a real
 * person's parcels.
 */
fun ComponentActivity.guardTheWindow() {
    window.decorView.filterTouchesWhenObscured = true
    if (!BuildConfig.DEBUG) {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }
}
