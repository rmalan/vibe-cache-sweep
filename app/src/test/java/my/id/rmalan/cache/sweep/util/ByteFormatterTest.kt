package my.id.rmalan.cache.sweep.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun format_bytes() {
        assertEquals("0 B", ByteFormatter.format(0L))
        assertEquals("500 B", ByteFormatter.format(500L))
        assertEquals("0 B", ByteFormatter.format(-10L))
    }

    @Test
    fun format_kilobytes() {
        assertEquals("1.0 KB", ByteFormatter.format(1024L))
        assertEquals("1.5 KB", ByteFormatter.format(1536L))
    }

    @Test
    fun format_megabytes() {
        assertEquals("1.0 MB", ByteFormatter.format(1024L * 1024L))
        assertEquals("500.0 MB", ByteFormatter.format(500L * 1024L * 1024L))
    }

    @Test
    fun format_gigabytes() {
        assertEquals("1.00 GB", ByteFormatter.format(1024L * 1024L * 1024L))
        assertEquals("7.24 GB", ByteFormatter.format((7.24 * 1024 * 1024 * 1024).toLong()))
    }
}
