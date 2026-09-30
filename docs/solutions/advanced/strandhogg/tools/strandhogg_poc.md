# StrandHogg PoC recipe (advanced/strandhogg)

The challenge's insecure build declares the recovery activity with default
`taskAffinity` and `allowTaskReparenting="true"`.

**In-app simulation:** press *Open recovery screen* in insecure mode — the challenge
simulates the attacker task reparenting and overlaying the screen, harvesting the
recovery entry (the flag). Secure mode refuses: `taskAffinity=""` + no reparenting.

**Real-world PoC from a second app:**

1. Attacker activity declares the same `taskAffinity` as the victim and calls
   `startActivity` for the victim's recovery activity with
   `Intent.FLAG_ACTIVITY_NEW_TASK`.
2. The victim task reparents into the attacker's task (allowed because of the
   manifest flags), letting the attacker's activity sit above it in the same task.
3. The attacker draws a full-screen overlay (`TYPE_APPLICATION_OVERLAY`) asking for
   the "account recovery code".
4. Verify with: `adb shell dumpsys activity activities | grep -B2 -A2 affinity`.

**Fix (verified in secure mode):** `android:taskAffinity=""`,
`android:allowTaskReparenting="false"`, explicit `launchMode`, and (API 29+)
`setTaskDescription` + `setRecentsScreenshotEnabled` hygiene.
