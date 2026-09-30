<div align="center">

# 🏰 DroidSiege

**An intentionally vulnerable Android app with *graded difficulty levels* for every vulnerability.**

Lay siege to a modern Kotlin + Jetpack Compose app — from `Easy` warm-ups to `Insane` multi-stage chains — and learn mobile security the way real targets are built.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![OWASP](https://img.shields.io/badge/aligned-OWASP%20MASVS%20%7C%20MASTG-000000)](https://mas.owasp.org/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

</div>

> [!WARNING]
> DroidSiege is **deliberately insecure**. Read the [**DISCLAIMER**](DISCLAIMER.md) before you build or run it.
> Use a dedicated emulator or test device — never a personal daily driver, and never expose the backend publicly.

---

## ✨ What makes DroidSiege different

Most vulnerable Android apps give you one flat example per bug. DroidSiege is built around three ideas the others miss:

| Idea | What it means |
|------|---------------|
| **🎚️ Difficulty levels per vulnerability** | Every vulnerability ships in **4 tiers** — 🟢 `L1 Easy` → 🟡 `L2 Medium` → 🟠 `L3 Hard` → 🔴 `L4 Insane`. Beat the easy version, then face the same class with validation, obfuscation, and bypass requirements layered on. |
| **🚩 Built-in CTF engine** | Each challenge hides a **flag**. Submit it in-app to score points, unlock hints, and track progress on a local **scoreboard**. |
| **🔁 Secure / Insecure toggle** | Flip any challenge between its **vulnerable** and **hardened** implementation to see *exactly* what the fix looks like — turning every bug into a lesson. |
| **📖 In-app Learn tab** | Every challenge has a built-in lesson: short theory, MASTG references, and a **before/after code diff** of the vulnerable vs. hardened code — so the app is also a course. |
| **🦹 Companion attacker app** | A second app, **SiegeRaider**, launches **live exploits** against SiegeApp (exported components, insecure providers, intent redirection, deep-link hijack, tapjacking, task hijack) — see real IPC attacks, not just descriptions. |
| **🧰 Tooling packs** | Each challenge ships ready-to-run **Frida scripts, objection commands, and Burp configs** in its solution write-up — bridging learning and hands-on practice. |

Built with a **modern stack** (Kotlin, Jetpack Compose, Coroutines, Room, Hilt) and a companion **intentionally-insecure backend** for server-side classes (IDOR/BOLA, broken auth) — so it mirrors how apps are actually written today, not a decade ago.

---

## 🧩 The app: SiegeApp

Instead of a bare list of buttons, DroidSiege is a believable fictional **"super app"** called
**SiegeApp**. Each module is a realistic screen that naturally hosts a family of vulnerabilities —
so you learn to find bugs in context, the way you would against a real target.

| Module | Hosts |
|--------|-------|
| 🔐 **Login / Auth** | credential usage, session/JWT, PIN & biometric |
| 💳 **Wallet / Payments** | IDOR/BOLA (backend), cryptography, insecure storage |
| 🗄️ **Vault** (notes & secrets) | insecure storage, weak crypto |
| 💬 **Chat / Messages** | input injection, local SQLi, WebView XSS |
| 📰 **Offers / News** (WebView) | JS-bridge abuse, deep links, insecure Custom Tabs |
| 👤 **Profile** | mass assignment, privacy / PII leakage |
| ⚙️ **Settings** | exported components, backup exfiltration, screenshot exposure |

The modules are intentionally *believable, not fully functional* — just enough product to make
each vulnerability feel real. Challenges are reached both through the module screens and through
the Challenges hub (grouped by OWASP category with difficulty tiers).

---

## 🎯 Vulnerability coverage (OWASP Mobile Top 10 · 2024)

| # | Category | Status |
|---|----------|--------|
| M1 | Improper Credential Usage | 🚧 Planned |
| M2 | Inadequate Supply Chain Security | 🚧 Planned |
| M3 | Insecure Authentication / Authorization | 🚧 Planned |
| M4 | Insufficient Input/Output Validation | 🚧 Planned |
| M5 | Insecure Communication | 🚧 Planned |
| M6 | Inadequate Privacy Controls | 🚧 Planned |
| M7 | Insufficient Binary Protections | 🚧 Planned |
| M8 | Security Misconfiguration | 🚧 Planned |
| M9 | Insecure Data Storage | 🏗️ In progress |
| M10 | Insufficient Cryptography | 🚧 Planned |

See the full challenge catalog and the level design in [**docs/ARCHITECTURE.md**](docs/ARCHITECTURE.md).

---

## 🚀 Getting started

### The app

```bash
git clone https://github.com/0xsl4m/DroidSiege.git
cd DroidSiege
# Open in Android Studio (Ladybug or newer) and run on an emulator, or:
./gradlew :app:assembleDebug
```

### The lab backend (for server-side challenges)

DroidSiege ships a small Ktor backend (`:backend`) hosting the server-side
vulnerability families (IDOR/BOLA, broken authentication, mass assignment —
four escalation tiers each). It is a **local lab only**: never deploy it
anywhere public, and never point it at data you care about.

```bash
# from the repository root (Dockerfile + docker-compose.yml live here)
docker compose up --build
# API listens on http://localhost:8080 — health: GET /health
```

No Docker? `./gradlew :backend:installDist` and run
`backend/build/install/backend/bin/backend`.

Point the app at your host with the in-app **Settings → Backend URL** field
(default `http://10.0.2.2:8080` for the Android emulator). The backend's
`SECURE_MODE` environment variable mirrors the app's Secure/Insecure toggle:
`off` (default) serves the vulnerable behaviors, `on` enforces the hardened
variants of every family — flip it to verify the fixes:

```bash
SECURE_MODE=on docker compose up --build
```

---

## 📚 Learning path

1. Pick a category from the home screen.
2. Start at **L1** and read the challenge brief.
3. Exploit it, capture the flag, submit it.
4. Flip the **Secure/Insecure** toggle and read *why* the fix works.
5. Climb to **L4**. Some Insane-tier challenges require **chaining** multiple bugs.

Stuck? Each challenge has tiered **hints** (they cost points). Full write-ups live in
[`docs/solutions/`](docs/solutions/) — try not to peek. 😉

---

## 🗺️ Roadmap

- [x] Phase 0 — Foundation, docs, CI
- [ ] Phase 1 — CTF engine, scoreboard, navigation shell
- [ ] Phase 2 — Core categories (Storage, Crypto, Components, Network) L1–L2
- [ ] Phase 3 — Advanced tiers L3–L4 (pinning bypass, root/Frida detection, native/JNI, WebView bridges)
- [ ] Phase 4 — Insecure backend (IDOR/BOLA, broken auth)
- [ ] Phase 5 — Solutions guide, GitHub Pages docs, release APK

---

## 🤝 Contributing & credit

DroidSiege stands on the shoulders of projects like DIVA, InjuredAndroid, AndroGoat,
InsecureShop and OVAA. It aims to add the one thing they don't have: **structured,
level-based progression** for every vulnerability class.

Contributions welcome — see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for how challenges
are structured before opening a PR.

## 📄 License

[MIT](LICENSE) — for education. Read the [DISCLAIMER](DISCLAIMER.md).
