// The OTP sender server (spec: docs/superpowers/specs/2026-08-09-otp-sender-design.md).
// JVM target 17 like the app module: the machine runs JDK 23, and a toolchain
// would silently download a second JDK. None of that here.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("otp.MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.bouncycastle)
    implementation(libs.sqlite.jdbc)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
