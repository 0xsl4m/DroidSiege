# Contributing to DroidSiege

Thanks for wanting to add to the range! DroidSiege is a teaching range: every challenge
exists to demonstrate a real mobile vulnerability class, its exploitation, **and its fix**.

## The Challenge contract

Every challenge in the app:

1. **Has both paths.** A vulnerable behavior (default, insecure mode) and a hardened
   variant that genuinely blocks the exploit. The Secure/Insecure toggle switches them.
2. **Escalates.** A family ships four tiers (L1 🟢 EASY → L4 🔴 INSANE) with growing
   exploit complexity — new tool, new chain step, or a bypass of the previous tier's fix.
3. **Carries a flag.** Format `DS{<category>_<slug>_L<1-4>_<6hex>}`, unique per tier,
   revealed only through the intended exploit path (never on the secure path, never
   shared across tiers).
4. **Teaches.** A Learn tab with the theory, the vulnerable snippet, the fix snippet,
   MASTG references, and a takeaway.
5. **Documents.** A write-up under `docs/solutions/<category>/<slug>/README.md` covering
   every tier (setup, exact exploit steps, flag, fix) plus a tooling pack under `tools/`
   that actually reproduces each flag.

## Adding a family

1. Model it: a `TieredChallenge` subclass per tier in
   `app/src/main/java/com/droidsiege/challenges/<category>/`, registered in the category's
   `CategoryContributor`, which `DroidSiegeApp` passes to the `ChallengeRegistry`.
2. Build both paths; make sure the secure path is a real fix, not a message.
3. Add the scoreboard artifacts (flag, hints, LearnContent).
4. Write the solution pack: write-up + tools that mirror the app derivations exactly.
5. Run the gates: `./gradlew :app:assembleDebug :app:detekt :app:ktlintCheck :app:testDebugUnitTest`
   and `./gradlew :backend:build` if you touch the lab backend.

## Ground rules

- Intentionally vulnerable **only inside the lab**: no exploit code that targets anything
  but this app/backend, no real vulnerable third-party libraries (simulate CVE classes).
- No secrets in the repository beyond challenge flags (they are intended data).
- Keep commit history clean and conventional (`feat:`, `fix:`, `docs:` …).
- Every PR: description of the vulnerability class, both paths demonstrated, and the
  write-up in the same change.
