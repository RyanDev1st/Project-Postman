# Getting a build onto a phone

Parent: none

How the team installs the app. Two ways, and the second one is not built yet.

## What we use, and why

**Firebase App Distribution.** A tester gets a link, installs, and is told when a new build is ready.

We are **not** on the Play Store, and that is a decision rather than an oversight. A personal Google Play account opened after 13 November 2023 cannot publish to production until it has run a closed test with **12 testers who stayed opted in for 14 days**. This team is smaller than twelve. Play's *internal testing* track escapes that rule and costs a one-off $25, so it is the next step when there is something worth showing outside the team — not before.

**Firebase is release plumbing here, and nothing else.** No Firebase code ships inside the app. The app talks to the Server team's API, which is what [architecture.md](architecture.md) says and what [ADR 0005](../adr/0005-we-propose-they-object.md) assumes. Firebase Authentication and Firestore are **not** in use, and adopting either would replace work another team owns — that needs an ADR and a conversation with them, not a plugin.

## Send a build

```
gradlew assembleDebug appDistributionUploadDebug
```

That builds the app and uploads it. Testers in the `team` group get an email.

What each part is:

| Thing | Value |
| --- | --- |
| Firebase project | `project-postman-575ed` (number `853813159408`) |
| Android app id | `1:853813159408:android:7a1e50d75e1d0e4c192935` |
| Tester group | `team` |
| What testers are told | `docs/reference/release-notes.txt` — **edit this before every send** |

The app id sits in `src/app/build.gradle.kts` in plain sight. That is safe: Google publishes Firebase app ids as public config, and an id only names the app — it authorises nothing. Sending a build still needs a real login, so that line on its own lets nobody upload anything.

## Add somebody to the team

```
npx -y firebase-tools@latest appdistribution:testers:add name@example.com --project project-postman-575ed
npx -y firebase-tools@latest appdistribution:group:add-testers team name@example.com --project project-postman-575ed
```

**This sends that person an email.** Ask them first.

They then install the *App Tester* app from the link in that email, once. After that every new build appears there on its own.

## First time on a new computer

```
npx -y firebase-tools@latest login
```

The upload borrows those credentials. Nothing else is needed — **building the app does not require a Firebase login**, only sending it does.

## google-services.json

Fetch it with:

```
npx -y firebase-tools@latest apps:sdkconfig ANDROID 1:853813159408:android:7a1e50d75e1d0e4c192935 --project project-postman-575ed --out src/app/google-services.json
```

It is **git-ignored on purpose** and the build does not need it today. Nothing reads it until push notifications arrive in Phase 4 (**P4-01**), which is the first thing that will.

## What is still missing

**A release keystore.** Debug builds are signed with a throwaway key that Android generates. A real release needs our own key, and **if that key is lost the app can never be updated again** — not by us, not by anybody. Whoever makes it must decide where it lives and who else holds a copy, before the first release build. Task **P8-09**.
