package vn.edu.vgu.smartlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import vn.edu.vgu.smartlocker.auth.OneTimeCodeScreen
import vn.edu.vgu.smartlocker.auth.PhoneNumberScreen
import vn.edu.vgu.smartlocker.parcels.HistoryScreen
import vn.edu.vgu.smartlocker.parcels.WaitingScreen
import vn.edu.vgu.smartlocker.pickup.OpenedScreen
import vn.edu.vgu.smartlocker.pickup.ScanScreen
import vn.edu.vgu.smartlocker.pickup.TypeCodeScreen

/** The seven screens, named in docs/reference/app-screens.md. */
enum class Screen { PHONE, CODE, WAITING, SCAN, OPENED, TYPE_CODE, HISTORY }

/**
 * The app.
 *
 * P1-09: every screen exists and can be walked through, with made-up data
 * and no server. It is here to be looked at and argued with, not used.
 *
 * Moving between screens is a variable and a `when`. No navigation library:
 * seven screens do not need one, and the real routes arrive with the real
 * screens in Phase 2. Rule J - the simplest thing that solves it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppSkeleton()
                }
            }
        }
    }
}

@Composable
fun AppSkeleton() {
    var screen by remember { mutableStateOf(Screen.PHONE) }

    // Back goes where the flow diagram says it goes, so walking the skeleton
    // on a phone tells you whether the flow is right.
    BackHandler(enabled = screen != Screen.PHONE) {
        screen = when (screen) {
            Screen.CODE -> Screen.PHONE
            Screen.SCAN, Screen.TYPE_CODE, Screen.HISTORY -> Screen.WAITING
            Screen.OPENED -> Screen.WAITING
            else -> Screen.WAITING
        }
    }

    when (screen) {
        Screen.PHONE -> PhoneNumberScreen(
            version = BuildConfig.VERSION_NAME,
            onSend = { screen = Screen.CODE },
        )

        Screen.CODE -> OneTimeCodeScreen(
            onDone = { screen = Screen.WAITING },
        )

        Screen.WAITING -> WaitingScreen(
            onScan = { screen = Screen.SCAN },
            onTypeCode = { screen = Screen.TYPE_CODE },
            onHistory = { screen = Screen.HISTORY },
        )

        Screen.SCAN -> ScanScreen(
            onScanned = { screen = Screen.OPENED },
            onTypeCode = { screen = Screen.TYPE_CODE },
        )

        Screen.TYPE_CODE -> TypeCodeScreen(
            onAccepted = { screen = Screen.OPENED },
        )

        Screen.OPENED -> OpenedScreen(
            onDone = { screen = Screen.WAITING },
        )

        Screen.HISTORY -> HistoryScreen()
    }
}
