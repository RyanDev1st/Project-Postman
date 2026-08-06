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
        versionCode = 1
        versionName = "0.1.0"
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    // Preview support. debugImplementation so the tooling never ships in a
    // release build.
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
}
