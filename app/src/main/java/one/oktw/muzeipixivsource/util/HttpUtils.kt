package one.oktw.muzeipixivsource.util

import android.net.LinkProperties
import android.os.Build
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.internal.platform.Platform
import one.oktw.muzeipixivsource.hack.DisableSNISSLSocketFactory
import one.oktw.muzeipixivsource.hack.FallbackDns
import org.conscrypt.Conscrypt
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.Security

object HttpUtils {
    init {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    private val bootstrapDNS = arrayListOf<InetAddress>().apply {
        addAll(listOf("104.16.248.249", "104.16.249.249").map(InetAddress::getByName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) addAll(LinkProperties().dnsServers)
    }
    private val dns = FallbackDns(DnsOverHttps.Builder()
        .client(OkHttpClient())
        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
        .post(true)
        .includeIPv6(NetworkInterface.getNetworkInterfaces().toList().any { it.interfaceAddresses.any { it.address.run { this is Inet6Address && !isLoopbackAddress && !isLinkLocalAddress && !isSiteLocalAddress } } })
        .bootstrapDnsHosts(bootstrapDNS)
        .build(), Dns.SYSTEM)

    val httpClient = OkHttpClient().newBuilder()
        .retryOnConnectionFailure(true)
        .connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
        .dns(dns)
        .build()
    val apiHttpClient = OkHttpClient().newBuilder()
        .retryOnConnectionFailure(true)
        .connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
        .sslSocketFactory(DisableSNISSLSocketFactory(), Platform.get().platformTrustManager())
        .dns(dns)
        .build()
}
