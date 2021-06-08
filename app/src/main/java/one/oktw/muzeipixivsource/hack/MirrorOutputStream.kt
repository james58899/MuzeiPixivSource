package one.oktw.muzeipixivsource.hack

import okhttp3.internal.closeQuietly
import java.io.OutputStream

class MirrorOutputStream(private val out: OutputStream, private val mirror: OutputStream) : OutputStream() {
    override fun write(b: Int) {
        out.write(b)
        try {
            mirror.write(b)
        } catch (e: Exception) {
        }
    }

    override fun write(b: ByteArray) {
        out.write(b)
        try {
            mirror.write(b)
        } catch (e: Exception) {
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        try {
            mirror.write(b, off, len)
        } catch (e: Exception) {
        }
    }

    override fun flush() {
        out.flush()
        try {
            mirror.flush()
        } catch (e: Exception) {
        }
    }

    override fun close() {
        out.close()
        try {
            mirror.close()
        } catch (e: Exception) {
        }
    }

    fun closeQuietly() {
        out.closeQuietly()
        mirror.closeQuietly()
    }
}
