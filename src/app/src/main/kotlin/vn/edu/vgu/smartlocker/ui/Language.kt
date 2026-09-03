package vn.edu.vgu.smartlocker.ui

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.Locale
import vn.edu.vgu.smartlocker.R

/**
 * The two languages the app is written in.
 *
 * Vietnamese is not a translation of an English app. The product is for VGU,
 * the headline on the sign-in screen was written in Vietnamese first, and
 * English is the second version of it. As of task **P2-16** the resource
 * files say so too: `values/` holds Vietnamese and `values-en/` holds
 * English, where it used to be the other way round.
 *
 * [tag] is a BCP 47 tag, which is what `values-en` resolves against.
 */
enum class AppLanguage(val tag: String, val labelRes: Int) {
    ENGLISH("en", R.string.lang_en),
    VIETNAMESE("vi", R.string.lang_vi),
    ;

    companion object {
        /**
         * What the app opens in before anybody chooses. **Vietnamese, on
         * every phone.**
         *
         * It used to follow the phone, which meant an English phone opened an
         * English app. That is the wrong default here for a reason that is
         * about the users and not about the code: this locker stands on a
         * Vietnamese campus, most of the people who will use it read
         * Vietnamese more easily than English, and a phone's language is
         * often whatever it shipped with rather than a choice anybody made.
         *
         * An English reader changes it in Settings once and the app changes
         * on the next frame. A Vietnamese reader who was handed an English
         * app may never find the row that fixes it.
         *
         * The phone's own locale is deliberately not consulted. It is kept as
         * a function rather than a constant because the caller reads as a
         * question - *what does this phone open in* - and because a saved
         * choice will answer it here when one is stored.
         */
        fun ofSystem(): AppLanguage = VIETNAMESE
    }
}

/** Which language is being drawn. Read it to tick the right row in a picker. */
val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

/**
 * Draw everything inside in [language].
 *
 * **The locale is swapped in the composition, not on the Activity.** The
 * platform's own per-app language API — `LocaleManager` on API 33, or
 * `AppCompatDelegate.setApplicationLocales` below it — would mean either
 * dropping API 24 to 32 or adding AppCompat to an app that is pure Compose
 * and does not otherwise want it. Both also restart the Activity to apply,
 * which throws away the screen you are on and plays the theme wipe backwards
 * on the way out.
 *
 * Overriding the locals costs nothing and changes the language in place, on
 * the next frame, with the Settings screen still under your thumb.
 *
 * All three locals have to be provided together. `stringResource` reads
 * [LocalResources] for the strings and touches [LocalConfiguration] so it
 * recomposes when the configuration changes; [LocalContext] is provided too
 * so anything that reaches for resources through the context — a drawable, a
 * plural, an `AndroidView` — resolves in the same language rather than
 * quietly staying in the system's.
 */
@Composable
fun AppLanguageProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Keyed on both: the language the user picked, and the configuration the
    // system handed down. Rotating the phone or changing its font scale
    // replaces the second one, and the override has to be rebuilt on top of
    // the new one or it would pin the app to a stale screen size.
    val localized = remember(language, configuration) {
        val config = Configuration(configuration).apply {
            setLocale(Locale.forLanguageTag(language.tag))
        }

        // A WRAPPER around the Activity, not the context that
        // `createConfigurationContext` returns.
        //
        // That context is a detached one: it carries the right resources but
        // it is not the Activity and does not lead back to it. Anything that
        // looks for the Activity by walking up `baseContext` — which is how
        // `rememberLauncherForActivityResult`, and every other
        // `LocalXOwner`, finds its owner — then walks off the end and throws
        // `No ActivityResultRegistryOwner was provided`. It crashed the app
        // the moment Home drew its map, so signing in threw you out to the
        // launcher, which read as "the app just exits".
        //
        // Wrapping keeps the chain intact: the wrapper answers with the
        // localised resources, and `baseContext` is still the Activity, so
        // the walk finds it on the first step.
        val strings = context.createConfigurationContext(config).resources
        object : ContextWrapper(context) {
            override fun getResources(): Resources = strings
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalConfiguration provides localized.resources.configuration,
        LocalContext provides localized,
        LocalResources provides localized.resources,
        content = content,
    )
}
