Parent: docs/reference/architecture.md

# Courier APIs in Vietnam — what they cost and who can call them

## Status

Research only. **Nothing is integrated, and one finding says the integration cannot live in this repo.** Two decisions are open at the bottom.

## Scope

Ryan asked for a courier service that is generous with fees, lets the user pay instead, or is free — free first. This checks which Vietnamese couriers publish an API a student team can actually get into, what integration costs, and who can be made to pay the shipping fee.

It does **not** cover carrier prices per parcel. Those are negotiated per account and change; nothing here should be quoted as a rate.

## Evidence

Fetched 2026-08-11. Portals were read directly; the notes say plainly where a page is JavaScript-rendered and could not be read without an account.

| Carrier | Docs public? | Self-serve signup | Sandbox | Integration fee | Who pays the shipping |
| --- | --- | --- | --- | --- | --- |
| **GHN** (Giao Hàng Nhanh) | **Yes**, fully | **Yes** — "Create Account and Get Token, Client_ID, ShopID" | Not stated on the public docs | None found | **`payment_type_id`: `1` Shop/Seller, `2` Buyer/Consignee** |
| **Lalamove** | **Yes**, best of the set | Partner portal | **Yes — `rest.sandbox.lalamove.com/v3`, and the docs say "No additional credentials or approval"** | None found | Per order; postpaid plans exist on request |
| **GHTK** (Giao Hàng Tiết Kiệm) | Yes, `docs.giaohangtietkiem.vn` | Yes — "Tạo tài khoản" | Not readable | None found | Not readable — the docs are JS-rendered and the anchor returned nothing |
| **Viettel Post** | Portal exists | Unknown | Unknown | Unknown | Unknown — the page returns 90 characters of text, so it is a JS app |
| **Ahamove** | Portal exists | Unknown | Unknown | Unknown | Unknown — 895 characters, same reason |
| J&T, Ninja Van, BEST, SPX, VNPost | Marketing sites reachable | — | — | — | Not investigated further; none published an open developer portal on inspection |

Three things came out of this that matter more than the table.

**1. API access is free at every carrier that publishes one.** None of them charges to integrate. What you pay is per shipment, and that is a commercial account matter, not an API matter. So "absolutely free" is already the answer for the software; the money question is the carrier contract, which is not an engineering decision.

**2. "User pays instead" is one field, not a plan.** GHN's create-order takes `payment_type_id`, documented verbatim as *"Choose who pay shipping fee. 1: Shop/Seller. 2: Buyer/Consignee."* Sending `2` puts the fee on the receiver. It is chosen per order, so the app never has to commit to a model.

**3. Lalamove is the only one you can build against today.** Its sandbox needs no approval and no contract — an API key and secret from the portal, HMAC-SHA256 signing, and `rest.sandbox.lalamove.com/v3`. Every other carrier needs a real merchant account before the first request. For a team with no signed contract, that is the difference between starting this week and waiting.

### The finding that decides where this code goes

**Every one of these authenticates with a long-lived shop token or an API secret, and none of it can go in the app.**

- GHN sends `Token:` and `ShopId:` headers. Lalamove signs each request with an API **secret**. An APK is a zip file; anything compiled into it is readable by anyone who installs it. A leaked courier credential can create real shipments and move real COD money.
- This repo's own rule already forbids it — CLAUDE.md, Engineering standards: *"never paste a key, token, cookie or private address"*, and *"no key, token, password or private address inside app code"*.
- Status updates arrive by **webhook**, which needs a public HTTPS endpoint. The app is not one.

So a courier integration is **Server-team work**, and it is out of scope for this repo as CLAUDE.md defines it: *"Out of scope: Server code, database design, cabinet hardware."* What belongs here is the receiver's view of a delivery, behind a route in `api-contract.md` — which is a contract change, and a contract change needs both teams.

There is also a product question underneath, and it should be answered before anyone writes an integration: **the current flow has no step a courier API would serve.** In PRODUCT.md a shipper walks up to the cabinet and drops a parcel. Nothing in the app books a delivery. A carrier integration is a new capability, not a missing piece — worth having, but it needs its own ADR rather than arriving as a plugin.

## Next

1. **Decide what "courier service" is for.** Tracking a parcel that is already on its way, letting a receiver send one out, or making the locker a carrier drop point. These are three different products and only the third fits the current flow.
2. **If it goes ahead, write the ADR**, because it crosses into Server-team scope and adds a carrier as a dependency.
3. **Start on Lalamove's sandbox** whatever the eventual carrier. It is the only one that runs without a contract, and the shape of the work — create, track, webhook — is the same everywhere.
4. **Recommend GHN for the real thing.** Public docs, self-serve token, receiver-pays is one integer, and it has a `Get Station` call, which is the nearest published concept to a locker as a delivery point.
5. **Never put the carrier credential in the app.** The app calls our server; our server holds the token and calls the carrier.
