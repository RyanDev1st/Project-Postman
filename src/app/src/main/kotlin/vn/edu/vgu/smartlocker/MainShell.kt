package vn.edu.vgu.smartlocker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.AppBar
import vn.edu.vgu.smartlocker.ui.AppBarBead
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.BackdropState
import vn.edu.vgu.smartlocker.ui.BottomNav
import vn.edu.vgu.smartlocker.ui.ScanButton
import vn.edu.vgu.smartlocker.ui.backdropSource
import vn.edu.vgu.smartlocker.ui.lockerGround
import vn.edu.vgu.smartlocker.ui.rememberBackdrop
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
    /**
     * The first name to greet, from endpoint 24. Null until the server has
     * answered, and null for good if it never does - the bar then carries the
     * product's name instead. BUG-021: this was the literal "Minh", so every
     * account was greeted as one particular person.
     */
    greetingName: String? = null,
    /** The scanner button in the Home bar. It drew a scanner and did nothing
     * until P5-02, because until P5-02 there was no scanner behind it. */
    onScan: () -> Unit,
    content: @Composable () -> Unit,
) {
    val t = LocalLockerTokens.current
    // What the glass bends: the ground and the content. Both bars are outside
    // it, because a bar inside the recording would be sampling a layer it is
    // part of.
    //
    // The app bar used to be inside, so its beads were given no backdrop at
    // all and fell through to the plain fallback — a flat 30% disc with a ring
    // round it, sitting on the bar in a material nothing else on the screen
    // uses. That is the fault over the top bar. It is also the best place on
    // any screen for glass to be: the accent light in [lockerGround] runs out
    // by 900px, so the top of the screen is the one region with something
    // behind the glass worth bending.
    val backdrop = rememberBackdrop()

    // The bar is drawn over the content now rather than above it in the flow,
    // so the content has to be told how much room to leave. Measured rather
    // than assumed — the bar is a Row around a title and 30dp beads, and its
    // height moves with the text scale.
    var barHeight by remember { mutableIntStateOf(0) }

    // How far this copy of the ground sits below the one [LockerBackdrop]
    // paints across the whole window. Without it the two lights stack inside
    // the gutter and the difference shows as a warm rectangle over the app
    // bar — see [lockerGround].
    var groundTop by remember { mutableFloatStateOf(0f) }

    // Insets and the gutter belong to MainActivity, which wraps every screen.
    // Repeating them here inset the tab screens twice.
    Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // The ground goes INSIDE what the glass samples, and
                    // "inside" is a matter of modifier ORDER. `drawContent()`
                    // draws what comes after it in the chain, so a background
                    // declared before `backdropSource` is painted outside the
                    // recording — and a pane sampling that recording gets a
                    // transparent sheet, which blurs to a transparent sheet.
                    // That is why the bar had no material and the page read
                    // straight through it, sharp.
                    .onGloballyPositioned { groundTop = it.positionInWindow().y }
                    .backdropSource(backdrop)
                    .lockerGround(topInWindow = groundTop),
            ) {
                Spacer(Modifier.height(with(LocalDensity.current) { barHeight.toDp() }))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                ) {
                    // Material's fade-through, at its published numbers: the
                    // outgoing screen fades out over 90ms, the incoming one
                    // fades in over 210ms after it and grows from 92%.
                    //
                    // Tabs are siblings — nothing slides in from anywhere,
                    // because neither is "after" the other and a slide would
                    // claim otherwise. What it buys is that the eye is told
                    // something changed. An instant cut between two dark
                    // pages of similar weight is the single cheapest-feeling
                    // thing an app can do, and it is the one moment the user
                    // triggers deliberately and watches for.
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            (fadeIn(tween(210, delayMillis = 90)) +
                                scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f))
                                .togetherWith(fadeOut(tween(90)))
                        },
                        label = "tab",
                    ) { shown ->
                        // Keyed on the tab so each keeps its own scroll state,
                        // but only the current one is composed.
                        if (shown == screen) content() else Spacer(Modifier.fillMaxSize())
                    }
                }
            }

            TabAppBar(
                screen = screen,
                greetingName = greetingName,
                onToggleDark = onToggleDark,
                onScan = onScan,
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { barHeight = it.height },
            )

            // The nav floats over content, with the content visible through
            // the glass — the cabinet render runs down under it.
            BottomNav(
                selected = tabIndex(screen),
                onSelect = { onSelectTab(tabOf(it)) },
                icons = listOf(AppIcons.Home, AppIcons.Cabinet, AppIcons.Settings),
                labels = listOf(
                    stringResource(R.string.tab_home),
                    stringResource(R.string.tab_cabinet),
                    stringResource(R.string.tab_settings),
                ),
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Down into the gutter, not out of it. The shell keeps
                    // 15dp all round; the nav takes 8 of the bottom 15 and
                    // leaves 7, which is still clear of the gesture bar
                    // because the safe-drawing inset is taken before this.
                    // A floating bar wants more air above it than below —
                    // sat mid-gutter it reads as a thing that failed to
                    // reach the bottom rather than a thing floating over it.
                    .offset(y = 8.dp)
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
    greetingName: String?,
    onToggleDark: () -> Unit,
    onScan: () -> Unit,
    backdrop: BackdropState,
    modifier: Modifier = Modifier,
) {
    when (screen) {
        // The name comes from endpoint 24 and the greeting around it is the
        // app's own word, so it moves with the language. With no name yet
        // the bar says what the app is rather than guessing who you are.
        Screen.HOME -> AppBar(
            brand = greetingName
                ?.let { stringResource(R.string.greeting, it) }
                ?: stringResource(R.string.app_name),
            modifier = modifier,
            leading = {
                AppBarBead(
                    icon = AppIcons.Menu,
                    contentDescription = stringResource(R.string.cd_menu),
                    onClick = {},
                    backdrop = backdrop,
                )
            },
            actions = {
                ScanButton(
                    icon = AppIcons.Scan,
                    contentDescription = stringResource(R.string.cd_scan_cabinet),
                    onClick = onScan,
                )
                AppBarBead(
                    icon = AppIcons.Bell,
                    contentDescription = stringResource(R.string.cd_notifications),
                    onClick = {},
                    backdrop = backdrop,
                )
            },
        )

        Screen.CABINET -> AppBar(
            brand = stringResource(R.string.cabinet_back_gate),
            modifier = modifier,
            leading = {
                AppBarBead(
                    icon = AppIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = {},
                    backdrop = backdrop,
                )
            },
            actions = {
                AppBarBead(
                    icon = AppIcons.Bell,
                    contentDescription = stringResource(R.string.cd_notifications),
                    onClick = {},
                    backdrop = backdrop,
                )
            },
        )

        Screen.SETTINGS -> AppBar(
            brand = stringResource(R.string.title_settings),
            modifier = modifier,
            leading = {
                AppBarBead(
                    icon = AppIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = {},
                    backdrop = backdrop,
                )
            },
        )

        else -> {}
    }
}
