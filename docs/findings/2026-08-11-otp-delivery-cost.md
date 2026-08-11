Parent: docs/superpowers/specs/2026-08-09-otp-sender-design.md

# What it costs to deliver a one-time code in Vietnam

## Status

Research. **Nothing needs to change today** — SpeedSMS is already built and is the right choice for the demo. One finding is worth acting on later, and one claim in the original spec turns out to be beatable.

## Scope

Ryan asked for an OTP sender that is generous with fees, lets the user pay instead, or is free — free first. This prices every route a one-time code can take to a Vietnamese phone.

The code that sends is already written and swappable: `SmsProvider` in `src/otp-server/`, with `SpeedSms` and a console `LogSms` behind it. Changing carrier is one new file, not a rewrite. So this is a cost question, not an engineering one.

## Evidence

Fetched 2026-08-11 from each provider's own pages.

| Route | Cost per code | Free allowance | Reaches a VGU student? |
| --- | --- | --- | --- |
| **Telegram Gateway** | **$0.01** (~250đ) — *"up to 50x cheaper than SMS"*, their words | **Codes to your own number are free**, plus a free test environment. **You are not charged when a code cannot be delivered** | Only if they use Telegram. In Vietnam most do not |
| **Zalo ZNS** | 200–800đ by template type | Monthly quota earned from past usage, since 1 Nov 2024 | **Yes — Zalo is on nearly every Vietnamese phone.** Needs an Official Account and business verification |
| **SMS long code** (SpeedSMS, what we use) | ~390–500đ | Free credit on registration | **Yes. Any phone, no app, no data** |
| **SMS brandname** | 600–1,000đ | — | Yes, and the sender shows a name instead of a number |
| **Firebase phone auth** | Per SMS, on the Blaze plan | Identity Platform has a free tier for phone as a provider, but **the SMS itself is billed** | Yes, but it replaces our own login — rejected in [ADR 0011](../adr/0011-google-sign-in-no-passwords.md) |

### Three things worth saying plainly

**1. Nothing is free for real users.** Every route bills per delivered code. "Absolutely free" does not exist for SMS in Vietnam, and any page that says otherwise is selling a trial credit. What is genuinely free is *development*: Telegram Gateway never charges for codes you send to your own number, and our own `LogSms` prints the code to the console and costs nothing at all.

**2. "The user pays instead" has a real form here, and it is not SMS.** Zalo and Telegram ride the data the student already pays for. That makes them cheaper for us *because* the carrier leg disappears — 200–800đ against 600–1,000đ, or $0.01. It is the same idea Ryan meant, arriving through the channel rather than through a billing flag.

**3. The spec's reason for choosing SpeedSMS is now half wrong.** It says SpeedSMS is "cheapest per SMS (~390–500đ)". Cheapest *SMS*, yes. Telegram Gateway delivers the same code for about half that, and Zalo ZNS can beat it too. SpeedSMS is still the right choice, but for a different reason than the one written down: **it is the only one that reaches a phone with no app, no account and no data.** That is what a login must not get wrong.

## Next

1. **Keep SpeedSMS.** At demo scale the cost is not a real number — a hundred sign-ins is under 50,000đ, and registration credit covers most of it. Changing carrier to save 200đ a message is optimising the wrong thing while P0-04 is still open.
2. **Correct the spec's stated reason** so a future reader does not re-derive it. Done in this change.
3. **If cost ever matters, add Zalo ZNS, not Telegram.** Zalo reaches Vietnamese students; Telegram does not. It would be a new `SmsProvider` file and nothing else.
4. **Use Telegram Gateway's free self-test if OTP testing ever gets expensive.** Codes to your own number cost nothing, which is most of what development needs.
5. **Never let the channel become the identity.** Whatever carries the code, the account is the phone number — [ADR 0011](../adr/0011-google-sign-in-no-passwords.md).
