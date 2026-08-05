# Glossary

Plain-English meaning of every term used in this repo. Add a row when you use a new one.

| Term | Plain meaning |
| --- | --- |
| **API** | The list of requests the app may send to the server, and the answers it gets back. The only door between the app and the data |
| **API contract** | The written, agreed version of that list. See [api-contract.md](api-contract.md) |
| **Endpoint** | One single request on that list. "Log in" is one endpoint. "Open locker" is another |
| **Token** | A temporary pass the server gives the app after a correct login. The app shows it with every later request, instead of sending the password again |
| **Expiry** | The moment a token stops working. After that the user logs in again |
| **Backend / server** | The machine that holds the truth and commands the cabinet hardware. Owned by the Server team |
| **Database** | The tables that remember users, cabinets, boxes, parcels and history |
| **Cabinet** | One physical unit of boxes, with a screen on it. Sits in one place on campus |
| **Box** | One door in a cabinet. A parcel goes in one box |
| **Cabinet screen** | The screen fixed to the cabinet. The shipper uses it. It has no login, so anyone can walk up to it |
| **Shipper** | The person delivering a parcel. Installs nothing, uses the cabinet screen |
| **Receiver** | The person collecting a parcel. Uses the phone app |
| **QR session code** | The code shown on the cabinet screen. It says *which cabinet, at what moment*, and nothing else. It is not a key — see [architecture.md](architecture.md) section 6 |
| **One-time code** | The short number sent to a phone during registration, to prove the number is real |
| **Masked name** | A name with most of it hidden, such as `Nguyễn V. A***`. Enough for a shipper to confirm the right person, not enough to harvest names |
| **Cabinet key** | The secret that proves a cabinet is that cabinet. Placed on the device at setup. Never in this repo |
| **Push notice** | A message the phone shows when a parcel arrives, even with the app closed |
| **Fallback** | The second way to do something when the first way fails |
| **Native** | An app built separately for Android and for iOS, in each platform's own language |
| **Cross-platform** | One code base that builds an app for both Android and iOS |
| **Build** | Turning the code into an app file that installs on a phone |
| **Release build** | The build that goes to real users. Slower to make, faster to run, harder to debug |
| **Emulator / simulator** | A fake phone on a computer. Good for layout. Not good enough for camera, network or hardware tests |
| **Repo** | This folder, tracked by git |
| **Branch** | A separate line of work in the repo, so two people do not overwrite each other |
| **Commit** | One saved change, with a message saying why |
| **ADR** | Architecture Decision Record. One short file saying why a choice was made. See [../adr/](../adr/) |
| **Finding** | A dated report of something we checked on a given day. See [../findings/](../findings/) |
| **STE** | Simplified Technical English. Short sentences, one idea each, plain words. The writing style for these docs |
