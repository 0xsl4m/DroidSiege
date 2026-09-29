#!/usr/bin/env bash
# DroidSiege — network family tooling pack
# The M5 families are backed by an in-app local endpoint (loopback), so the same
# exchanges a proxy would capture are shown on each challenge console. For a real
# proxy pass, point the emulator host at Burp (below) and re-run the fetches.
set -euo pipefail

echo "== adb reverse for a desktop Burp/proxy (optional) =="
echo "adb reverse tcp:8080 tcp:8080   # then use http://127.0.0.1:8080 in the app"

echo
echo "== capture flows from the challenges =="
echo "1) toggle 'proxy mode (attacker)' on the challenge screen"
echo "2) press the fetch action — the console prints the exchange"
echo "3) 'Show what the proxy captured' prints the in-transit view (flag included)"
echo "4) 'Read the server request log' shows the server-side view (urlleak family)"

echo
echo "== Frida pinning bypass (pinning L3-style misconfig is bypassable without Frida; =="
echo "== the robust-pin tier is the baseline to test your bypass against)          =="
cat <<'JS'
// frida -U -f com.droidsiege -l bypass_pinning.js --no-pause
Java.perform(function () {
  var CertificatePinner = Java.use("okhttp3.CertificatePinner");
  CertificatePinner.check$okhttp.overload(
    "java.lang.String", "java.util.List"
  ).implementation = function (hostname, peerCertificates) {
    console.log("[+] bypassing pin check for " + hostname);
  };
});
JS
