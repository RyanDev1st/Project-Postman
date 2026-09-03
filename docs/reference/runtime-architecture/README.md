# Runtime Architecture

VGU Smart Locker flow and runtime architecture.

## Start Here

Open the [interactive architecture diagram](../runtime-architecture.html).

The diagram follows the product in the order people use it:

1. Receiver signs in with Google, chooses a cabinet and parcel size, and reserves one box.
2. Shipper types the receiver number at the public cabinet, confirms the masked name and box, places the parcel, and confirms the door closed.
3. Receiver scans the QR shown on the cabinet and collects from the server-authorized box.

Use the viewer's guided views to isolate reservation, drop-off, and pickup. Cards contain contract details so the main path stays readable.

## Source

- [Archify specification](../runtime-architecture.architecture.json)
- [Current architecture contract](../architecture.md)
- [Current API contract](../api-contract.md)
- [Cabinet hardware contract](../cabinet-hardware.md)

The diagram reflects current repository contracts. Google sign-in, push/email notices, Pi-owned cabinet networking, and non-networked ESP32 occupancy sensors are current facts. SMS OTP and offline endpoints 16/17 are not current runtime paths.
