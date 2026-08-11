# 0012 — Passwords, set on a phone account. No email anywhere

- **Status:** accepted
- **Date:** 2026-08-11
- **Deciders:** Ryan
- **Supersedes:** [0011](0011-google-sign-in-no-passwords.md)

## Context

[ADR 0011](0011-google-sign-in-no-passwords.md) decided there would be no passwords. The argument was that a password is a second thing to steal, that its reset path is a one-time code to the phone, and so it adds attack surface without adding a way in. That argument was put to Ryan with the cost stated. **He chose passwords anyway.** This ADR records the decision and the shape that makes it as safe as it can be.

Everything else 0011 decided still holds and is restated below, so this file is the whole picture rather than half of it.

## Decision

**Three ways into one account, and the account is always a phone number.**

| Way in | How | Makes an account? |
| --- | --- | --- |
| One-time code | `POST /auth/request-code`, `POST /auth/verify-code` | **Yes.** The only one |
| Google | `POST /auth/google` — endpoint 19 | No. Opens an account a code already proved |
| Password | `POST /auth/password-login` — endpoint 21 | No. Set by `POST /auth/set-password` on a live session |

**There is no email address anywhere in this, and no register-with-a-password route.** A password is set on an account that a one-time code has already proved, and you sign in with the phone number and the password. The reasoning is the same one that kept Google from making accounts: the shipper finds the receiver by typing a **phone number** on the cabinet screen, so an account identified by an email could never be sent a parcel. It would look complete and receive nothing.

**There is no password reset endpoint, and no reset email.** Forgetting a password is the ordinary one-time code, then setting a new one — two calls that already existed. Setting a password clears any lockout, because the person just proved themselves another way. The shortest reset path is the one already in the building.

### What makes it as safe as a password gets

| Thing | Choice | Why |
| --- | --- | --- |
| Hashing | **Argon2id**, 19 MB, 2 passes, 1 lane | OWASP's first choice and its minimum parameters. Memory is the point: a graphics card can try billions of SHA-256 guesses a second because each is tiny, but every Argon2id guess must hold 19 MB |
| Parameters | Stored **inside** each hash | Raising the cost later does not strand passwords hashed before the change |
| Salt | 16 random bytes, fresh per hash | Two people who pick the same password must not produce the same row |
| Comparison | Constant time | A comparison that stops at the first wrong byte leaks, through timing, how much of a hash a guess got right |
| Wrong tries | 5, then locked 15 minutes | Mirrors `wrong_tries_before_lock` in `config/settings.json` |
| **Where the counter lives** | **On disk, SQLite, `synchronous=FULL`** | The rule is that a lockout must survive a power cut. A counter in memory resets when the process dies, so anyone who can crash the server — or simply wait for a restart — gets their five guesses back |
| Rules on the password itself | Length only: 8 to 200 | A pile of must-contain-a-symbol rules mostly teaches people to write `Password1!` and reuse it everywhere |
| What a failure says | `WRONG_PASSWORD`, always | One wording for wrong, locked out, and no such account. Telling them apart would answer two questions a sign-in screen must not answer: whether a number is registered here, and whether the lockout has started. Same rule the one-time code already follows, where `WRONG_CODE` also means expired and out of tries |
| An unknown number | Hashed anyway, against a throwaway hash | Otherwise an unknown number answers in a millisecond and a known one in tens of them, and that gap is a way to ask "is this person registered?" without being told |

## Why

Because Ryan asked for it after the case against was made. The argument in 0011 has not changed and is not withdrawn: this genuinely does add attack surface that phone-plus-code did not have. What this ADR buys is that the surface is as small as it can be — no email, no reset flow, no new delivery channel, no account that a password alone can create, and the one counter that must outlive a crash actually does.

## What we accept

- **A stolen password now opens an account**, where before there was nothing to steal. That is the whole cost, and it is taken knowingly.
- **A database file that must be looked after.** `accounts.db` holds every hash. It is the first thing in this project that is worth stealing, and the first that must never be committed, copied to a laptop, or left in a backup nobody owns.
- **Durability is designed for, not proven.** `synchronous=FULL` is what makes the counter survive a power cut, and a test cannot pull the plug out. The tests prove the counter survives a **restart**; the pragma is what carries it the rest of the way.
- **Two more things to keep in step.** Lockout numbers now exist in `config/settings.json` and in `Accounts.kt`, repeated rather than read, the same way `AuthStore` already repeats the code lifetime.

## Affects

- **P2-09** — new task, the app side.
- **Endpoints 20 and 21**, and three refusal codes in [api-contract.md](../reference/api-contract.md).
- `src/otp-server/src/main/kotlin/otp/password/` — `Passwords.kt`, `Accounts.kt`, `PasswordRoutes.kt`.
- Two new dependencies, with reasons: Bouncy Castle for Argon2id, SQLite for the counter that must survive.
- **Supersedes [ADR 0011](0011-google-sign-in-no-passwords.md).** Its Google decision is restated here unchanged; only its no-passwords decision is reversed.
