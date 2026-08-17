# 0021 — Build for the proposed hardware, before it is bought

- **Status:** accepted
- **Date:** 2026-08-17
- **Deciders:** Ryan, IT team

## Context

The hardware team put a bill of materials in front of the school for funding. Total 12,040,000 VND. Nothing has been bought.

| # | What | Model | Qty |
| --- | --- | --- | --- |
| 1 | Main control computer | Orange Pi 5 (8GB) **or** Raspberry Pi 4 B | 1 |
| 2 | Touch screen | 10.1" HDMI touchscreen LCD, **1024 x 600** | 1 |
| 3 | Barcode / QR scanner | GM65 module, USB/UART | 1 |
| 4 | Lock | LY-03 solenoid, 12V, with bracket | **25** |
| 5 | Lock controller | 32-channel RS485 Modbus RTU relay board | 1 |
| 6 | **Item detection sensor** | E18-D80NK infrared, 3–80cm | **25** |
| 7 | Sensor microcontroller | ESP32 DevKit V1 — *"transmits data to Pi"* | 1 |
| 8 | Network | ZTE MF833 4G dongle — noted *"switch to a LAN cable on the school wifi"* | 1 |
| 9 | Power | 12V 10A switching supply | 1 |
| 10 | Storage | MicroSD 32GB Class 10 / A1 | 1 |
| 11 | Connectors and cable | — | 1 |

Until now this team has built against unknowns. [ADR 0009](0009-start-phase-1-early.md) let the cabinet screen be built in a browser because *nobody could say what the screen was*. [ADR 0006](0006-no-sensor.md) recorded that there is no sensor, and accepted the consequence.

This list answers both, and answers them differently from what we assumed.

## Decision

**Build against these numbers now, and write down that they are a proposal.** Waiting for delivered metal costs weeks and buys nothing: every number here is either something the code can be shaped around cheaply, or something that changes one line.

Five things follow.

### 1. The cabinet screen is 1024 x 600, and that is short

Not merely a size — a **short** one. 600 pixels of height, landscape, at 10.1 inches. Every earlier assumption about this screen came from a desktop browser at 1280x800 or 1920x1080, which is 200 to 480 pixels taller.

A shipper's flow that scrolled on this screen would be a flow that fails standing up, holding a parcel. So it is built to fit 1024 x 600 **without scrolling**, and it stays relative rather than hard-coded, because a proposal is not a purchase order.

### 2. There is an item sensor, and it does not do what a door sensor does

This is the part most worth reading twice. ADR 0006 accepted a real risk: a driver opens a door, walks away still holding the parcel, and the system records a delivery that never happened. There was no sensor, so there was no way to know.

**An E18-D80NK in a box reports whether the box has something in it. It does not report whether the door is shut.** Those are different facts and only one of them was ever the hard one:

| Question | Door sensor | Item sensor | Which one matters |
| --- | --- | --- | --- |
| Is the door shut? | yes | no | tidiness |
| Did the parcel actually go in? | no | **yes** | **this is ADR 0006's accepted risk** |
| Did the parcel actually leave? | no | **yes** | **this is what "collected" should mean** |

So the sensor does not reverse ADR 0006 — it makes the thing ADR 0006 gave up on possible, and leaves the thing it settled for untouched. ADR 0006 stands. A future ADR may record that a delivery is evidenced by the box filling rather than by the door shutting, once there is hardware to test that on.

It also answers [BUG-009](../reference/bug-log.md) without a contract change: a cabinet that can see a box go from full to empty does not need to be told whether an open was a drop or a collect. It can see which happened.

### 3. The Pi polls the server. The ESP32 does not

Our contract and `architecture.md` describe the ESP32 as the thing that asks the server for work. This list demotes it: the ESP32 reads 25 sensors and *transmits data to the Pi*, and the Pi is the computer with the screen and the network.

That is a better shape than ours and we take it. The Pi already has to run a browser for the cabinet screen and already has to hold a TLS trust store; splitting the network across two devices would have meant two things to configure, two certificates and two keys.

`scripts/cabinet-agent.py` is therefore a model of **the Pi's job**, not the ESP32's.

### 4. Twenty-five boxes, not twenty

`Demo.kt` seeds 20 and the app's own words say "20 doors free" and "All 20 boxes in use". Both are wrong against this list, and both were guesses to begin with. The count belongs to the cabinet row in the database, which already carries it — the screens must read it rather than repeat it.

The relay board has 32 channels, so 25 is the order rather than the ceiling.

### 5. The GM65 scanner does not fit the product we agreed, and is not ours to resolve

[ADR 0003](0003-parcel-locker-product.md) is built on one sentence: **the phone scans the cabinet, never the other way round.** That is what makes a photograph of the screen worthless, because identity comes from the app login and not from the code.

A barcode scanner bolted to the cabinet only makes sense if something is presented *to* it — a student card, or a code on a phone. The Server team's own learning plan names a `card_barcode_map` table mapping student-card barcodes to users, so this is not an accident: someone is designing card-based identification.

**That is a product decision, not an implementation detail, and it changes who the system trusts.** It is raised here and deliberately not resolved. Nothing in this repo reads a cabinet-side scanner, and nothing will until there is an ADR saying it should.

## Consequences

- The cabinet screen is built for 1024 x 600 and checked at that size.
- The box count is read from the server, never written into a screen.
- The hardware interface — what the Pi must implement — is written down in [cabinet-hardware.md](../reference/cabinet-hardware.md) so the Server team can build against it before either side has metal.
- Every number above is an assumption with a date. When the hardware is bought, the differences are a list of small edits, not a redesign.
- The GM65 question goes to Ryan and the hardware team. Until it is answered, the drop flow assumes a shipper types a phone number, exactly as [ADR 0003](0003-parcel-locker-product.md) describes.
