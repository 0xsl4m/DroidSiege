# Custom Tabs URL capture (advanced/customtabs)

The app appends the recovery code to the Custom Tab URL: `https://offers.siegeapp.dev/redeem?code=<flag>`.

**In-app simulation:** press *Open offers in a tab* — insecure mode launches the URL
with the code in it (the console confirms what a browser history entry would hold);
secure mode redeems server-side with nothing in the URL.

**Where the leak is observable in the real world:**

1. Browser history: `adb shell content query --uri content://com.android.chrome.browser/history` (or the history provider of whichever browser handles the tab).
2. logcat: Custom Tab launches log the intent — `adb logcat | grep offers.siegeapp.dev`.
3. A redirect chain you control: point the redeem host at a local server
   (`python3 -m http.server 8080` + /etc/hosts on a rooted device) and read the request line.

**Fix:** redeem via a POST from the app's own screen; the tab URL carries an opaque,
single-use reference at most.
