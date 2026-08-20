Parent: ../reference/architecture.md

# How the one-time code reaches a student, 2026-08-18

Ryan asked whether Zalo or WhatsApp could send the code for free. Researched
because the answer decides whether anyone outside this team can sign in at
all.

## Status

Answered. **Neither is free, and neither is available to Ryan personally.**
Nothing here is built or changed — this is the research behind a decision that
is his.

## Scope

Zalo ZNS, WhatsApp Business API, and Firebase phone auth, on two questions
only: what does a message cost, and who is allowed to send one. Delivery rates
and message design are not covered.

## Evidence

### Zalo ZNS — the right channel, the wrong paperwork

Zalo is what Vietnamese students actually use, so ZNS is the correct channel
on reach alone. It fails on eligibility.

| | |
| --- | --- |
| Price | 200–800 VND per message, by template type. No free allowance |
| Monthly | Official Account packages at 59,000 and 399,000 VND |
| Charged | Per message successfully delivered |

The blocker is not price. **A ZNS sender needs a verified Zalo Official
Account tied to a Vietnamese business registration certificate**, plus at
least two brand-proving elements — signage photographs, a company-owned
website, press links, or a domain registration from the Ministry of Industry
and Trade. Zalo only serves Vietnam-based companies.

A student cannot register one. VGU can: there is a Government Agency account
type for institutions, and most Vietnamese universities already hold an OA.
**That makes this an ask, not a purchase** — and it is free to Ryan if the
university sends it.

### WhatsApp Business API — wrong country

| | |
| --- | --- |
| Price | USD 0.004–0.0456 per authentication message, by country |
| Plus | A provider markup, typically USD 0.003–0.010 |
| Free tier | None for template messages. User-started conversations are free, which an OTP is not |

Meta charges per delivered template message and has since 1 July 2025.

Price is beside the point: **WhatsApp is not what Vietnamese students use.**
Zalo is. Sending codes over WhatsApp would mean most of the campus never gets
one. It also needs Meta business verification, which is the same wall as ZNS.

### Firebase phone auth — card-gated

Already ruled out by a standing rule, and it holds:

> Phone sign-in is never free, and since September 2024 SMS verification
> requires the Blaze plan with a billing account attached.

Email, social and anonymous sign-in are free to 50,000 monthly users on the
free plan. **Phone is explicitly excluded.** A card is required before the
first message.

### What is actually free

| Route | Cost | Catch |
| --- | --- | --- |
| The console provider we ship today | 0 | Codes print in the server terminal. Correct for testing, useless in a corridor |
| Telegram bot | 0, no card, no business check | The student must already have Telegram and message the bot first. Far behind Zalo in Vietnam |
| Email | 0 up to a few hundred a day | The account is a phone number — the shipper types a number at the cabinet. Email could carry the code while the number stays the identity, but that is a contract change |
| VGU's own Zalo OA | 0 to Ryan | The university pays and has to agree |
| SpeedSMS, already wired | ~300–500 VND per message | Real money, but small: a twelve-tester Play run is under 100,000 VND |

## What this means

There is no free channel that reaches a Vietnamese student's phone tonight.
The honest positions are:

1. **Testing stays on the console provider.** It is already built, costs
   nothing, and the ten-step test reads the code off the laptop. Nothing
   blocks the work Ryan asked for.
2. **A real deployment goes through VGU's Zalo OA.** Right channel, right
   country, and free to the project because the institution holds the account.
   It is a request with a lead time, so it wants starting early.
3. **SpeedSMS is the bridge** if a handful of outside testers are needed
   before the university answers. Not free, but the amounts are trivial at
   this scale.

## Next

1. **Ask VGU whether it has a Zalo Official Account**, and who administers it.
   This is the load-bearing question and it costs nothing to ask.
2. **Keep the console provider for every test until then.** It is not a
   limitation on anything currently planned.
3. **Do not build a Telegram or email path yet.** Both are real options and
   both are premature before the answer to 1.

## Sources

- [Bảng Giá ZNS 2026](https://www.smsthuonghieu.com/gia-zns/)
- [Zalo compliance and guidelines, Infobip](https://www.infobip.com/docs/zalo/compliance-guidelines)
- [Zalo ZNS template, VietGuys](https://www.vietguys.biz/en/martech/knowledge/zalo-zns-template-an-optimal-solution-for-customer-care-strategies-on-zalo)
- [WhatsApp API pricing explained, Authgear](https://www.authgear.com/post/whatsapp-api-pricing/)
- [WhatsApp Business API pricing 2026, Blueticks](https://blueticks.co/blog/whatsapp-business-api-pricing-2026)
- [Firebase Authentication pricing, Logto](https://blog.logto.io/firebase-authentication-pricing)
