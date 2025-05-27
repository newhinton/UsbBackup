package de.felixnuesse.usbbackup

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class Prefs(mContext: Context) {

    private val prefs: SharedPreferences = mContext.getSharedPreferences("usbbackup", Context.MODE_PRIVATE)

    fun setString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }
}