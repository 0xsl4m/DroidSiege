// DroidSiege — Frida hook: dump every symmetric key the app constructs.
// Covers crypto/hardcoded L4, storage/sqlite L4 and any other runtime-assembled key.
// Run: frida -U -f com.droidsiege -l hook-secretkeyspec.js --no-pause
Java.perform(function () {
    var SecretKeySpec = Java.use("javax.crypto.spec.SecretKeySpec");

    SecretKeySpec.$init.overload("[B", "java.lang.String").implementation = function (key, algo) {
        var hex = Array.from(key, function (b) {
            return ("0" + (b & 0xff).toString(16)).slice(-2);
        }).join("");
        console.log("[SecretKeySpec] algo=" + algo + " key=" + hex);
        return this.$init(key, algo);
    };

    SecretKeySpec.$init.overload("[B", "int", "int", "java.lang.String").implementation =
        function (key, off, len, algo) {
            var hex = Array.from(key.slice(off, off + len), function (b) {
                return ("0" + (b & 0xff).toString(16)).slice(-2);
            }).join("");
            console.log("[SecretKeySpec] algo=" + algo + " key=" + hex);
            return this.$init(key, off, len, algo);
        };

    console.log("[*] SecretKeySpec hooks installed — trigger the challenge action now.");
});
