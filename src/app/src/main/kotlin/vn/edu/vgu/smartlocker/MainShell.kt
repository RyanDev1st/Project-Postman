package vn.edu.vgu.smartlocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.AppBar
import vn.edu.vgu.smartlocker.ui.AppBarBead
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.BottomNav
import vn.edu.vgu.smartlocker.ui.ScanButton
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * The three-tab shell: the app bar, the content, and the glass nav floating
 * over it. The nav floats — content runs under the chrome, which is what the
 * material is for: a lens with no backdrop to bend is a plain rectangle.
 *
 * App chrome, not feature code, which is why it sits beside MainActivity.
 */
@Composable
fun MainShell(
    screen: Screen,
    dark: Boolean,
    onToggleDark: () -> Unit,
    onSelectTab: (Screen) -> Unit,
    content: @Composable () -> Unit,
) {
    val t = LocalLockerTokens.current
    // Insets and the gutter belong to MainActivity, which wraps every screen.
    // Repeating them here inset the tab screens twice.
    Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabAppBar(screen = screen, onToggleDark = onToggleDark)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                ) {
                    content()
                }
            }

            // The nav floats over content, with the content visible through
            // the glass — the cabinet render runs down under it.
            BottomNav(
                selected = tabIndex(screen),
                onSelect = { onSelectTab(tabOf(it)) },
                icons = listOf(AppIcons.Home, AppIcons.Cabinet, AppIcons.Settings),
                labels = listOf("HOME", "CABINET", "SETTINGS"),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = 12.dp),
            )
    }
}

/**
 * How much room a screen must leave at its foot so its last control is not
 * covered by the floating nav.
 *
 * The nav deliberately floats over the content — glass with nothing behind
 * it is a plain rectangle. That is fine for a render or a map, which are
 * only being looked at, and wrong for a button. The Cabinet screen's "Scan
 * to open" sat half under the bar and "Use a code" was hidden completely.
 *
 * Measured from BottomNav: 5dp pane padding, 7dp item padding, a 17dp icon,
 * 2dp gap and the label, and the same again below - about 58dp - plus a
 * gap so a control does not sit right against the glass.
 */
val NavClearance = 70.dp

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val t = LocalLockerTokens.current
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(17.dp),
        tint = t.ink2,
    )
}

private fun tabIndex(s: Screen): Int = when (s) {
    Screen.HOME -> 0
    Screen.CABINET -> 1
    Screen.SETTINGS -> 2
    else -> 0
}

private fun tabOf(i: Int): Screen = when (i) {
    0 -> Screen.HOME
    1 -> Screen.CABINET
    else -> Screen.SETTINGS
}

@Composable
private fun TabAppBar(
    screen: Screen,
    onToggleDark: () -> Unit,
) {
    when (screen) {
        Screen.HOME -> AppBar(
            brand = "Chào Minh",
            leading = {
                AppBarBead(icon = AppIcons.Menu, contentDescription = "Menu", onClick = {})
            },
            actions = {
                ScanButton(icon = AppIcons.Scan, contentDescription = "Scan a cabinet", onClick = {})
                AppBarBead(icon = AppIcons.Bell, contentDescription = "Notifications", onClick = {})
            },
        )

        Screen.CABINET -> AppBar(
            brand = "Back gate",
            leading = {
                AppBarBead(icon = AppIcons.Back, contentDescription = "Back", onClick = {})
            },
            actions = {
                AppBarBead(icon = AppIcons.Bell, contentDescription = "Notifications", onClick = {})
            },
        )

        Screen.SETTINGS -> AppBar(
            brand = "Settings",
            leading = {
                AppBarBead(icon = AppIcons.Back, contentDescription = "Back", onClick = {})
            },
        )

        else -> {}
    }
}
