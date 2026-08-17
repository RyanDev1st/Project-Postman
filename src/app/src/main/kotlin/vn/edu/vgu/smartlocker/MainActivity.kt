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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import vn.edu.vgu.smartlocker.net.Backend
import vn.edu.vgu.smartlocker.net.Http
import vn.edu.vgu.smartlocker.auth.CodeScreen
import vn.edu.vgu.smartlocker.auth.SignInScreen
import vn.edu.vgu.smartlocker.cabinet.CabinetScreen
import vn.edu.vgu.smartlocker.cabinet.YourDoor
import vn.edu.vgu.smartlocker.loading.LoadingScreen
import vn.edu.vgu.smartlocker.parcels.HomeScreen
import vn.edu.vgu.smartlocker.parcels.rememberHome
import vn.edu.vgu.smartlocker.pickup.OpenedScreen
import vn.edu.vgu.smartlocker.pickup.Pickup
import vn.edu.vgu.smartlocker.pickup.ScanScreen
import vn.edu.vgu.smartlocker.pickup.TypeCodeScreen
import vn.edu.vgu.smartlocker.pickup.whatToDo
import vn.edu.vgu.smartlocker.settings.SettingsScreen
import vn.edu.vgu.smartlocker.ui.AppLanguage
import vn.edu.vgu.smartlocker.ui.AppLanguageProvider
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
    SIGN_IN, CODE,
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

            // The phone's language, if the app speaks it. Unlike the scheme
            // above, following the system is right here: a person whose phone
            // is in Vietnamese has already said which language they read.
            //
            // Held in memory, like `dark`, so it lasts as long as the process
            // and no longer. Neither setting is written down yet — that wants
            // one store for both, and the app has none.
            var language by remember { mutableStateOf(AppLanguage.ofSystem()) }

            // Outside the theme, because it decides what the words ARE and
            // the theme only decides what they look like.
            AppLanguageProvider(language) {
                // The wipe owns the toggle: it has to hold the outgoing frame
                // before the theme flips, so it cannot be told after the fact.
                ThemeWipe(dark = dark, onDarkChanged = { dark = it }) { requestToggle ->
                    SmartLockerTheme(dark = dark) {
                        AppSkeleton(
                            dark = dark,
                            onToggleDark = requestToggle,
                            language = language,
                            onLanguage = { language = it },
                            start = start,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppSkeleton(
    dark: Boolean,
    onToggleDark: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
    onLanguage: (AppLanguage) -> Unit = {},
    /** Debug-only starting screen — see [MainActivity.onCreate]. */
    start: Screen? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // One of these, for the life of the process. It holds the token store and
    // the settings, and both read files - remaking it on every recomposition
    // would re-read them thirty times a second.
    val backend = remember { Backend(context) }

    // A phone that has signed in before opens on Home.
    //
    // `signedIn` existed and nothing read it, so every launch asked for a
    // phone number again - including after an update - and the token store
    // built in P1-06 may as well not have been there. If the token has since
    // expired the first call says so, which is the honest failure.
    var screen by remember {
        mutableStateOf(start ?: if (backend.signedIn) Screen.HOME else Screen.SIGN_IN)
    }
    var lastMain by remember { mutableStateOf(Screen.HOME) }

    // Which box the server said it opened. Not a guess, and not a default:
    // it is written the moment endpoint 6 answers and read only by the
    // "Opened" screen. It used to start at "04", which is how that screen
    // came to announce box 04 whatever had actually been unlocked.
    var openedBox by remember { mutableStateOf("") }

    /** Which box the typed-code screen is about, chosen on the Cabinet tab. */
    var codeBox by remember { mutableStateOf("") }

    // A sentence for a code that was read and not acted on. Null nearly
    // always; the scan screen shows "Scan the cabinet" instead.
    var scanNote by remember { mutableStateOf<Int?>(null) }

    // The same, for the two sign-in screens, plus whether a call is in flight.
    var authNote by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }

    // A build with nowhere to send a request says so on the first screen,
    // rather than letting somebody type a number and wait for a timeout.
    val noAddress = if (backend.hasAddress) null else R.string.no_server_address

    // The number being signed in with, local part — see `VnMobile`. Held here
    // rather than in SignInScreen because the code screen has to show it, and
    // going back to correct a typo must not lose it.
    //
    // In memory only. Nothing about signing in is written down yet: the token
    // store exists (P1-06) but there is no server to mint a token, so there is
    // nothing to keep.
    var number by remember { mutableStateOf("") }

    fun gotoMain(tab: Screen) {
        lastMain = tab
        screen = tab
    }

    // Arriving at the scanner always arrives at a clean one. A sentence left
    // over from a code somebody scanned five minutes ago is about a cabinet
    // they may not even be standing at now.
    //
    // It takes no box any more. Tapping a particular parcel on Home used to
    // choose which door this screen would open; the server chooses now, from
    // the cabinet the scanned code names.
    fun gotoScan() {
        scanNote = null
        screen = Screen.SCAN
    }

    /**
     * Turn one answer from the server into one sentence, or a way onward.
     *
     * [Http.Answer.Unclear] never counts as success. The request may well have
     * arrived - the network dropped after it went out - so the honest answer
     * is that we do not know, and rule 4 in `architecture.md` says to say so.
     */
    fun <T> sentenceFor(answer: Http.Answer<T>): Int = when (answer) {
        is Http.Answer.Ok -> R.string.working
        is Http.Answer.Unclear -> R.string.unclear_result
        is Http.Answer.Refused -> answer.reason.message ?: R.string.refused_unknown
    }

    BackHandler(enabled = screen != Screen.SIGN_IN) {
        screen = when (screen) {
            Screen.CODE -> Screen.SIGN_IN
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
                // Every screen but one. "Opened" is the single full-bleed
                // screen in the app - amber to all four edges is the whole
                // point of it, readable across a corridor - so the shared
                // gutter would leave it a coloured card floating on the
                // ground, which is what it looked like. It keeps the system
                // bars clear itself, on its content rather than its colour.
                .then(
                    if (screen == Screen.OPENED) Modifier
                    else Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ),
        ) {
            when (screen) {
                Screen.SIGN_IN -> SignInScreen(
                    number = number,
                    onNumberChange = { number = it },
                    // Endpoint 1. The code screen is only reached if the
                    // server says it sent something - going there on a failed
                    // send would leave somebody waiting for a text that is
                    // never coming, with six empty boxes in front of them.
                    onSendCode = {
                        if (!backend.hasAddress) return@SignInScreen
                        busy = true
                        authNote = null
                        scope.launch {
                            val answer = backend.requestCode(number)
                            busy = false
                            if (answer is Http.Answer.Ok) screen = Screen.CODE
                            else authNote = sentenceFor(answer)
                        }
                    },
                    note = authNote ?: noAddress,
                    busy = busy,
                )
                Screen.CODE -> CodeScreen(
                    // Endpoint 2. On success the token is already in the
                    // phone's secure store - see Api.verifyCode - and nothing
                    // here ever holds it.
                    onDone = { typed, fullName ->
                        busy = true
                        authNote = null
                        scope.launch {
                            val answer = backend.verifyCode(number, typed, fullName)
                            busy = false
                            if (answer is Http.Answer.Ok) gotoMain(Screen.HOME)
                            else authNote = sentenceFor(answer)
                        }
                    },
                    onBack = { screen = Screen.SIGN_IN; authNote = null },
                    number = number,
                    note = authNote,
                    busy = busy,
                )

                Screen.HOME -> MainShell(
                    screen = screen,
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onSelectTab = ::gotoMain,
                    onScan = { gotoScan() },
                    content = {
                        // Reloaded when Home is arrived at, which includes
                        // coming back from a collect - so a parcel that has
                        // just been taken out stops being listed.
                        val home = rememberHome(backend, reloadKey = openedBox)
                        HomeScreen(
                            parcels = home.parcels,
                            second = home.second,
                            ledger = home.ledger,
                            note = home.trouble?.let(::sentenceFor),
                            onOpen = { gotoScan() },
                            onOpenSecond = { gotoScan() },
                            onMap = {},
                        )
                    },
                )

                Screen.CABINET -> MainShell(
                    screen = screen,
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onSelectTab = ::gotoMain,
                    onScan = { gotoScan() },
                    content = {
                        // The same fetch Home makes, shown as doors. The tab
                        // used to build two parcels of its own - box 04 and
                        // box 07 - which is why it never named the box Home
                        // named.
                        val cab = rememberHome(backend, reloadKey = openedBox)
                        CabinetScreen(
                            onScan = { gotoScan() },
                            onTypeCode = { codeBox = it; screen = Screen.TYPE_CODE },
                            yours = cab.doors.map {
                                YourDoor(it.box, it.at, it.left, it.pct, it.soon)
                            },
                        )
                    },
                )

                Screen.SETTINGS -> MainShell(
                    screen = screen,
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onSelectTab = ::gotoMain,
                    onScan = { gotoScan() },
                    content = {
                        SettingsScreen(
                            dark = dark,
                            onToggleDark = onToggleDark,
                            language = language,
                            onLanguage = onLanguage,
                        )
                    },
                )

                Screen.SCAN -> ScanScreen(
                    onTypeCode = { screen = Screen.TYPE_CODE },
                    onBack = { screen = lastMain },
                    note = scanNote,
                    // **The product.** A code is read, the app decides only
                    // whether it is worth asking about, and the SERVER decides
                    // whether a door moves and which one.
                    //
                    // Until P5-03 this branch chose the box itself, from a
                    // list of parcels the app was carrying. That list is gone.
                    // It could never have protected anything - it ran on the
                    // caller's phone - and it was wrong as often as it was
                    // stale.
                    onRead = { read, raw ->
                        when (val next = whatToDo(read, raw)) {
                            Pickup.ScanAgain -> scanNote = R.string.refused_session_expired
                            Pickup.KeepLooking -> Unit
                            is Pickup.Ask -> {
                                // Exactly one request per code. `Aperture`
                                // drops every later frame carrying the same
                                // string, so this cannot fire twice for one
                                // scan - and it is never retried, because a
                                // retry can open a door nobody is standing at.
                                scanNote = R.string.working
                                scope.launch {
                                    when (val answer = backend.collect(next.code)) {
                                        is Http.Answer.Ok -> {
                                            openedBox = answer.value.boxNumber
                                            screen = Screen.OPENED
                                        }
                                        // Not "failed", and never "opened".
                                        // The request may have arrived.
                                        else -> scanNote = sentenceFor(answer)
                                    }
                                }
                            }
                        }
                    },
                )

                Screen.OPENED -> OpenedScreen(
                    // The server's answer, never the app's guess.
                    box = openedBox,
                    onDone = { screen = lastMain },
                    onBack = { screen = lastMain },
                )

                // No `onAccepted`. It went straight to the "opened" screen
                // without a request, so the app announced a door it had never
                // asked anybody to open. See TypeCodeScreen.
                Screen.TYPE_CODE -> TypeCodeScreen(
                    box = codeBox,
                    onScan = { gotoScan() },
                    onBack = { screen = lastMain },
                )
            }
        }
    }
}
