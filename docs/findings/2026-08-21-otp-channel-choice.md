Parent: ../reference/architecture.md

# Which channel sends the one-time code, 2026-08-21

Ryan asked for the actual best option, decided from what the Vietnamese
developer community recommends, searched in Vietnamese.

Replaces `2026-08-18-otp-channels.md`, now in `docs/legacy/`. That file asked
"is Zalo or WhatsApp free" and answered it correctly. It got the *decision*
wrong, on one point of fact: it assumed sending under a brandname meant
registering a brandname. It does not, and that assumption was the only thing
standing between this project and a code that reaches a real phone.

## Status

**Decided: SpeedSMS, shared brandname `Verify`.** Already the provider in
`Sms.kt`; the change today is one digit.

**Nothing has been sent for real** — nobody has a token, and buying one is
Ryan's call. What *has* been tested is the provider refusing, against the live
endpoint, which cost nothing and found two faults in our own code.

Zalo ZNS is not rejected. It is second, and it stays second until VGU answers
about its Official Account.

## Scope

Which channel, and who is allowed to use it. Searched in Vietnamese, on what
Vietnamese backend developers write for each other rather than what a vendor's
English landing page says. Delivery rates were not measured — nothing here has
sent a message.

## Evidence

### The mistake in the 2026-08-18 finding

That file said the paperwork for a brandname is "a term's work on its own", so
the code sends `"type": 2` — the long code, a message from a random number.

**Both halves of that are wrong for this project.**

A Vietnamese business registration certificate is required to register *your
own* brandname, such as `VGU`. It is not required to use the provider's
shared ones. SpeedSMS operates `Verify` and `Notify` as shared brandnames:
already registered with the carriers, available on a personal account, no
licence, no template approval, no registration fee and no monthly fee. That is
what `sms_type: 4` selects.

The long code is also the worse channel on delivery, not only on looks. A
Vietnamese carrier treats a code arriving from a random mobile number as the
shape spam takes; a code from a bank arrives under a brandname. Choosing type
2 to avoid paperwork that did not exist bought a message more likely to be
filtered.

`Sms.kt` now sends `"type": 4`. One digit, and the comment above it says why.

### What the Vietnamese community actually recommends

The consistent recommendation for a small Vietnamese project is a domestic SMS
provider, not Twilio and not Firebase. The reasons given are always the same
three: domestic providers are an order of magnitude cheaper per message into
Vietnamese networks, they need no card, and they answer support in Vietnamese.
SpeedSMS and eSMS are the two named most often at this size.

| | SpeedSMS |
| --- | --- |
| Sign up | Free, personal account, no card |
| Registration fee | None |
| Monthly fee | None |
| Per message | ~350 VND under a shared brandname |
| Voice OTP | About half that |
| Paperwork | None, for `Verify` / `Notify` |
| Delivery | Under 10 seconds, per their documentation — unmeasured by us |

A live demo form on their own site sends to one number without an account.
**That is the cheapest possible first test and it should be the first thing
done** — before any money moves, before any token exists.

### Zalo ZNS — right channel, still the wrong paperwork

The Vietnamese search turns up "OTP miễn phí" — free OTP — attached to ZNS
constantly. It is real and it is not about us: ZNS is free to the *recipient*,
because it costs a student no SMS charge and no data worth counting. The
sender pays, around 300 VND a message.

The eligibility wall from the 18th is unchanged and is not a shared-brandname
situation: a verified Official Account, a Vietnamese business registration
certificate, and two brand-proving elements. A student cannot register one. A
university can, and most already have.

So ZNS is not ruled out — it is **an ask with a lead time**, and it is
strictly better than SMS if VGU says yes: cheaper per message, richer message,
and Zalo is where students already are. Ask early, ship on SMS meanwhile.

### The provider refusing, tested — 2026-08-21

Done before any money moved, and it needs no account. The live endpoint was
asked with a bogus token, then the whole server was run against it.

The endpoint, the URL and the auth scheme are right. What it answers a bad
token is not from documentation:

    HTTP 401 {"name":"Unauthorized","message":"...","code":0,"status":401}

Then the server, with `SPEEDSMS_TOKEN=not-a-real-token`, asked for a code for
a test number:

