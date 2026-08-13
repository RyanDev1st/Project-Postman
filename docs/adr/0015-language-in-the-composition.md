# 0015 — The language is swapped in the composition, not on the Activity

- **Status:** accepted
- **Date:** 2026-08-13
- **Deciders:** Claude, delegated by Ryan

## Context

The app is for VGU. It has to be readable in Vietnamese, and the Settings
screen has carried a Language row since the first design. The row called a
callback that did nothing, and every screen wrote its words as Kotlin string
literals.

Android has an official way to give one app its own language, separate from
the phone's. It is called per-app language preferences. Choosing it or not is
the decision here, because it is the difference between the app appearing in
the phone's own Settings under "App languages" and not appearing there at all.

Our floor is `minSdk 24`. The app is pure Compose on a bare
`ComponentActivity`: no AppCompat, no fragments, no XML layouts.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| A — `LocaleManager.setApplicationLocales` | The platform's own answer. The choice is stored by the system, survives reinstall, and shows in the phone's Settings | API 33 and up. Our floor is 24, so on a phone from 2017 to 2022 there is no language switch at all. Restarts the Activity to apply |
| B — `AppCompatDelegate.setApplicationLocales` | Works down to API 21, so every phone we support. Same stored-by-the-system behaviour on 33+ | Needs `androidx.appcompat` and a manifest service entry, in an app that uses none of AppCompat. Still restarts the Activity |
| C — Override the locale in the composition | Works on every API level. No new dependency. Changes the words on the next frame with no restart | The system does not know. The app does not appear in the phone's "App languages" screen, and the choice is not stored by anyone but us |

## Decision

We choose **C**. `AppLanguageProvider` in `ui/Language.kt` builds a
`Configuration` with the chosen locale, makes a context from it with
`createConfigurationContext`, and provides `LocalContext`,
`LocalConfiguration` and `LocalResources` together. Every screen reads its
words through `stringResource`, so they resolve in whichever language is
provided above them.

## Why

A and B both restart the Activity to apply. That is not a detail on this app:
a restart throws away the screen the person is standing on, and this app is
used standing at a cabinet with a parcel in the box. B also means adding
AppCompat — a whole compatibility layer, its own theme requirements and a
manifest service — to an app that deliberately has none of it, in order to
change a string. A rules out every phone older than Android 13, which is most
of the team's.

Overriding three composition locals is about twenty lines, needs nothing new,
and the language changes while the picker is still open, so the person can see
that the thing they pressed did what it said.

## What we accept

**The system does not know what language the app is in.** The app will not
appear in the phone's Settings under "App languages", and we do not declare
`android:localeConfig`. Someone looking for the switch in the phone's settings
will not find it; it is in the app, on the Settings tab.

**The choice is not written down.** It lives in memory and is forgotten when
the process dies, exactly like the Dark mode toggle. Neither is persisted yet
because both want the same store and the app has none. First run follows the
phone's language, so a Vietnamese phone opens in Vietnamese without anyone
choosing, which covers most of the cost.

**We own a path the platform would otherwise own.** If Compose changes which
local `stringResource` reads, this breaks and the app silently stays in one
language. It reads `LocalResources` today; all three are provided so that a
change to any one of them does not leave a gap. The check is the one in the
release: switch the language on Settings and watch the nav bar change with it.

Reversing this is cheap and gets cheaper. When `minSdk` reaches 33, option A
is a few lines and this file is superseded.

## Affects

- `src/app/src/main/kotlin/vn/edu/vgu/smartlocker/ui/Language.kt` — the provider
- `src/app/src/main/res/values/strings.xml`, `values-vi/strings.xml` — the words
- `settings/LanguagePicker.kt` — where the choice is made
- Shipped in 0.10.0. Supersedes nothing.
- Blocks nothing. When a settings store exists, both this and the theme should
  be written to it in the same change.
