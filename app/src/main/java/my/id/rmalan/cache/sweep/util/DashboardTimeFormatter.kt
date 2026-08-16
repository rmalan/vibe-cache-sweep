package my.id.rmalan.cache.sweep.util

import java.util.Locale

object DashboardTimeFormatter {
    fun formatLastScanned(lastScanTimeMillis: Long, currentTimeMillis: Long = System.currentTimeMillis()): String {
        if (lastScanTimeMillis <= 0L) return "Not scanned yet"
        val diffMs = maxOf(0L, currentTimeMillis - lastScanTimeMillis)
        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes == 1L -> "1 minute ago"
            minutes < 60 -> "$minutes minutes ago"
            hours == 1L -> "1 hour ago"
            hours < 24 -> "$hours hours ago"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    fun formatDuration(durationMillis: Long): String {
        if (durationMillis <= 0L) return "< 1s"
        return if (durationMillis < 1000L) {
            "${durationMillis}ms"
        } else {
            String.format(Locale.US, "%.1fs", durationMillis / 1000.0)
        }
    }
}
