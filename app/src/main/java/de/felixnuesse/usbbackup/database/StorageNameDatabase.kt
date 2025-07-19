package de.felixnuesse.usbbackup.database

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class StorageNameDatabase (mContext: Context) {

    private val prefs: SharedPreferences = mContext.getSharedPreferences("usbbackup", Context.MODE_PRIVATE)

    companion object {
        private const val PREFIX = "STORAGE_ID_"
    }

    fun cacheName(id: String, name: String) {
        prefs.edit { putString(PREFIX+id, name) }
    }

    fun getName(id: String): String? {
        return prefs.getString(PREFIX+id, null)
    }
}