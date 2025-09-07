package de.felixnuesse.usbbackup.utils

import android.icu.text.RelativeDateTimeFormatter.*

class DateFormatter {

    companion object {
        fun relative(days: Long): String {
            val formatter = getInstance()
            return when(days) {
                0L -> formatter.format(Direction.THIS, AbsoluteUnit.DAY)
                -1L -> formatter.format(Direction.LAST, AbsoluteUnit.DAY)
                else -> formatter.format((-days).toDouble(), Direction.LAST, RelativeUnit.DAYS)
            }
        }
    }
}