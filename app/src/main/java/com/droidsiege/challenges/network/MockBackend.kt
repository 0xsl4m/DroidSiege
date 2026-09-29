package com.droidsiege.challenges.network

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetAddress
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext

/**
 * Interim local backend for the M5 families: a pair of MockWebServer instances
 * (plain HTTP + self-signed TLS) bound to the emulator loopback, with every hostname
 * the clients resolve mapped to 127.0.0.1. Phase 6 replaces the backing with the real
 * Ktor backend; the challenge logic (clients, policies, pinning) stays identical.
 *
 * The MITM tiers' attacker is simulated in-process: while [mitmEnabled] is set the
 * dispatcher serves the attacker's substitution and records what a proxy would have
 * observed in transit.
 */
object MockBackend {
    /** Virtual host the TLS endpoint's certificate is issued for. */
    const val VIRTUAL_HOST = "trusted.siege.local"

    private val started = AtomicBoolean(false)
    private val plainServer = MockWebServer()
    private val tlsServer = MockWebServer()
    private val mitmServer = MockWebServer()

    private val observed = mutableListOf<String>()

    /** The proxy's substitution body while a MITM is running. */
    @Volatile
    var mitmBody: String? = null

    /** Proxy-mode toggle: routes client traffic through the attacker's endpoint. */
    @Volatile
    var mitmEnabled: Boolean = false

    @Volatile
    var plainPort: Int = 0
        private set

    @Volatile
    var tlsPort: Int = 0
        private set

    @Volatile
    var mitmPort: Int = 0
        private set

    val requestLog: List<String>
        get() = synchronized(observed) { observed.toList() }

    fun clearLog() = synchronized(observed) { observed.clear() }

    private fun log(line: String) = synchronized(observed) { observed += line }

    fun localCertificate(): HeldCertificate = localCertificate

    /** Burp-style MITM certificate: issued for the TARGET host, attacker-signed. */
    val mitmCertificate: HeldCertificate by lazy {
        HeldCertificate.Builder()
            .commonName(VIRTUAL_HOST)
            .addSubjectAlternativeName(VIRTUAL_HOST)
            .addSubjectAlternativeName("localhost")
            .build()
    }

    /** A wrong-host certificate for the hostname-verification tier. */
    fun wrongHostCertificate(): HeldCertificate = HeldCertificate.Builder().commonName("mitm.siege.local").build()

    private val localCertificate: HeldCertificate by lazy {
        HeldCertificate.Builder()
            .commonName(VIRTUAL_HOST)
            .addSubjectAlternativeName(VIRTUAL_HOST)
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
    }

    private val dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: "/"
                val response =
                    when {
                        path == "/clear/L1" -> "session recovery: DS{network_cleartext_L1_61d4b8}"
                        path == "/plain/sync" -> "sync payload: DS{network_cleartext_L2_c95e07}"
                        path == "/nsa/L3" -> "policy check: DS{network_cleartext_L3_2fa836}"
                        path == "/dsg/L4" -> "downgraded: DS{network_cleartext_L4_8b71d4}"
                        path == "/pin/L1" -> "pinned payload: DS{network_pinning_L1_e4c192}"
                        path == "/pin/L2" -> "pinned payload: DS{network_pinning_L2_570ad8}"
                        path == "/pin/L3" -> "pinned payload: DS{network_pinning_L3_b26f45}"
                        path == "/pin/L4" -> "pinned payload: DS{network_pinning_L4_93d7e0}"
                        path == "/trust/L1" -> "trusted payload: DS{network_trustall_L1_78b4c2}"
                        path == "/trust/L2" -> "trusted payload: DS{network_trustall_L2_0d96e5}"
                        path == "/trust/L3" -> "trusted payload: DS{network_trustall_L3_41f7a3}"
                        path == "/sdk/L4" -> "sdk payload: DS{network_trustall_L4_6c28d9}"
                        else -> "ok"
                    }
                log(
                    if (mitmEnabled) {
                        "PROXY $path -> 200 body: $response"
                    } else {
                        "SERVER $path -> 200"
                    },
                )
                mitmBody?.let {
                    log("PROXY substituted body: $it")
                    return MockResponse().setBody(it)
                }
                return MockResponse().setBody(response)
            }
        }

    @Synchronized
    fun ensureStarted() {
        if (!started.compareAndSet(false, true)) return
        plainServer.dispatcher = dispatcher
        plainServer.start(InetAddress.getByName("127.0.0.1"), 0)
        plainPort = plainServer.port

        val handshake =
            HandshakeCertificates.Builder()
                .heldCertificate(localCertificate)
                .build()
        tlsServer.useHttps(handshake.sslSocketFactory(), false)
        tlsServer.dispatcher = dispatcher
        tlsServer.start(InetAddress.getByName("127.0.0.1"), 0)
        tlsPort = tlsServer.port

        // the MITM endpoint: attacker certificate, forwards to the plain upstream and
        // records everything a proxy would observe
        val mitmHandshake =
            HandshakeCertificates.Builder()
                .heldCertificate(mitmCertificate)
                .build()
        mitmServer.useHttps(mitmHandshake.sslSocketFactory(), false)
        val upstream = OkHttpClient()
        mitmServer.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path ?: "/"
                    val upstreamCall =
                        upstream
                            .newCall(
                                okhttp3.Request.Builder()
                                    .url("http://127.0.0.1:" + plainPort + path)
                                    .build(),
                            )
                            .execute()
                    val body = upstreamCall.body?.string() ?: ""
                    log("PROXY captured " + path + " -> body: " + body)
                    mitmBody?.let {
                        log("PROXY substituted body: $it")
                        return MockResponse().setBody(it)
                    }
                    return MockResponse().setBody(body)
                }
            }
        mitmServer.start(InetAddress.getByName("127.0.0.1"), 0)
        mitmPort = mitmServer.port
    }

    private fun OkHttpClient.Builder.loopbackDns(): OkHttpClient.Builder =
        dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<InetAddress> = listOf(InetAddress.getByName("127.0.0.1"))
        })

    /** Plain-HTTP client; every hostname resolves to the loopback servers. */
    fun plainClient(): OkHttpClient = OkHttpClient.Builder().loopbackDns().build()

    /** TLS client that additionally trusts the local self-signed certificate. */
    fun trustingClient(): OkHttpClient {
        val certificates =
            HandshakeCertificates.Builder()
                .addTrustedCertificate(localCertificate.certificate)
                .build()
        return OkHttpClient
            .Builder()
            .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
            .loopbackDns()
            .build()
    }

    /** TLS client with the platform default trust store only (hardened baseline). */
    fun defaultTlsClient(): OkHttpClient = OkHttpClient.Builder().loopbackDns().build()

    /** The MITM's own certificate — a different identity an attacker presents. */
    fun mitmCertificates(): HandshakeCertificates =
        HandshakeCertificates.Builder()
            .heldCertificate(
                HeldCertificate.Builder().commonName("mitm.siege.local").build(),
            )
            .build()

    /** Trust-all X509TrustManager — the L1 vulnerable configuration. */
    fun trustAllManager(): X509TrustManagerCompat =
        object : X509TrustManagerCompat {
            override fun checkClientTrusted(
                chain: Array<out java.security.cert.X509Certificate>?,
                authType: String?,
            ) = Unit

            override fun checkServerTrusted(
                chain: Array<out java.security.cert.X509Certificate>?,
                authType: String?,
            ) = Unit

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }

    fun sslContextFor(trustManager: X509TrustManagerCompat): SSLContext =
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<javax.net.ssl.TrustManager>(trustManager), SecureRandom())
        }
}
