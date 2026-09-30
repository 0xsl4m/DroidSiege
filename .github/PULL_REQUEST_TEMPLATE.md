## What

<!-- family/tier added or fixed; vulnerability class -->

## Both paths

- Vulnerable behavior:
- Hardened behavior (must genuinely block it):

## Flags

<!-- one unique DS{...} per tier, revealed only via the exploit -->

## Write-up & tools

<!-- docs/solutions/<category>/<slug>/README.md + tools/ updated in this PR -->

## Gates

- [ ] `./gradlew :app:assembleDebug :app:detekt :app:ktlintCheck :app:testDebugUnitTest`
- [ ] `./gradlew :backend:build` (if backend touched)
- [ ] ktlint clean, no attribution in commit messages
