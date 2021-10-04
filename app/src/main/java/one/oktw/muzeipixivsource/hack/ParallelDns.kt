package one.oktw.muzeipixivsource.hack

import kotlinx.coroutines.*
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

class ParallelDns(private vararg val dns: Dns) : Dns, CoroutineScope by CoroutineScope(SupervisorJob()) {
    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        val jobs = ArrayList<Deferred<List<InetAddress>>>(5)
        var exception: UnknownHostException? = null

        // Parallel request
        dns.forEach { jobs += async(Dispatchers.IO) { it.lookup(hostname) } }

        // Collect DNS results
        val results = ArrayList<InetAddress>()
        runBlocking {
            jobs.forEach {
                try {
                    results += it.await()
                } catch (e: UnknownHostException) {
                    // Concat exception cause
                    var cause: Throwable = e
                    while (cause.cause != null) cause = cause.cause!!
                    cause.initCause(exception)

                    exception = e
                }
            }
        }

        if (results.isNotEmpty()) return results else throw UnknownHostException(hostname).initCause(exception)
    }
}
