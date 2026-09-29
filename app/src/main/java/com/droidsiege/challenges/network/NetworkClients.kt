package com.droidsiege.challenges.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import java.net.InetAddress

/**
 * Client factory for the M5 tiers. Every client resolves all hostnames to the loopback
 * where the mock backend (and the MITM endpoint) listen; the [viaMitm] flag decides
 * whether "trusted.siege.local" lands on the honest TLS endpoint or on the attacker's.
 */
object NetworkClients {
    private fun dnsLoopback(): OkHttpClient.Builder =
        OkHttpClient.Builder().dns(
            object : okhttp3.Dns {
                override fun lookup(hostname: String): List<InetAddress> = listOf(InetAddress.getByName("127.0.0.1"))
            },
        )

    private fun port(viaMitm: Boolean): Int = if (viaMitm) MockBackend.mitmPort else MockBackend.tlsPort

    /** Trusts the "user CA" the sim installs: the honest cert, or the MITM's when routing via mitm. */
    fun userCaClient(viaMitm: Boolean): OkHttpClient {
        val trusted =
            if (viaMitm) {
                HandshakeCertificates.Builder()
                    .addTrustedCertificate(MockBackend.mitmCertificate.certificate)
                    .build()
            } else {
                HandshakeCertificates.Builder()
                    .addTrustedCertificate(MockBackend.localCertificate().certificate)
                    .build()
            }
        return dnsLoopback()
            .sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
            .build()
    }

    /** User-CA trust PLUS a certificate pin — [correctPin] decides whether it protects. */
    fun pinnedClient(
        viaMitm: Boolean,
        correctPin: Boolean,
    ): OkHttpClient {
        val trusted =
            if (viaMitm) {
                HandshakeCertificates.Builder()
                    .addTrustedCertificate(MockBackend.mitmCertificate.certificate)
                    .build()
            } else {
                HandshakeCertificates.Builder()
                    .addTrustedCertificate(MockBackend.localCertificate().certificate)
                    .build()
            }
        val pinPattern =
            if (correctPin) MockBackend.VIRTUAL_HOST else "other.siege.local"
        val pinner =
            CertificatePinner
                .Builder()
                .add(
                    pinPattern,
                    CertificatePinner.pin(MockBackend.localCertificate().certificate),
                )
                .build()
        return dnsLoopback()
            .sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
            .certificatePinner(pinner)
            .build()
    }

    /** Custom X509TrustManager that only accepts the app's own cert (the "native" pin). */
    fun customPinnedClient(): OkHttpClient {
        val local = MockBackend.localCertificate().certificate
        val trustManager =
            object : X509TrustManagerCompat {
                override fun checkClientTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String?,
                ) = Unit

                override fun checkServerTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String?,
                ) {
                    val peer = chain?.firstOrNull() ?: throw java.security.cert.CertificateException("no peer")
                    if (!peer.subjectX500Principal.name.contains(MockBackend.VIRTUAL_HOST)) {
                        throw java.security.cert.CertificateException("unexpected peer")
                    }
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf(local)
            }
        val context = javax.net.ssl.SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<javax.net.ssl.TrustManager>(trustManager), java.security.SecureRandom())
        }
        return dnsLoopback()
            .sslSocketFactory(context.socketFactory, trustManager)
            .build()
    }

    /** Plain hostname-verifier-true client trusting the presented cert (trustall L2). */
    fun hostnameBypassClient(viaMitm: Boolean): OkHttpClient {
        val base = userCaClient(viaMitm)
        return base
            .newBuilder()
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    /** Trust-all X509 trust manager client (trustall L1/L3/L4). */
    fun trustAllClient(): OkHttpClient {
        val trustManager = MockBackend.trustAllManager()
        val context = javax.net.ssl.SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<javax.net.ssl.TrustManager>(trustManager), java.security.SecureRandom())
        }
        return dnsLoopback()
            .sslSocketFactory(context.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    fun httpsUrl(
        viaMitm: Boolean,
        path: String,
    ): String = "https://trusted.siege.local:${port(viaMitm)}$path"
}
