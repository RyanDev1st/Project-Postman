// The API server. Endpoints 1 to 22 of docs/reference/api-contract.md.
//
// Kotlin rather than Python, and SQLite rather than MySQL: ADR 0020. The
// version of everything below comes from gradle/libs.versions.toml, never from
// a number typed here.

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

// Java 17 bytecode, the same as `:app`, and set the same way it is there - a
// target rather than a toolchain. A toolchain would demand a JDK 17 be
// installed; this machine builds the app on 23 and there is no reason the
// server should need a second JDK to build beside it.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.default.headers)

    // Serves TLS in development. Both front-ends refuse plain HTTP.
    implementation(libs.ktor.network.tls.certificates)

    // The two things that must survive a power cut: the lockout counter and
    // the sessions the server has issued.
    implementation(libs.sqlite.jdbc)

    // Argon2id, for the password endpoints.
    implementation(libs.bouncycastle)

    // Without a binding, Ktor's log is silently discarded - and the server log
    // is the evidence that a door opened.
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("vn.edu.vgu.smartlocker.server.MainKt")

    // Everything the server needs to run is an environment variable with a
    // default, so `./gradlew :server:run` works on a clean clone with no
    // arguments. See src/server/README.md.
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}
