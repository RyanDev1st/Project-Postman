# The cabinet hardware — what the ESP32 must do

Parent: none

For the team building the cabinet. **The board is yours. This page is only the
part that has to match ours**, so a door opens when a student scans and at no
other time.

Two calls, one key, and one rule. Nothing else about the wiring, the relays or
the power supply is decided here.

## Why the board asks instead of being told

The ESP32 takes a DHCP address on campus Wi-Fi. It has no name, nothing outside
can dial it, and its address changes. So it **polls**: about once a second it
asks the server whether there is anything to do, and does what comes back.

That costs up to a second of delay before a door opens. A student who has just
scanned will accept a second. The alternative — the server calling the cabinet —
needs a fixed address, a port forward, and a way to find the board again every
time the router hands out a new lease. Decided in
[ADR 0020](../adr/0020-the-server-is-kotlin.md).

```
   ESP32  ──  GET  /cabinet/commands     ──>  server     "anything for me?"
          <──  {"commands":[{"id":"…","box":"04","action":"open"}],"at":"…"}
   ESP32  ──  POST /cabinet/command-done ──>  server     "04 opened"
   ESP32  ──  POST /cabinet/door-closed  ──>  server     "04 is shut again"
```

## The key

Every call carries the header:

```
X-Cabinet-Key: <the key for this cabinet>
```

The server prints that key **once**, when the cabinet is created:

```
./gradlew :server:run --args="cabinet add vgu-back-gate 'VGU Back Gate' 20"
```

It is stored hashed and cannot be shown again. If a cabinet is stolen, or the
key ends up somewhere it should not be, replace it — the old one stops working
the moment you do:

```
./gradlew :server:run --args="cabinet rotate vgu-back-gate"
```

**One key per cabinet, never one shared master.** A key that opens every cabinet
turns one stolen board into every locker on campus.

**The key never goes in the repo**, and never in a commit, in a screenshot, or
in a chat message. It is typed onto the device at setup and lives there.

## HTTPS, and why there is no flag to turn it off

The server speaks HTTPS only. The phone app refuses a plain-HTTP address before
it opens a socket, the cabinet screen does the same, and the board must too.

An open command travels over this connection. Over plain HTTP, anyone on the
same Wi-Fi can read the key out of the header and then open every door in the
cabinet whenever they like.

In development the certificate is self-signed and lives at
`config/dev-cert/locker.crt`. Put it in the sketch as the root CA:

```cpp
client.setCACert(LOCKER_CA);   // the contents of config/dev-cert/locker.crt
```

Do **not** use `client.setInsecure()`. It compiles, it works, and it accepts any
certificate anybody offers — which is the whole attack this connection exists to
stop.

## The one rule

**A command is handed over exactly once.** The server stamps a row as taken in
the same transaction that reads it, so a board that loses the reply and asks
again is handed nothing.

That means the board must not "helpfully" re-run the last command it saw. If a
reply is lost, the door did not open, the student taps again, and a new command
is issued. A door that opens twice — once now and once two minutes later, after
the student has gone — is worse than one that never opened, because nobody is
standing at it.

## Reference sketch

Not the firmware. It is the shortest thing that speaks the protocol correctly,
so the parts that matter are visible. Relay pins, timings and the enclosure are
yours.

```cpp
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "...";
const char* WIFI_PASS = "...";

// One cabinet, one key. Printed once by `cabinet add`. Never in git.
const char* CABINET_KEY = "...";
const char* SERVER      = "https://192.168.1.19:8443";

// The contents of config/dev-cert/locker.crt, exactly as they appear there.
const char* LOCKER_CA = R"(-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
)";

// Door number -> relay pin. Numbers are strings on the wire, because the
// doors are labelled 01..20 and an int drops the leading zero.
const int RELAY[21] = { 0, 13, 12, 14, 27, 26, 25, 33, 32, /* ... */ };

WiFiClientSecure tls;

void openDoor(int box) {
  digitalWrite(RELAY[box], HIGH);
  delay(400);                      // long enough for the latch to release
  digitalWrite(RELAY[box], LOW);
}

// Every request in one place, so the key and the certificate are attached
// once and cannot be forgotten on the call that matters.
int callServer(const char* method, const String& path,
               const String& body, String& answer) {
  HTTPClient http;
  if (!http.begin(tls, SERVER + path)) return -1;
  http.addHeader("X-Cabinet-Key", CABINET_KEY);
  http.addHeader("Accept", "application/json");

  int status;
  if (strcmp(method, "POST") == 0) {
    http.addHeader("Content-Type", "application/json");
    status = http.POST(body);
  } else {
    status = http.GET();
  }
  answer = (status > 0) ? http.getString() : "";
  http.end();
  return status;
}

void setup() {
  Serial.begin(115200);
  for (int b = 1; b <= 20; b++) { pinMode(RELAY[b], OUTPUT); digitalWrite(RELAY[b], LOW); }

  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) delay(500);

  tls.setCACert(LOCKER_CA);        // never setInsecure()
}

void loop() {
  String answer;
  int status = callServer("GET", "/cabinet/commands", "", answer);

  // Anything other than a clear 200 means do nothing at all. A door is not
  // opened on a guess about what the server might have meant.
  if (status == 200) {
    JsonDocument doc;
    if (!deserializeJson(doc, answer)) {
      for (JsonObject c : doc["commands"].as<JsonArray>()) {
        const char* id  = c["id"];
        const char* box = c["box"];

        if (strcmp(c["action"], "open") == 0) {
          openDoor(atoi(box));

          // For the record only. Nothing waits on it: a board that loses
          // power between opening a door and saying so must not leave a
          // student standing at an open box being told it failed.
          JsonDocument done;
          done["id"] = id;
          done["result"] = "opened";
          String out; serializeJson(done, out);
          callServer("POST", "/cabinet/command-done", out, answer);
        }
      }
    }
  }

  delay(1000);   // one poll a second. See "Why the board asks".
}
```

## Telling the server a door shut

There is no sensor — [ADR 0006](../adr/0006-no-sensor.md) — so today the cabinet
screen fires this when the shipper taps "closed". If a switch is ever fitted,
the switch fires the same call and **nothing else changes**. That is why it is
named for the event and not for the sensor:

```
POST /cabinet/door-closed   {"box_number":"04","purpose":"drop"}
```

`purpose` is `drop` or `collect`. A collect frees the box and writes the parcel
off as collected; a drop leaves it taken with a parcel waiting.

## A box that will not work

```
POST /cabinet/fault   {"box_number":"04","what":"latch does not release"}
```

Nothing opens that box again until staff clear it, and anybody with a parcel in
it is told plainly rather than left pulling at a dead door.

## What we need from you

1. **How does the key get onto a board at setup, and what does replacing it
   involve?** This blocks task P0-05. Our end is done — `cabinet add` and
   `cabinet rotate` — and yours is whatever a person does at the cabinet.
2. **How long does a latch need the relay held?** The sketch says 400 ms and
   that is a guess.
3. **What happens on a power cut mid-open?** We know the server's side: the
   command is already marked taken and is not re-sent. We do not know yours.
