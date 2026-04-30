package de.felixnuesse.usbbackup.utils

import android.icu.text.RelativeDateTimeFormatter.*
import java.time.Duration
import java.time.Instant

class DateFormatter {

    companion object {

        fun daysDifference(timestamp: Long): Long {
            val now = Instant.now()
            val then = Instant.ofEpochMilli(timestamp)
            return Duration.between(now, then).toDays()
        }

        fun relative(timestamp: Long): String {
            val days = daysDifference(timestamp)
            return relativeDays(days)
        }

        fun relativeDays(days: Long): String {
            val formatter = getInstance()

            return if(days == 0L) {
                formatter.format(Direction.THIS, AbsoluteUnit.DAY)
            } else if(days == -1L) {
                formatter.format(Direction.LAST, AbsoluteUnit.DAY)
            } else if (days < 0) {
                formatter.format((-days).toDouble(), Direction.LAST, RelativeUnit.DAYS)
            } else {
                formatter.format((days).toDouble(), Direction.NEXT, RelativeUnit.DAYS)
            }
        }
    }
}