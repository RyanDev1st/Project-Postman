pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // A module may not add its own repository. One list, here, so nobody
    // pulls a dependency from somewhere nobody agreed to.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "project-postman"

// The module lives under src/, not at the repo root. Gradle's default is a
// top-level `app/` folder; the Repository map in CLAUDE.md says app source
// goes in src/, and that rule wins. Only the Gradle setup files sit at the
// root, which the root policy allows.
include(":app")
project(":app").projectDir = file("src/app")

// The OTP sender server. Same rule as the app: source lives in src/.
include(":otp-server")
project(":otp-server").projectDir = file("src/otp-server")
