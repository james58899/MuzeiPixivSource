package one.oktw.muzeipixivsource.util

import okhttp3.ConnectionSpec
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.internal.platform.Platform
import one.oktw.muzeipixivsource.hack.DisableSNISSLSocketFactory
import one.oktw.muzeipixivsource.hack.ParallelDns
import org.conscrypt.Conscrypt
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.Security

object HttpUtils {
    init {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    private val ipv6Ready = NetworkInterface.getNetworkInterfaces().toList()
        .any { it.interfaceAddresses.any { i -> i.address.run { this is Inet6Address && !isLoopbackAddress && !isLinkLocalAddress && !isSiteLocalAddress } } }
    private val cloudFlareDoH = DnsOverHttps.Builder()
        .client(OkHttpClient())
        .url("https://muzeipixivsource.cloudflare-dns.com/dns-query".toHttpUrl())
        .post(true)
        .includeIPv6(ipv6Ready)
        .bootstrapDnsHosts(listOf("104.16.248.249", "104.16.249.249").map(InetAddress::getByName))
        .build()
    private val googleDoH = DnsOverHttps.Builder()
        .client(OkHttpClient())
        .url("https://dns.google/dns-query".toHttpUrl())
        .post(true)
        .includeIPv6(ipv6Ready)
        .bootstrapDnsHosts(listOf("8.8.8.8", "8.8.4.4").map(InetAddress::getByName))
        .build()
    private val HEDoH = DnsOverHttps.Builder()
        .client(OkHttpClient())
        .url("https://ordns.he.net/".toHttpUrl())
        .post(true)
        .includeIPv6(ipv6Ready)
        .bootstrapDnsHosts(InetAddress.getByName("74.82.42.42"))
        .build()
    private val dns = ParallelDns(cloudFlareDoH, HEDoH, googleDoH, Dns.SYSTEM)

    val httpClient = OkHttpClient().newBuilder()
        .retryOnConnectionFailure(true)
        .connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
        .dns(dns)
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = 16 })
        .build()
    val directHttpClient = OkHttpClient().newBuilder()
        .retryOnConnectionFailure(true)
        .connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
        .sslSocketFactory(DisableSNISSLSocketFactory(), Platform.get().platformTrustManager())
        .dns(dns)
        .addInterceptor {
            it.proceed(it.request().newBuilder().url(it.request().url.newBuilder().host("pixiv.net").build()).header("Host", it.request().url.host).build())
        }
        .build()
}
