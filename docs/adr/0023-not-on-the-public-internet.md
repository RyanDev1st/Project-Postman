# 0023 — The locker lives on the campus network, not the internet

- **Status:** accepted
- **Date:** 2026-08-22
- **Deciders:** Ryan

## Context

Work on 2026-08-21 closed the common web attacks one at a time and ended with
one item that code in this repository cannot close: a distributed denial of
service. The recommendation was a domain name with Cloudflare in front, which
is free and would also have ended the self-signed certificate problem.

Ryan, 2026-08-22: *"there isnt a domain name, this isnt going on web."*

That is a constraint on the whole shape of the system, not a preference about
one recommendation, so it is written down here rather than quietly dropped
from a to-do list.

## Decision

**The server is reachable from the campus network and from nowhere else.** No
domain, no public certificate, no port forwarded from the internet.

## Consequences

**A distributed denial of service leaves the threat model, and the defences
against it stay.** A botnet cannot reach a machine it cannot address. The
attacker who can still reach this server is a person on campus Wi-Fi with a
laptop — and that is precisely what the per-caller ceilings, the request
timeouts and the body-size limit were measured against. One laptop reached
about 114,000 requests a minute in testing; the ceilings sit three orders of
magnitude below that. Nothing about the hardening changes. What changes is
that the honest note beside it is now *not applicable here*, rather than
*unfixed*.

**HSTS stays off, permanently.** It was off because the cabinet screen shares
a host with the server and would have been forced to HTTPS it cannot speak.
Now there is a second reason that does not expire: HSTS protects a downgrade
that only matters between a browser and a public site, and there is no public
site. The `send_hsts` flag stays, because a future deployment that does get a
domain should turn it on in the same change.

**The certificate is self-signed forever, and that has a cost nobody had
priced.** No public authority signs a certificate for a private address, so
the certificate this server presents will always be one we made.

The app's **debug** build trusts certificates a person has installed by hand,
which is why testing works. The **release** build has no such rule and trusts
only the system store — so *a release APK cannot talk to this server at all*.
That was invisible while every build was a debug build, and it becomes a
shipping blocker the day one is not. The fix is to ship the cabinet server's
certificate inside the app and trust that one, which also pins it: better than
a public certificate, not worse, because the app would then refuse any
certificate but ours. It needs a real deployment certificate to exist first.
Raised as a task rather than guessed at here.

**Every tester still installs the certificate by hand**, once per phone, until
the task above is done. Two minutes in Settings, and there is no way around it
that is not a lie about what the app trusts.

**Nothing else moves.** A stolen database file, a stolen laptop, a person
standing at the cabinet, somebody on the same Wi-Fi — every threat this
project has actually met is local, and local is exactly where this system now
lives. Backups and the hashing key of [ADR 0022](0022-a-key-outside-the-database.md)
matter more under this decision, not less: on a private network the realistic
loss is a machine walking away, not a packet flood.

## Alternatives considered

**A free subdomain, or a Cloudflare tunnel.** Both work and neither needs a
card. Both also put a locker that opens real doors on the public internet in
exchange for convenience during testing, and the person who would have to
answer for that decided against it. The threat that reaches a machine on a
university LAN is a person who is already on the campus.
