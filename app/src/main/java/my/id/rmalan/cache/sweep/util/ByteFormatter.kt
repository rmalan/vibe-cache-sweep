package my.id.rmalan.cache.sweep.util

import java.util.Locale

object ByteFormatter {
    private const val KB = 1024L
    private const val MB = KB * 1024L
    private const val GB = MB * 1024L
    private const val TB = GB * 1024L

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        return when {
            bytes >= TB -> String.format(Locale.US, "%.2f TB", bytes.toDouble() / TB)
            bytes >= GB -> String.format(Locale.US, "%.2f GB", bytes.toDouble() / GB)
            bytes >= MB -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / MB)
            bytes >= KB -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / KB)
            else -> "$bytes B"
        }
    }
}
