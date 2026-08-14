# ADR 0018 — The QR reader is ML Kit, and that means Google Play Services

- **Status:** accepted
- **Date:** 2026-08-14
- **Decided by:** Claude, building P5-02. Raised here because it puts a Google
  dependency in the pickup path, which is the one flow the product exists for
- **Relates to:** [ADR 0014](0014-openfreemap-not-google.md) — the map went the
  other way, and this ADR says why the same reasoning does not apply

## The problem

The receiver collects a parcel by pointing the phone at the cabinet screen and
reading a QR. That read is the product. If it is slow, or it needs a second
try, or it fails in a dim corridor, the whole design — *no key, just your
phone* — is worse than a key.

So the reader had to be chosen on how well it reads, and the choice carries a
dependency the team should see rather than discover.

## What was considered

| Library | Reads | Costs |
| --- | --- | --- |
| **ML Kit barcode scanning** | Google's own detector, the one in Google Lens | Needs Google Play Services on the phone |
| **zxing-cpp** (`io.github.zxing-cpp:android`) | The C++ rewrite of ZXing, no Google anything | Nobody on this team has held it up to a real cabinet |
| **ZXing / journeyapps embedded** | The original Java ZXing | Older, and reads worse in poor light than either above |

## The decision

**`com.google.mlkit:barcode-scanning:17.3.0`, the bundled-model build.**

Bundled means the detector ships inside the APK. It does not download a model
on first use, so the first scan a student ever makes works the same as the
hundredth, on a phone with no signal, standing in front of a locker. That
matters here more than the 3 MB it adds: a pickup must work when the network
does not — [ADR 0010](0010-c4-offline-exception.md).

## The cost, stated plainly

**Bundled does not mean Google-free.** There is no build of ML Kit that avoids
Play Services. Resolved dependency tree, `debugRuntimeClasspath`, checked:

```
com.google.mlkit:barcode-scanning:17.3.0
com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1
com.google.android.gms:play-services-base:18.5.0
com.google.android.gms:play-services-basement:18.4.0
com.google.android.gms:play-services-tasks:18.2.0
```

Bundling removes the model *download*, not the Play Services *dependency*.
Anyone reading the code and expecting otherwise is reading it wrong, which is
why this file exists.

**What it does not cost:** no key, no account, no card, no quota, and nothing
to sign up for. That was the whole reason the map left Google in
[ADR 0014](0014-openfreemap-not-google.md), and none of it applies here. The
objection to Google Maps was a billing account with a card on it. There is no
such thing on this side.

**Who this shuts out:** a phone with no Play Services — a de-Googled Android,
or a Huawei sold after 2019. That student cannot scan. They are not shut out
of the product: the typed code (P5-08) is the other way into the same locker,
and it exists for the refused-camera case anyway. Nobody has to install
anything they did not want to install in order to collect a parcel.

## The way out, if it is ever needed

`io.github.zxing-cpp:android:2.3.0`. It reads the same QRs with no Google
dependency at all, and the swap is confined to one file — `Camera.kt` calls
the detector in one place, and `SessionCode.kt`, which holds every rule about
what the string means, never touches a camera library.

The trigger for taking it: a real phone in the team that cannot scan, or a
read that is measurably worse on the cabinet screen than a person will accept.
Neither is true today, and neither can be known without a cabinet.

## Checked

**Bundled really does read with no Play Services on the device.** The claim is
in Google's documentation; this is the team's own evidence for it.

`ScanPipelineTest` runs on the emulator, puts the cabinet encoder's own QR
through the real ML Kit detector, and reads back the exact payload. That AVD
is a plain AOSP image — `pm list packages | grep com.google.android.gms`
returns **nothing**, API 36 — so nothing was there to fall back on.

```
./gradlew :app:connectedDebugAndroidTest -Pemulator
Starting 4 tests on parity(AVD) - 16
tests="4" failures="0" errors="0"
```

This settles the packaging question, not the reading question. The two are
different and only the second one is below.

## Not verified

The reason for choosing ML Kit is read quality in a dim corridor, and **that
has not been measured against zxing-cpp** — there is no cabinet, no corridor,
and no screen to point a phone at yet. The claim rests on the library's
reputation, not on this team's evidence.

When P1-06 unblocks and a real cabinet screen exists, the honest check is both
readers against the same screen at the same distance in the same light. If
zxing-cpp reads as well, this ADR loses its only argument and should be
reversed for the one it does not have to defend.
