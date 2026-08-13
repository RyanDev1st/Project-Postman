package vn.edu.vgu.smartlocker.ui

import android.content.res.Configuration
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
 * the headline on the sign-in screen was written in Vietnamese first, and the
 * English base is the second version of it. English is the resource default
 * only because that is what `values/` means to Android.
 *
 * [tag] is a BCP 47 tag, which is what `values-vi` resolves against.
 */
enum class AppLanguage(val tag: String, val labelRes: Int) {
    ENGLISH("en", R.string.lang_en),
    VIETNAMESE("vi", R.string.lang_vi),
    ;

    companion object {
        /**
         * What the phone is set to, if the app speaks it. A Vietnamese phone
         * gets a Vietnamese app on first run without anyone choosing.
         */
        fun ofSystem(): AppLanguage =
            if (Locale.getDefault().language == VIETNAMESE.tag) VIETNAMESE else ENGLISH
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
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalConfiguration provides localized.resources.configuration,
        LocalContext provides localized,
        LocalResources provides localized.resources,
        content = content,
    )
}