| Checked | Result |
| --- | --- |
| The request is refused, not quietly accepted | `502 SEND_FAILED` |
| The server says why, in the log | `WARN sms.speedsms - SpeedSMS refused: HTTP 401 {...}` |
| No code is left that anybody could spend | 0 rows in `otp` |
| No account appears for a number never proved | 0 rows in `receivers` |
| Guessing anyway gets nowhere | `000000`, `123456`, `111111` → `401`, no token |
| The console provider still works, so this is the provider and not us | `200`, code printed |

**Two faults were found doing it, both in our code, neither on a device.**

The success check was read from their documentation and had never seen a real
answer. It matched `"status"` followed by an optional-quoted `1` — which also
matches the `1` at the front of `"status":12` and `"status":100`. A 1xx status
in an error body would have been read as a message successfully sent, and a
code nobody received would have opened an account. Now `1` refuses to be
followed by another digit, and there is a unit test.

Their errors also put an HTTP code in the same `status` field a success uses,
so "the field is present" would have been the wrong test entirely.

`speedSmsAccepted(httpCode, body)` is its own function now with five test
cases, including the real 401 above byte for byte. Every case is free; asking
the provider the same questions is 350 VND each.

**Still unproven, and only a real token proves it:** that a *success* is one
of the two spellings we accept. If it is a third, the failure is loud and safe
— the SMS arrives, the app says it did not, and the log carries the exact body
to fix it with.

### The threat the community warns about first

Every Vietnamese write-up on OTP leads with SMS pumping — draining somebody's
message budget by requesting codes for numbers you control or invent — and the
defence they name is capping the send as well as the verify.

**We had only half of it, and it was already exploitable.** The limit was one
code a minute per phone number, which stops a person pressing resend and stops
nothing else. `stress.py --writes` had been demonstrating the attack by
accident for days: 19,402 code requests in ten seconds, every one a different
number, from one laptop. Free today because codes print in a terminal. At the
350 VND above, the same ten seconds is about **6.8 million VND** — and this
server has been reached from public addresses.

Fixed before writing this file, because recommending a provider that charges
per message while that hole is open is not a recommendation. BUG-015: 200
codes an hour across everybody, from rows the `otp` table already keeps. Same
attack now: 22,309 requests, 205 sent, 22,104 refused.

### Not chosen, and why

| Route | Why not |
| --- | --- |
| Firebase phone auth | Needs the Blaze plan and a card before the first message. Ruled out by standing rule |
| Twilio | Works, no Vietnamese paperwork, and roughly 10–20× the per-message cost into Vietnam. Also card-gated |
| WhatsApp | Not what Vietnamese students use, and the same Meta verification wall |
| Telegram bot | Free and card-free, but the student must already have Telegram and message the bot first |
| Console provider | Stays. It is correct for every test on this laptop and costs nothing |

## Next

1. **Send one message from SpeedSMS's demo form to `+84908619328`.** Free, no
   account, five minutes. It answers the only question that matters — does a
   brandname message actually arrive on Ryan's phone, and how fast. If it does
   not, nothing below is worth doing.
2. **Open a SpeedSMS account and top it up small.** 100,000 VND is roughly 285
   messages, which is more than the whole Play test round needs.
3. **Put the token in the environment, never in the repo.** The variable is
   `SPEEDSMS_TOKEN`, read in `Config.kt`. Without it the server uses the
   console provider, which is why every test so far has worked. It is never a
   fallback when a real send fails — that stays a refusal.
4. **Then send one real code through our own server** and read it on the
   phone. Until that has happened, `Sms.kt` is documentation, not a tested
   path — `type: 4` included.
5. **Ask VGU about its Zalo Official Account**, and who administers it. Costs
   nothing, has a lead time, and decides the deployment channel rather than
   the test channel.

## Sources

- SpeedSMS pricing and brandname registration, `speedsms.vn` — shared
  `Verify` / `Notify` brandnames, `sms_type: 4`, no registration or
  maintenance fee
- SpeedSMS API documentation — `POST /index.php/sms/send`, basic auth
- Vietnamese-language provider comparisons, 2026 — SpeedSMS and eSMS against
  Twilio and Kaleyra for small projects
- Viblo, on SMS pumping — *"Đừng để Hacker 'đốt' sạch tiền SMS"* — names
  capping the send, not only the verify, as the defence
- Zalo ZNS price tables and Official Account verification requirements, 2026
