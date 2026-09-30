// DroidSiege — binary/rootdetect Frida bypass (all three tiers).
// Usage: frida -U -f com.droidsiege -l root_bypass.js --no-pause
// The native tier hooks the JNI bridge; the Java tiers hook the platform calls.
Java.perform(function () {
    // L1 — su binary existence probes
    var File = Java.use("java.io.File");
    File.exists.implementation = function () {
        var p = this.getAbsolutePath();
        if (p.indexOf("su") !== -1 || p.indexOf("Superuser") !== -1 || p.indexOf("xbin") !== -1) {
            console.log("[+] File.exists(" + p + ") -> false");
            return false;
        }
        return this.exists();
    };

    // L2 — build fingerprint
    var Build = Java.use("android.os.Build");
    console.log("[*] TAGS currently: " + Build.TAGS.value);
    // verify by reading; the detector compares against test-keys:
    Java.use("com.droidsiege.challenges.binary.BinaryFamily");
    // (the family reads Build.TAGS — force release-keys)
    Build.TAGS.value = "release-keys";
    console.log("[+] TAGs forced to release-keys");
});

// L3 — native verdict: hook the JNI bridge result
Interceptor.attach(Module.findExportByName("libdroidsiege-vault.so", "Java_com_droidsiege_challenges_advanced_NativeVault_nativeCheck"), {
    onLeave: function (retval) {
        console.log("[+] nativeCheck ->", retval.toInt32(), "(forcing clean)");
        retval.replace(ptr(0)); // the 'clean' verdict the detector expects
    }
});
