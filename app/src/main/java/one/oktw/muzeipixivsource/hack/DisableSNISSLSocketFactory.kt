package one.oktw.muzeipixivsource.hack

import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

class DisableSNISSLSocketFactory : SSLSocketFactory() {
    private val defaultFactory = getDefault() as SSLSocketFactory

    override fun getDefaultCipherSuites(): Array<String> = defaultFactory.defaultCipherSuites

    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        return defaultFactory.createSocket(socket, null, port, autoClose) // TODO check works
    }

    override fun createSocket(host: String, port: Int): Socket = defaultFactory.createSocket(host, port)

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        return defaultFactory.createSocket(host, port, localHost, localPort)
    }

    override fun createSocket(host: InetAddress, port: Int): Socket = defaultFactory.createSocket(host, port)

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        return defaultFactory.createSocket(address, port, localAddress, localPort)
    }

    override fun getSupportedCipherSuites(): Array<String> = defaultFactory.supportedCipherSuites
}
