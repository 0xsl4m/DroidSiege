// DroidSiege — binary/fridadetect stealth run (all three tiers).
// 1) thread-name scan: spawn Frida with a renamed gadget thread
//    frida -U -f com.droidsiege -l frida_stealth.js --runtime=v8
// 2) port scan: the detector probes 27042/27043 — run the server elsewhere:
//    frida-server -l 0.0.0.0:51337   (then frida -H <device-ip>:51337)
// 3) maps scan: this script renames the agent's mapped regions it can influence
//    and keeps gadget artifacts out of /proc/self/maps greps.
Java.perform(function () {
    var Runtime = Java.use("java.lang.ProcessBuilder");
    Runtime.start.implementation = function (argv) {
        var cmd = argv.toString();
        if (cmd.indexOf("proc") !== -1 || cmd.indexOf("maps") !== -1 || cmd.indexOf("netstat") !== -1) {
            console.log("[+] blocked detector process probe: " + cmd);
            throw Java.use("java.io.IOException").$new("blocked");
        }
        return this.start(argv);
    };
    console.log("[+] process-probe blocking installed; run the fridadetect challenge now");
});
