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
import androidx.compose.runtime.LaunchedEffect
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
import vn.edu.vgu.smartlocker.net.Account
import vn.edu.vgu.smartlocker.net.Backend
import vn.edu.vgu.smartlocker.net.Http
import vn.edu.vgu.smartlocker.net.Refusal
import vn.edu.vgu.smartlocker.auth.CodeScreen
import vn.edu.vgu.smartlocker.auth.PasswordSignInScreen
import vn.edu.vgu.smartlocker.auth.SetPasswordScreen
import vn.edu.vgu.smartlocker.auth.SignInScreen
import vn.edu.vgu.smartlocker.auth.VnMobile
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
    SIGN_IN, CODE, PASSWORD_SIGN_IN,
    HOME, CABINET, SETTINGS, SET_PASSWORD,
    SCAN, OPENED, TYPE_CODE,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        guardTheWindow()

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
    language: AppLanguage = AppLanguage.ofSystem(),
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

    // Endpoint 15, once per launch. Task P1-08.
    //
    // Every number in this build is a guess - how long a QR code lives, how
    // many hours a parcel may sit before the ticket says hurry - and the
    // point of that task is that a wrong guess is corrected by editing one
    // file on the server, not by asking a hundred people to update the app.
    // The plumbing was built on both sides and nothing ever pulled it, so
    // until now every guess was frozen at whatever shipped.
    //
    // It carries no credential, so it runs before sign-in too. It is never
    // retried and a failure is never shown: the last good copy stays, and
    // none of these numbers open a door.
    //
    // The stamp is why Home draws with the corrected numbers on this launch
    // and not the next one. This fetch finishes after Home has already read
    // `pickupCodeHours`, and that is a plain getter over a file, not state
    // Compose watches - so without the stamp a number corrected on the
    // server landed one launch late. Seen: the ticket said 48 hours on the
    // launch that fetched 96, and agreed only on the one after. It moves
    // only when something really changed, so the usual launch, which
    // corrects nothing, redraws nothing.
    var settingsStamp by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { if (backend.refreshSettings()) settingsStamp++ }

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

    // Who is signed in - endpoint 24, BUG-021.
    //
    // Null until the server answers, and null again if it never does. Every
    // screen that shows a name has to handle that, because the alternative
    // is what this replaces: `SettingsScreen` and `MainShell` carried
    // "Minh Nguyen" and "0912 345 678" as parameter defaults, nothing ever
    // passed real ones, and a tester who registered as Tran Thi Mai was
    // greeted by a stranger's name on her own account.
    //
    // Keyed on the screen leaving sign-in rather than on Unit, so the fetch
    // happens again after a sign-in and after a session that expired, not
    // only on the launch that happened to start signed in.
    var account by remember { mutableStateOf<Account?>(null) }
    LaunchedEffect(screen == Screen.HOME || screen == Screen.CABINET || screen == Screen.SETTINGS) {
        if (backend.signedIn && account == null) account = backend.me()
    }

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
    /**
     * End the session and send the person back to register. Once.
     *
     * **The loop this guards against is the whole task.** A token that has
     * run out is noticed by whatever call happened to be in flight - the
     * parcel list, the history, a scan - and each of those could send the
     * user back to sign in on its own. Home reloads on arrival, so a version
     * of this without the guard bounces: Home asks, is refused, goes to sign
     * in, and any other answer still landing does it again.
     *
     * Already on the way in? Then nothing to do. That single line is what
     * makes it happen once. Rule 5 in `architecture.md`.
     *
     * The token is dropped locally and the server is not told. It is the one
     * that refused us; there is nothing to tell it.
     */
    fun endSession() {
        if (screen == Screen.SIGN_IN || screen == Screen.CODE ||
            screen == Screen.PASSWORD_SIGN_IN
        ) return
        scope.launch { backend.forget() }
        number = ""
        busy = false
        authNote = R.string.signed_out_expired
        screen = Screen.SIGN_IN
    }

    fun <T> sentenceFor(
        answer: Http.Answer<T>,
        // What "we do not know" means here, and there is no default.
        //
        // It used to default to the door sentence - an open may have
        // happened, so go and look before trying again - which is nonsense
        // on a screen with no cabinet near it. Ryan read it on the sign-in
        // screen on 2026-08-18 (BUG-014), and again on Home on 2026-08-29
        // when a parcel list failed to load (BUG-025), because that fix was
        // made one call site at a time and Home was not one of them.
        //
        // Required, so the next screen that forgets does not compile.
        unclear: Int,
    ): Int {
        // Every answer from the server passes through here, which is why the
        // check lives here and not in six screens that would each forget it
        // differently.
        if (answer is Http.Answer.Refused && answer.reason == Refusal.TOKEN_EXPIRED) {
            endSession()
        }
        return when (answer) {
            is Http.Answer.Ok -> R.string.working
            is Http.Answer.Unclear -> unclear
            // TOKEN_EXPIRED carries no words on purpose - endSession has
            // already put the right sentence on the sign-in screen, and a
            // second one about tokens would explain plumbing to somebody who
            // was simply away for a month.
            is Http.Answer.Refused -> answer.reason.message ?: R.string.refused_unknown
        }
    }

    BackHandler(enabled = screen != Screen.SIGN_IN) {
        screen = when (screen) {
            Screen.CODE, Screen.PASSWORD_SIGN_IN -> Screen.SIGN_IN
            // Back from setting a password returns to Settings, not out of
            // the app: it is reached from there and nowhere else.
            Screen.SET_PASSWORD -> Screen.SETTINGS
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
                            else authNote = sentenceFor(answer, R.string.unclear_no_server)
                        }
                    },
                    onUsePassword = { authNote = null; screen = Screen.PASSWORD_SIGN_IN },
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
                            else authNote = sentenceFor(answer, R.string.unclear_no_server)
                        }
                    },
                    onBack = { screen = Screen.SIGN_IN; authNote = null },
                    number = number,
                    note = authNote,
                    busy = busy,
                )

                // Endpoint 21. The second way in, and today the only one
                // that works at all - BUG-016 means no Vietnamese one-time
                // code has ever been delivered.
                Screen.PASSWORD_SIGN_IN -> PasswordSignInScreen(
                    number = number,
                    onNumberChange = { number = it },
                    onSignIn = { typed ->
                        busy = true
                        authNote = null
                        scope.launch {
                            val answer = backend.passwordLogin(number, typed)
                            busy = false
                            if (answer is Http.Answer.Ok) gotoMain(Screen.HOME)
                            else authNote = sentenceFor(answer, R.string.unclear_no_server)
                        }
                    },
                    onUseCode = { authNote = null; screen = Screen.SIGN_IN },
                    onBack = { authNote = null; screen = Screen.SIGN_IN },
                    note = authNote,
                    busy = busy,
                )

                // Endpoint 20. Reached from Settings and nowhere else,
                // because it needs a live session - which is exactly what
                // makes it the reset path as well. ADR 0012.
                Screen.SET_PASSWORD -> SetPasswordScreen(
                    onSave = { typed ->
                        busy = true
                        authNote = null
                        scope.launch {
                            val answer = backend.setPassword(typed)
                            busy = false
                            authNote =
                                if (answer is Http.Answer.Ok) R.string.pw_saved
                                else sentenceFor(answer, R.string.unclear_no_server)
                        }
                    },
                    onBack = { authNote = null; screen = Screen.SETTINGS },
                    note = authNote,
                    busy = busy,
                )

                Screen.HOME -> MainShell(
                    screen = screen,
                    // The first name only. The bar has room for one word and
                    // "Hi Tran Thi Mai" is not a greeting.
                    greetingName = account?.name?.trim()?.split(" ")
                        ?.lastOrNull()?.takeIf { it.isNotBlank() },
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onSelectTab = ::gotoMain,
                    onScan = { gotoScan() },
                    content = {
                        // Reloaded when Home is arrived at, which includes
                        // coming back from a collect - so a parcel that has
                        // just been taken out stops being listed.
                        val home = rememberHome(backend, reloadKey = openedBox to settingsStamp)
                        HomeScreen(
                            parcels = home.parcels,
                            second = home.second,
                            ledger = home.ledger,
                            // BUG-025. A list that would not load is a
                            // list that would not load; nothing here opened
                            // anything, so nothing here sends anybody to a
                            // cabinet.
                            note = home.trouble?.let {
                                sentenceFor(it, R.string.unclear_no_server)
                            },
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
                        val cab = rememberHome(backend, reloadKey = openedBox to settingsStamp)
                        CabinetScreen(
                            onScan = { gotoScan() },
                            onTypeCode = { codeBox = it; screen = Screen.TYPE_CODE },
                            yours = cab.doors.map {
                                YourDoor(it.box, it.at, it.left, it.pct, it.soon)
                            },
                            // BUG-024. This was computed and thrown away, so
                            // a dead network and an empty cabinet arrived as
                            // the same empty list and the tab drew the
                            // cheerful one - a green tick and "Room for a
                            // drop", to somebody with a parcel in box 11.
                            note = cab.trouble?.let {
                                sentenceFor(it, R.string.unclear_no_server)
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
                            name = account?.name.orEmpty(),
                            phone = account?.phone?.let(VnMobile::display).orEmpty(),
                            dark = dark,
                            onToggleDark = onToggleDark,
                            language = language,
                            onLanguage = onLanguage,
                            // Endpoint 4. The token is cleared on this phone
                            // whatever the server answers - a person who
                            // tapped Log out has logged out, and leaving the
                            // token behind because the wifi was down is the
                            // one outcome nobody expects. Api.logout keeps
                            // that promise; this only decides where to go.
                            // "Change password" had nothing behind it -
                            // onPassword defaulted to {} and nothing ever
                            // passed one, which is the same shape as BUG-011.
                            onPassword = { authNote = null; screen = Screen.SET_PASSWORD },
                            onLogOut = {
                                scope.launch { backend.logOut() }
                                number = ""
                                authNote = null
                                screen = Screen.SIGN_IN
                            },
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
                                        // The one place the door sentence is
                                        // the right one: an open really may
                                        // have happened.
                                        else -> scanNote =
                                            sentenceFor(answer, R.string.unclear_result)
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
