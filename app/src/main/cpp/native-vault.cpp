#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

// DroidSiege native vault — deliberately vulnerable JNI surface (M7/M4 phases).
// The checks are intentionally simple so the tiers are bypassable with basic
// tooling; DO NOT model production native code on this file.

static const char *MAGIC = "opensesame";
static const char *POOL = "record=user-data;secret=DS{advanced_native_L2_09d5c8}";

static jsize poolLen() {
    return static_cast<jsize>(strlen(POOL));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_droidsiege_challenges_advanced_NativeVault_nativeCheck(
        JNIEnv *env, jobject, jbyteArray input) {
    jsize len = env->GetArrayLength(input);
    jbyte *bytes = env->GetByteArrayElements(input, nullptr);
    jboolean ok = (len == static_cast<jsize>(strlen(MAGIC)) &&
                   memcmp(bytes, MAGIC, static_cast<size_t>(len)) == 0);
    env->ReleaseByteArrayElements(input, bytes, JNI_ABORT);
    return ok;
}

// L2 — parse routine that trusts the caller-declared length: the copy walks past
// the user record into the pool's secret region (clamped to the pool end to avoid
// a segfault in the demo).
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_droidsiege_challenges_advanced_NativeVault_nativeParse(
        JNIEnv *env, jobject, jbyteArray input, jint declaredLen) {
    jsize inLen = env->GetArrayLength(input);
    jsize copyLen = declaredLen;
    if (copyLen > poolLen()) copyLen = poolLen();
    jbyteArray out = env->NewByteArray(copyLen);
    jbyte *tmp = static_cast<jbyte *>(malloc(static_cast<size_t>(copyLen > 0 ? copyLen : 1)));
    jbyte *in = inLen > 0 ? env->GetByteArrayElements(input, nullptr) : nullptr;
    for (jint i = 0; i < copyLen; ++i) {
        if (i < inLen) {
            tmp[i] = in[i];
        } else if (i < static_cast<jint>(strlen(POOL))) {
            tmp[i] = static_cast<jbyte>(POOL[i]);
        } else {
            tmp[i] = '?';
        }
    }
    if (in) env->ReleaseByteArrayElements(input, in, JNI_ABORT);
    env->SetByteArrayRegion(out, 0, copyLen, tmp);
    free(tmp);
    return out;
}

// L3 — native logging sink; the vulnerable variant uses the user string as the
// format (kept here for the write-up; the shipped call is hardened with "%s").
extern "C" JNIEXPORT void JNICALL
Java_com_droidsiege_challenges_advanced_NativeVault_nativeLog(
        JNIEnv *env, jobject, jstring fmt) {
    const char *f = env->GetStringUTFChars(fmt, nullptr);
    __android_log_print(ANDROID_LOG_INFO, "SiegeNative", "%s", f);
    env->ReleaseStringUTFChars(fmt, f);
}

// L4 — hidden function reachable only by redirecting a controlled overflow.
extern "C" JNIEXPORT jstring JNICALL
Java_com_droidsiege_challenges_advanced_NativeVault_hiddenPrintFlag(JNIEnv *env, jobject) {
    return env->NewStringUTF("DS{advanced_native_L4_3b96f2}");
}
