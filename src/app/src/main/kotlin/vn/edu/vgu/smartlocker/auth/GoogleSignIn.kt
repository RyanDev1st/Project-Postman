package vn.edu.vgu.smartlocker.auth

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import vn.edu.vgu.smartlocker.BuildConfig
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.net.Backend
import vn.edu.vgu.smartlocker.net.Http

/**
 * Asking Android for a Google account, and getting back a token to post.
 *
 * This is the app's half of endpoint 19 and of
 * [ADR 0026](../../../../../../../../docs/adr/0026-the-booking-makes-the-number-true.md):
 * **a VGU Google account is what makes an account here.** No code is sent by
 * SMS, nothing waits for one, and the phone number is a delivery address
 * typed later, once, when a box is booked.
 *
 * ## What this file is not allowed to do
 *
 * It does not decide who anybody is. It asks the system for a credential and
 * hands the resulting token to the server, which checks Google's signature
 * and the university domain itself. The app cannot assert an identity - it
 * can only pass on what Google signed - and that is the whole reason a
 * photograph of this screen, or a rooted phone, buys nothing.
 *
 * It also never sees a password. Credential Manager is a system UI: the
 * account picker is drawn by Android, over this app, and the app is not told
 * which accounts exist until one is chosen.
 *
 * ## Why `GetSignInWithGoogleOption` and not `GetGoogleIdOption`
 *
 * The picker has to show **every** account on the phone, not the ones already
 * used here. A student's phone is usually signed in to a personal Gmail
 * first, and the account they need is the university one they may have just
 * added. `GetGoogleIdOption` filtering by authorised accounts would hide it
 * and leave them staring at an account the server is about to refuse.
 */
object GoogleSignIn {

    /** What came back. One of exactly four things, and the screen draws each. */
    sealed interface Result {
        /** A token to post to endpoint 19, and the name to seed a new account. */
        data class Ok(val idToken: String, val fullName: String) : Result

        /** They closed the picker. Not a failure, and never shown as one. */
        data object Cancelled : Result

        /** No account on this phone at all, so there is nothing to pick. */
        data object NoAccount : Result

        /**
         * Google or the system refused, and [detail] says why.
         *
         * Kept and shown, unlike every other error in this app, because the
         * likeliest cause by far is a build whose client id is not registered
         * against this app's signing certificate - and that is invisible from
         * the phone unless the message is put on the screen. It is a
         * developer-facing string from the platform and holds no personal
         * data.
         */
        data class Failed(val detail: String) : Result
    }

    /** True when this build was given a client id to ask with. */
    val configured: Boolean get() = BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()

    /**
     * Show the account picker and wait for a token.
     *
     * [context] must be the Activity, not the application: Credential Manager
     * draws over the current window and cannot find one otherwise.
     *
     * Suspends, and the wait is the person reading a list of accounts, so it
     * can be a long one. Nothing here retries - a second picker appearing
     * because the first timed out is worse than no picker.
     */
    suspend fun signIn(context: Context): Result {
        if (!configured) {
            return Result.Failed("This build has no GOOGLE_CLIENT_ID.")
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build()
            )
            .build()

        return try {
            val answer = CredentialManager.create(context).getCredential(context, request)
            read(answer.credential)
        } catch (stopped: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (none: NoCredentialException) {
            Result.NoAccount
        } catch (refused: GetCredentialException) {
            // `type` names the failure class and the message names the cause.
            // Both are needed: the type alone does not distinguish an
            // unregistered client id from a Play Services that is too old.
            Result.Failed(refused.message ?: refused.type)
        }
    }

    /**
     * The token out of whatever the system handed back.
     *
     * The type is checked rather than assumed. Credential Manager is a
     * general credential API and can answer with a passkey or a password if
     * one is ever asked for, and reading one of those as a Google token would
     * be a crash on a screen somebody is trying to sign in on.
     */
    private fun read(credential: androidx.credentials.Credential): Result {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return Result.Failed("Unexpected credential: ${credential.type}")
        }
        return try {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            Result.Ok(
                idToken = google.idToken,
                // The profile name, for an account that has none yet. Blank
                // is legal and the server treats it as "no name given" -
                // better than inventing one from the email address, which is
                // an alias as often as it is a person.
                fullName = google.displayName.orEmpty(),
            )
        } catch (bad: Exception) {
            Result.Failed("Could not read the Google credential: ${bad.message}")
        }
    }
}

/**
 * What the sign-in screen does when the Google button is tapped, start to
 * finish: ask the system for an account, post the token to endpoint 19, and
 * come back with one of three things to do.
 *
 * It lives here rather than in `MainActivity` because it is the only part of
 * signing in that has a decision in it, and a decision belongs somewhere it
 * can be read on its own. The screen keeps no state but *busy* and one note.
 */
sealed interface GoogleOutcome {
    /** A token is in the secure store. Go to Home. */
    data object SignedIn : GoogleOutcome

    /** They closed the picker. Say nothing at all - they know what they did. */
    data object Dismissed : GoogleOutcome

    /** Put this sentence under the button and stay where we are. */
    data class Say(@param:StringRes val note: Int) : GoogleOutcome
}

/**
 * The whole flow, off the main thread where it needs to be.
 *
 * The platform's own message is logged rather than shown, except for the two
 * cases a person can act on: no account on the phone, and the wrong kind of
 * account. Everything else is one flat sentence, because which check Google
 * failed is exactly what somebody forging a token wants to learn.
 */
suspend fun signInThroughGoogle(context: Context, backend: Backend): GoogleOutcome =
    when (val got = GoogleSignIn.signIn(context)) {
        is GoogleSignIn.Result.Cancelled -> GoogleOutcome.Dismissed
        is GoogleSignIn.Result.NoAccount -> GoogleOutcome.Say(R.string.signin_google_none)
        is GoogleSignIn.Result.Failed -> {
            // The one place a platform string is kept. It is the only way to
            // tell an unregistered client id from a phone with no Play
            // Services, and neither is visible from the screen.
            Log.w("GoogleSignIn", got.detail)
            GoogleOutcome.Say(
                if (GoogleSignIn.configured) R.string.signin_google_failed
                else R.string.signin_google_off
            )
        }
        is GoogleSignIn.Result.Ok -> when (
            val answer = backend.signInWithGoogle(got.idToken, got.fullName)
        ) {
            is Http.Answer.Ok -> GoogleOutcome.SignedIn
            is Http.Answer.Refused ->
                GoogleOutcome.Say(answer.reason.message ?: R.string.refused_unknown)
            is Http.Answer.Unclear -> GoogleOutcome.Say(R.string.unclear_no_server)
        }
    }
