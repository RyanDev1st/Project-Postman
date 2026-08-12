plugins {
    // No Kotlin plugin here. Since AGP 9.0 the Android plugin brings Kotlin
    // with it, and adding org.jetbrains.kotlin.android on top is an error.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Sends a build to the team's phones. It touches the upload task only -
    // nothing Firebase goes into the APK, and the app talks to the Server
    // team's API, not to Firebase. See docs/reference/releasing.md.
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "vn.edu.vgu.smartlocker"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.edu.vgu.smartlocker"

        // 24 is a guess, not a measurement - ADR 0009. Nobody asked the team
        // what phones they have. Lowering this later is safe; raising it
        // drops users, so it was set low on purpose. Assumption A-17.
        minSdk = 24
        targetSdk = 36

        // versionCode counts up and never repeats. versionName is what a
        // person reads, and it is the number P1-01 puts on the screen.
        // Bump BOTH on every build that is sent to a phone.
        //
        // Six releases went out on versionCode 1 and versionName 0.1.0. The
        // app prints its version name on screen, so all six printed the same
        // thing, and a tester holding the phone could not tell a build that
        // failed to install from a fix that failed to work. That is not a
        // cosmetic problem: it makes every report ambiguous, including
        // "this has all the same problems as the prior release".
        versionCode = 4
        versionName = "0.4.0"

        // One ABI, not four.
        //
        // MapLibre ships libmaplibre.so per architecture, 8-11 MB each, and
        // carrying all four put the debug APK at 55.6 MB - a real cost when
        // the build is downloaded to a phone on mobile data. arm64-v8a is
        // every Android phone sold since about 2017, which is every phone
        // this is tested on.
        //
        // The cost is emulators: an x86_64 image will not install this. Add
        // "x86_64" to the list below to get one back.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }

        debug {
            // Who gets the build, and what they are told about it. The app id
            // is written here rather than read from google-services.json,
            // because that file is git-ignored: reading it would mean nobody
            // else could build the app at all until they fetched one.
            //
            // A Firebase app id is not a secret. Google publishes these
            // config values as public by design - the id names the app, it
            // does not authorise anything. Uploading still needs a real
            // login, which is why this line alone lets nobody send a build.
            firebaseAppDistribution {
                appId = "1:853813159408:android:7a1e50d75e1d0e4c192935"
                artifactType = "APK"
                groups = "team"
                releaseNotesFile = "$rootDir/docs/reference/release-notes.txt"
            }
        }
    }

    buildFeatures {
        compose = true
        // Off by default in AGP 8 and later. The app reads its own version
        // name from BuildConfig, so it has to be on.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric needs the real resources - the theme, the fonts and
            // the colours are what the parity tests are checking. Without this
            // it gets an empty resource table and every colour comes out
            // transparent.
            isIncludeAndroidResources = true
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

/**
 * Ship `config/settings.json` inside the app, without a second copy of it.
 *
 * The app needs those numbers before it has ever reached the server, so they
 * have to be in the APK. Committing a duplicate under res/raw would mean two
 * files that must agree and will not: somebody corrects one, ships, and the
 * phone keeps using the other. So the real file is copied in at build time
 * into build/, which git ignores.
 *
 * Wired through the **variant API**, not `sourceSets[...].res.srcDir(...)`.
 * AGP 9 rejects a Provider there outright - it cannot tell a generated
 * directory from a hand-edited one, and the task dependency would not be
 * carried. `addGeneratedSourceDirectory` says both things at once.
 */
abstract class CopySettingsTask : DefaultTask() {
    @get:InputFile
    abstract val settingsFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val raw = outputDir.get().asFile.resolve("raw")
        raw.mkdirs()
        settingsFile.get().asFile.copyTo(raw.resolve("settings.json"), overwrite = true)
    }
}

androidComponents {
    onVariants { variant ->
        val copySettings = tasks.register<CopySettingsTask>(
            "copySettings${variant.name.replaceFirstChar { it.uppercase() }}",
        ) {
            settingsFile.set(rootProject.file("config/settings.json"))
        }
        variant.sources.res?.addGeneratedSourceDirectory(
            copySettings, CopySettingsTask::outputDir,
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.compose.animation)
    implementation(libs.maplibre)

    // Preview support. debugImplementation so the tooling never ships in a
    // release build.
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // The design-parity loop: draw a composable to a PNG on this machine and
    // compare it to the mock-up. See docs/roadmap/phase-1.5-design-parity.md.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    // The empty activity the test host runs the composable in. debug- rather
    // than testImplementation because it is a manifest, and it has to be
    // merged into the app under test rather than sit on the test classpath.
    debugImplementation(libs.androidx.ui.test.manifest)
}
