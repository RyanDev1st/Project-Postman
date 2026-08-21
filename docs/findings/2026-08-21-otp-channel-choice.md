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

**Decided: SpeedSMS, shared brandname `Notify`** — `sms_type` 4. Already the
provider in `Sms.kt`.

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
own* brandname, such as `VGU`. It is not required to send under one of theirs.
Their own SDKs name it, and this is read from the official downloads on
2026-08-21, not from a blog:

    SMS_TYPE_NOTIFY = 4          // sms gui bang brandname Notify   (PHP)
    TYPE_BRANDNAME_NOTIFY = 4    // Gửi sms sử dụng brandname Notify (C#)

`Notify` is SpeedSMS's brandname, already carrier-registered. Type 3 is *your
own* brandname and needs a `sender`; type 4 does not.

Their public price list prices the own-brandname route: **200,000 VND to
create a brandname and 200,000 VND a month to keep it.** That is the wall, and
type 4 goes round it.

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

**There is no demo form.** An earlier draft of this file said there was one
and that it should be the first test. Their site was checked on 2026-08-21:
the only form on it is a contact form. The zero-cost first test is `/user/info`
instead — see Next.

The 350 VND figure is from community comparisons, **not confirmed against a
type-4 price on their own site**. Their published table covers the
own-brandname service. Read the real rate in the account dashboard before
topping up.

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

### Two faults in our request, found in their SDK

Neither is visible without an account, and both would have made the first paid
send fail in a way that looks like the provider's fault.

**The field name.** We sent `"type"`. Their JavaScript and PHP SDKs send
`"sms_type"`; their C# SDK sends `"type"`. Their own downloads disagree. An
unknown field is ignored, and the PHP SDK shows the default when it is absent:
`SMS_TYPE_CSKH = 2`. So sending only the wrong spelling means every message
quietly goes out as a customer-care long code — from a random number, the shape
carriers filter — while the code looks like it asked for a brandname. Both
spellings are sent now. Costs nothing and removes the guess.

**The brandname name.** This file first said `Verify`. Type 4 is `Notify`.

`sender` is now sent as an empty string, as every SDK does. Type 4 does not
use it; only 3, 5, 7 and 8 require one.

### The live API, with a real token — 2026-08-21

Ryan supplied a token. `/user/info` answered:

    {"status":"success","code":"00","data":{"email":"...","balance":2000,"currency":"VND"}}

2,000 VND of credit, and **nothing was spent** — every send below was refused,
and a refusal is free. The balance was 2,000 before and after.

**Three faults in our request, none visible without a token.**

**1. The success spelling. This one was fatal.** The check accepted only
`"status":1`, taken from a write-up. A real success is `"status":"success"`.
Every delivered message would have been reported as a failure — the SMS
arrives, the app says it did not, the code is deleted as unsent, nobody
registers. It had already been widened to accept both on the strength of
their SDK; the live answer confirms which one is real.

**2. Vietnamese text made the send fail outright.** Same account, same
everything, only the content differing:

    "content":"Mã xác thực 123456"           -> {"status":"error","code":"101",
                                                 "message":"Invalid or missing
                                                 parameters"}
    "content":"Mã xác thực ..."  -> past parameter validation

Their API refuses raw UTF-8 in the body. Every code this server sends is
Vietnamese, so **no code could ever have been delivered**, and the reason came
back as "invalid parameters" — which points at the phone number or the type,
not at the message. This is exactly what PHP's `json_encode` does by default,
which is why their own SDK never hits it and their documentation never
mentions it. `asciiJson` now escapes everything above ASCII.

**3. `sender` is required, and the SDKs imply it is not.** They demand one only
for types 3, 5, 7 and 8, and type 4 is meant to use SpeedSMS's own `Notify`.
The live API disagrees. Every `sms_type` from 1 to 5, with `sender` empty or
set to `Notify`, `Verify`, `SpeedSMS` or `VGU`, answered the same:

    {"status":"error","message":"sender not found"}

`to` was tried as a string and as an array; both behave identically, so ours
stays a string.

**Where it stands.** The server's own endpoint, running the real Kotlin,
against the live API:

    WARN sms.speedsms - SpeedSMS refused: HTTP 200 {"status":"error","message":"sender not found"}

`sender not found`, not `code 101` — so the request is now well-formed and the
one thing left is on the account, not in this repo. It is `SPEEDSMS_SENDER`,
read in `Config.kt`.

**So the "no paperwork, works immediately" claim earlier in this file is too
strong.** A sender has to exist on the account before anything sends. Whether
the shared `Notify` can simply be switched on for a personal account, or
whether it needs a request to their support, is not established — nobody has
been able to log in to the dashboard and look.

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

Read from their official SDK downloads, so each step below has an exact call.

1. **Open a free account at `connect.speedsms.vn`.** No card. Copy the API
   access token.
2. **Prove the token works without sending anything.** `/user/info` costs
   nothing and needs no balance:

       curl -u "<TOKEN>:x" https://api.speedsms.vn/index.php/user/info

   A balance and an account name means the token and the auth scheme are
   right. `{"...","status":401}` means the token is wrong — the same body this
   project already tested against.
3. **Read the real type-4 price in the dashboard before topping up.** The
   ~350 VND in this file is from community comparisons and is not confirmed
   against their own type-4 rate.
4. **Top up small.** 100,000 VND at that rate is ~285 messages, more than a
   whole Play test round needs.
5. **Then one real send.** `SPEEDSMS_TOKEN=<token>` in the environment — read
   in `Config.kt`, never in the repo — and register from the app. Watch four
   things on the phone: the sender says `Notify` and not a phone number; it
   arrives in under 10 seconds; `Mã xác thực` renders with its diacritics; it
   is one message and not two. Accented SMS is Unicode, 70 characters a part
   against 160, and ours is about 56.
6. **Check the bill.** `/user/info` again. Messages sent × the rate should
   match. A gap means something sends twice, and a retry loop on a paid
   channel is worth catching on day one.
7. **Ask VGU about its Zalo Official Account**, and who administers it. Costs
   nothing, has a lead time, and decides the deployment channel rather than
   the test channel.

### Not used: their own 2FA API

`TwoFactorAPI.php` in the same download offers `/pin/create` and `/pin/verify`
— SpeedSMS generates, sends and checks the code for you.

**Deliberately not used.** It moves the lockout counter onto their server, and
this project has a standing rule that the counter survives a power cut, plus
its own rules that a wrong code never reveals whether the number has an
account and that five tries is five tries ever. Those are enforced in `Otp.kt`
and tested by `checkloop.py`. Handing the code to a provider gives all of that
away to save a table. We send the message and keep the decision.

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
