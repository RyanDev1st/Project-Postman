# Glossary

Plain-English meaning of every term used in this repo. Add a row when you use a new one.

| Term | Plain meaning |
| --- | --- |
| **API** | The list of requests the app may send to the server, and the answers it gets back. The only door between the app and the data |
| **API contract** | The written, agreed version of that list. See [api-contract.md](api-contract.md) |
| **Endpoint** | One single request on that list. "Log in" is one endpoint. "Open locker" is another |
| **Token** | A temporary pass the server gives the app after a correct login. The app shows it with every later request, instead of sending the password again |
| **Expiry** | The moment a token stops working. After that the user logs in again |
| **Backend / server** | The machine that holds the truth and commands the locker hardware. Owned by the Server team |
| **Database** | The tables that remember users, lockers, points, history, orders |
| **Locker point** | One physical place with a group of lockers. The school has several |
| **Mode** | Normal, or exam season. It changes which lockers may be opened by whom. Set by an admin on the server |
| **Barcode** | The printed code on a student or lecturer card. The app reads it with the camera |
| **Fallback** | The second way to do something when the first way fails. Scan fails → log in with ID and password |
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
