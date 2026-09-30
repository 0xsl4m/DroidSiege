# storage / logs — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M9 Insecure Data Storage · **References:** MASVS-STORAGE-3 · MASTG-TEST-0x84

## How this family works

The analytics build logs sensitive values with `Log.i`. Anything the app logs, the device owner and any attached `adb logcat` reader gets for free.

**Vulnerable code:** `challenges/storage/LogsFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Balance Refresh

**Vulnerable behavior:** The recovery code is logged verbatim at INFO.

**Exploit:**
1. Start `adb logcat -s SiegeLogs` and press the track action (insecure mode).
2. The flag appears in the log line.

**Flag:** `DS{storage_logs_L1_d81f6c}`

**Fix:** Strip secret logging in release; use a logging facade that redacts by default.

## L2 🟡 — Handled Failure

**Vulnerable behavior:** The value is 'hashed' with a printable transform before logging.

**Exploit:**
1. Capture the logged blob; apply the inverse transform from the bundled tool.
2. The transform is bijective — it hides nothing.

**Flag:** `DS{storage_logs_L2_e54c81}`

**Fix:** Log only non-sensitive identifiers; drop the payload entirely.

## L3 🟠 — Debug Verbose

**Vulnerable behavior:** The log line prints the payload inside a JSON structure with a 'masked' field that isn't masked.

**Exploit:**
1. Capture the JSON log; read the field that claims to be masked.
2. The mask function only replaces a middle slice — the flag survives at the edges.

**Flag:** `DS{storage_logs_L3_27f0b8}`

**Fix:** Redact by allowlist: log structs where secret fields simply do not exist.

## L4 🔴 — Reporter Breadcrumbs

**Vulnerable behavior:** Release builds keep a debug log file that Logcat rotation dumps to disk.

**Exploit:**
1. Trigger several track actions, then read the rotated file via run-as.
2. Every 'temporary' debug line is persisted — including the flag.

**Flag:** `DS{storage_logs_L4_6a13d9}`

**Fix:** No file-backed debug logging in release builds; nothing sensitive in any sink.

## Tooling

- [`tools/harvest_logs.sh`](tools/harvest_logs.sh) — harvests each tier's logcat trail and decodes the transforms.

## Vulnerable vs hardened

```kotlin
// vulnerable
Log.i("SiegeLogs", "track: $recovery")

// hardened
// release builds log identifiers only — never payloads
```
