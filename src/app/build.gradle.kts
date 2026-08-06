plugins {
    // No Kotlin plugin here. Since AGP 9.0 the Android plugin brings Kotlin
    // with it, and adding org.jetbrains.kotlin.android on top is an error.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
