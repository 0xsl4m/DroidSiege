# DroidSiege — Architecture & Contributing

This document explains how DroidSiege is structured and how to add a new challenge. It is a
guide for contributors — it deliberately does **not** contain solutions, flags, or a
challenge-by-challenge spoiler catalog.

---

## Design principles

1. **Every vulnerability has 4 difficulty tiers.** The *same* vulnerability class is presented
   at increasing difficulty so a learner can climb, not just tick a box.
   - 🟢 **L1 Easy** — textbook, no defenses.
   - 🟡 **L2 Medium** — light validation / basic obfuscation / a small twist.
   - 🟠 **L3 Hard** — requires a real bypass (root/pinning/signature/anti-tamper).
   - 🔴 **L4 Insane** — requires chaining multiple issues, native code, or tooling (Frida/objection).
2. **A realistic app shell.** Challenges live inside believable screens (login, wallet, chat,
   profile, settings) rather than a bare list of buttons.
3. **Teach the fix.** Every challenge has a `secure` and an `insecure` implementation behind a
   runtime toggle, so the learner sees the diff, not just the exploit.
4. **CTF loop.** Each challenge yields a flag → scored on a local scoreboard → hints cost points.
5. **Modern, idiomatic code.** Vulnerable code must look like code a real team would write today
   (Kotlin, Compose, Coroutines, Room, Retrofit/OkHttp, Hilt).

---

## Module & package layout

```
DroidSiege/
├── app/                        # Application: shell, navigation, CTF engine, DI
│   └── src/main/java/com/droidsiege/
│       ├── core/               # Challenge model, registry, difficulty, flags
│       ├── engine/             # Scoreboard (Room), hints, secure/insecure toggle
│       ├── ui/                 # Compose theme, navigation, shared components, home
│       └── challenges/         # One package per category
├── backend/                    # Intentionally-insecure Ktor API + Docker
└── docs/                       # Repo docs & (spoiler) solution write-ups
```

Each vulnerability **category** is a package under `challenges/`. Each **challenge** implements
the `Challenge` contract and registers itself, so the UI and scoreboard stay data-driven.

## Vulnerability scope

DroidSiege targets the full **OWASP Mobile Top 10 (2024)** — insecure storage, weak
cryptography, credential/auth issues, insecure communication, IPC/component misconfiguration,
input/WebView validation, privacy, binary protections, and supply chain — plus modern techniques
(task-affinity hijacking, tapjacking, insecure Custom Tabs, native/JNI). See the in-app category
list and the coverage table in the [README](../README.md) for current status.

---

## The `Challenge` contract

```kotlin
data class ChallengeId(val category: String, val slug: String, val level: Difficulty)

enum class Difficulty(val label: String, val points: Int) {
    EASY("L1 · Easy", 100),
    MEDIUM("L2 · Medium", 200),
    HARD("L3 · Hard", 400),
    INSANE("L4 · Insane", 800),
}

interface Challenge {
    val id: ChallengeId
    val title: String
    val brief: String                 // player-facing objective, no spoilers
    val owaspRefs: List<String>       // e.g. ["M9", "MASVS-STORAGE-1"]
    val hints: List<String>           // tiered; each reveal costs points
    val flag: String                  // format: DS{category_slug_Ln_token}
    fun validateFlag(input: String): Boolean = input.trim() == flag
    @Composable fun Screen(secureMode: Boolean)  // vulnerable when false, hardened when true
}
```

- **Flags** follow `DS{category_slug_Ln_token}` and validate by trimmed exact match.
- **`secureMode`** is driven by the global Secure/Insecure toggle. When `true`, the screen must
  render the hardened implementation and the intended exploit must fail.

---

## Adding a new challenge

1. Pick (or create) a category package under `challenges/`.
2. Implement the `Challenge` interface for **each** difficulty tier you're adding.
3. Provide **both** a vulnerable and a hardened code path, gated by `secureMode`.
4. Register the challenge in the category's registrar so it appears on the home screen.
5. Add 2–3 tiered hints and a solution write-up under `docs/solutions/<category>/<slug>.md`.
6. Update the coverage table in the [README](../README.md).

A challenge is only complete when it has: both paths working, a valid flag, hints, and a
write-up. See [DISCLAIMER](../DISCLAIMER.md) for the rules of use.
