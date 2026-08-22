package vn.edu.vgu.smartlocker.net

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.edu.vgu.smartlocker.net.Http.Answer

/**
 * The server, as a screen sees it.
 *
 * [Api] is deliberately blocking - one request, one answer, and no opinion
 * about where the waiting happens. This is where that opinion lives, once: on
 * [Dispatchers.IO], with the result handed back to whatever composable asked.
 * The alternative is every screen remembering to leave the main thread, and
 * the one that forgets freezes the app while somebody stands at a locker.
 *
 * **No screen holds a token.** The token is written and read by [TokenStore]
 * inside [Api], so nothing above this line can log one by accident.
 *
 * **Nothing here retries.** [Api.collectByScan] moves real metal and an
 * [Answer.Unclear] from it means *go and look at the cabinet*, never *ask
 * again*. Wrapping any of this in a retry would undo the rule that
 * `Http.kt` exists to keep.
 */
class Backend(context: Context) {

    private val settings = Settings(context.applicationContext)
    private val tokens = TokenStore(context.applicationContext)
    private val api = Api(settings, tokens)

    /**
     * Whether this build has anywhere to send a request.
     *
     * Blank until somebody fills in `server_base_url` in
     * `config/settings.json`. Asked before the first call so the app can say
     * so plainly, rather than showing a person a spinner that resolves into
     * "could not reach the server" - which is true, and useless.
     */
    val hasAddress: Boolean get() = settings.serverBaseUrl.isNotBlank()

    /** Whether this phone has been signed in before. Not whether it still is. */
    val signedIn: Boolean get() = tokens.hasToken()

    // --- The way in --------------------------------------------------------

    suspend fun requestCode(number: String): Answer<Unit> =
        io { api.requestCode(number) }

    /** On success the token is already stored. Nothing is handed back. */
    suspend fun verifyCode(number: String, code: String, fullName: String = ""): Answer<Unit> =
        io { api.verifyCode(number, code, fullName) }

    /** The deliberate act. Clears locally whatever the server answers. */
    suspend fun logOut(): Answer<Unit> = io { api.logout() }

    /** The server already ended it. Just drop what we are holding. */
    suspend fun forget() = io { api.forget() }

    // --- What is waiting ---------------------------------------------------

    suspend fun parcels(): Answer<List<Parcel>> = io { api.waitingParcels() }

    suspend fun history(): Answer<List<Event>> = io { api.history() }

    /**
     * How long a parcel may sit, and when the ticket starts saying hurry.
     *
     * Read through here rather than by a screen making its own [Settings], so
     * there is one copy holding one file open. Endpoint 15 can correct both
     * without a new build - see [refreshSettings].
     */
    val pickupCodeHours: Int get() = settings.pickupCodeHours
    val collectSoonHours: Int get() = settings.collectSoonHours

    // --- The door ----------------------------------------------------------

    /**
     * **The call that opens a locker.** Endpoint 6.
     *
     * Only ever from a scan the person made. Never from a timer, a retry, or a
     * screen coming back into view - rule 3 in `architecture.md`.
     */
    suspend fun collect(sessionCode: String): Answer<Opened> =
        io { api.collectByScan(sessionCode) }

    // --- The numbers -------------------------------------------------------

    /**
     * Fetch corrected settings. Failing is fine and is not reported: the last
     * good copy stays, which is rule 2 of [Settings].
     *
     * @return `true` if a number actually changed, so a screen that has
     *   already drawn with the old ones can be asked to draw again. A fetch
     *   that changes nothing - which is nearly every one - returns `false`
     *   and costs nothing.
     */
    suspend fun refreshSettings(): Boolean =
        io { api.fetchSettings() }.let { it is Answer.Ok && it.value }

    private suspend fun <T> io(work: () -> T): T = withContext(Dispatchers.IO) { work() }
}
