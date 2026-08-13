package vn.edu.vgu.smartlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.auth.AddPhoneScreen
import vn.edu.vgu.smartlocker.auth.CodeScreen
import vn.edu.vgu.smartlocker.auth.SignInScreen
import vn.edu.vgu.smartlocker.cabinet.CabinetScreen
import vn.edu.vgu.smartlocker.loading.LoadingScreen
import vn.edu.vgu.smartlocker.parcels.HomeScreen
import vn.edu.vgu.smartlocker.pickup.OpenedScreen
import vn.edu.vgu.smartlocker.pickup.ScanScreen
import vn.edu.vgu.smartlocker.pickup.TypeCodeScreen
import vn.edu.vgu.smartlocker.settings.SettingsScreen
import vn.edu.vgu.smartlocker.ui.LockerBackdrop
import vn.edu.vgu.smartlocker.ui.ThemeWipe
import vn.edu.vgu.smartlocker.ui.theme.SmartLockerTheme

/**
 * The app's screens, as the design's flow names them.
 *
 * Three tabs (Home, Cabinet, Settings) with the sign-in flow in front of
 * them and the pickup flow reachable from Home or Cabinet.
 */
enum class Screen {
    SIGN_IN, CODE, ADD_PHONE,
    HOME, CABINET, SETTINGS,
    SCAN, OPENED, TYPE_CODE,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Debug builds can be launched straight onto a screen:
        //
        //   adb shell am start -n <pkg>/.MainActivity --es screen HOME
        //
        // This exists for the design-parity loop. RenderEffect and AGSL need a
        // real Android runtime on a GPU, so the glass can only be looked at on
        // a device or an emulator — and the screens worth looking at sit
        // behind a sign-in that needs a server the team does not have yet.
        // Three separate faults in the backdrop reached a tester because
        // there was no way to see it here.
        //
        // It chooses a starting screen and nothing else. No token is minted
        // and no session is claimed, so it cannot stand in for signing in:
        // once the real API is wired, a screen opened this way has no
        // credentials and will fail its first call, which is correct.
        val start = if (BuildConfig.DEBUG) {
            intent?.getStringExtra("screen")
                ?.let { name -> Screen.entries.firstOrNull { it.name == name } }
        } else null

        setContent {
            // Light, whatever the phone is set to.
            //
            // The design is drawn light first and the light page is the one
            // that reads well outdoors — which is where this app is used,
            // standing at a cabinet by the back gate in daylight. Following
            // the phone meant half the team met the app in the scheme it was
            // tuned for second, and a dark first run is also the harsher
            // first impression: the same lens shows far more over near-black
            // ground than over pale steel.
            var dark by remember { mutableStateOf(false) }
            // The wipe owns the toggle: it has to hold the outgoing frame
            // before the theme flips, so it cannot be told after the fact.
            ThemeWipe(dark = dark, onDarkChanged = { dark = it }) { requestToggle ->
                SmartLockerTheme(dark = dark) {
                    AppSkeleton(dark = dark, onToggleDark = requestToggle, start = start)
                }
            }
        }
    }
}

@Composable
fun AppSkeleton(
    dark: Boolean,
    onToggleDark: () -> Unit,
    /** Debug-only starting screen — see [MainActivity.onCreate]. */
    start: Screen? = null,
) {
    var screen by remember { mutableStateOf(start ?: Screen.SIGN_IN) }
    var lastMain by remember { mutableStateOf(Screen.HOME) }
    var scanBox by remember { mutableStateOf("04") }

    fun gotoMain(tab: Screen) {
        lastMain = tab
        screen = tab
    }

    BackHandler(enabled = screen != Screen.SIGN_IN) {
        screen = when (screen) {
            Screen.CODE, Screen.ADD_PHONE -> Screen.SIGN_IN
            Screen.HOME, Screen.CABINET, Screen.SETTINGS -> Screen.SIGN_IN
            Screen.SCAN, Screen.TYPE_CODE -> lastMain
            Screen.OPENED -> lastMain
            else -> Screen.SIGN_IN
        }
    }

    // Every screen stands on the ground, keeps the system bars clear, and
    // gets the same gutter. One place, because the alternative was every
    // screen remembering for itself - and CodeScreen did not, which is why
    // its heading sat on the left edge and the sixth code cell fell off the
    // right one. The tab shell used to repeat both of these, so tab screens
    // were inset twice; it no longer does.
    LockerBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            when (screen) {
                Screen.SIGN_IN -> SignInScreen(
                    onSendCode = { screen = Screen.CODE },
                    onGoogle = { screen = Screen.ADD_PHONE },
                )
                Screen.CODE -> CodeScreen(
                    onDone = { gotoMain(Screen.HOME) },
                    onBack = { screen = Screen.SIGN_IN },
                )

                Screen.ADD_PHONE -> AddPhoneScreen(
                    onSaved = { gotoMain(Screen.HOME) },
                    onSkip = { gotoMain(Screen.HOME) },
                )

                Screen.HOME -> MainShell(
                    screen = screen,
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onSelectTab = ::gotoMain,
                    content = {
                        HomeScreen(
                            onOpen = { scanBox = it; screen = Screen.SCAN },
                            onOpenSecond = { scanBox = it; screen = Screen.SCAN },
                            onMap = {},
                        )
                    },
                )

                Screen.CABINET -> MainShell(
                    screen = screen,
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onSelectTab = ::gotoMain,
                    content = {
                        CabinetScreen(
                            onScan = { scanBox = it; screen = Screen.SCAN },
                            onTypeCode = { screen = Screen.TYPE_CODE },
                        )
                    },
                )

                Screen.SETTINGS -> MainShell(
                    screen = screen,
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onSelectTab = ::gotoMain,
                    content = {
                        SettingsScreen(
                            dark = dark,
                            onToggleDark = onToggleDark,
                        )
                    },
                )

                Screen.SCAN -> ScanScreen(
                    onScanned = { screen = Screen.OPENED },
                    onTypeCode = { screen = Screen.TYPE_CODE },
                    onBack = { screen = lastMain },
                )

                Screen.OPENED -> OpenedScreen(
                    onDone = { screen = lastMain },
                    onBack = { screen = lastMain },
                )

                Screen.TYPE_CODE -> TypeCodeScreen(
                    onAccepted = { screen = Screen.OPENED },
                    onScan = { screen = Screen.SCAN },
                    onBack = { screen = lastMain },
                )
            }
        }
    }
}
