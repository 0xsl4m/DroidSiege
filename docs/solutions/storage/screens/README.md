# storage / screens — all tiers

> **Spoilers!** This file solves every tier. Try the challenge first —
> hints cost points, and the flag input is on each challenge screen.

**OWASP Mobile Top 10:** M9 Insecure Data Storage · **References:** MASVS-PLATFORM-2 · MASTG-TEST-0x63

## How this family works

The wallet screen omits `FLAG_SECURE`, so its contents appear in recents thumbnails, task switcher snapshots and any screenshot the user (or malware) takes.

**Vulnerable code:** `challenges/storage/ScreensFamily.kt` — every screen ships both paths; the
Secure/Insecure toggle in the app switches between them.

## L1 🟢 — Recents Thumbnail

**Vulnerable behavior:** The secret screen sets no FLAG_SECURE — screenshots capture it.

**Exploit:**
1. Open the challenge (insecure mode), take a screenshot (`adb shell screencap`).
2. The flag is on the image.

**Flag:** `DS{crypto_ecbiv_L1_7a2e46}`

**Fix:** Set `FLAG_SECURE` on sensitive screens.

## L2 🟡 — Inventory Snapshot

**Vulnerable behavior:** The recents thumbnail keeps the last screen image on disk.

**Exploit:**
1. Open the screen, then list the task thumbnails via run-as / system recents.
2. The thumbnail shows the secret.

**Flag:** `DS{crypto_ecbiv_L2_d5f981}`

**Fix:** Clear the activity's content in `onPause` or set FLAG_SECURE so the snapshot is blanked.

## L3 🟠 — Delayed Obscure

**Vulnerable behavior:** A 'privacy blur' overlay only covers the view in layouts — not in the captured bitmap.

**Exploit:**
1. Screenshot while the blur is visible: the underlying text is in the bitmap.
2. Overlays are view-level; capture happens at the surface level.

**Flag:** `DS{crypto_ecbiv_L3_38c1b7}`

**Fix:** Surface-level protection (FLAG_SECURE), not view-level overlays.

## L4 🔴 — Partner Transition

**Vulnerable behavior:** The screenshot-'blocked' build still renders the secret into the share preview bitmap.

**Exploit:**
1. Trigger the share flow and pull the generated preview file.
2. The preview generator ignored the flag-secure path.

**Flag:** `DS{crypto_ecbiv_L4_82ea6f}`

**Fix:** Every rendering path of a secret screen must respect the same protection.

## Tooling

- [`tools/snap_windows.sh`](tools/snap_windows.sh) — captures the screen, thumbnails and task snapshots, then greps them.

## Vulnerable vs hardened

```kotlin
// vulnerable
// window flags never set

// hardened
window.setFlags(FLAG_SECURE, FLAG_SECURE)
```
