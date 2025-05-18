package de.felixnuesse.usbbackup

import android.content.Context
import android.net.Uri
import android.os.storage.StorageManager
import android.os.storage.StorageVolume

class StorageUtils {

    companion object {

        fun get(context: Context, uri: Uri): StorageVolume? {
            return get(context, UriUtils.getStorageId(uri))
        }

        fun get(context: Context, uuid: String): StorageVolume? {
            var sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            return sm.storageVolumes.firstOrNull { it.uuid.toString() == uuid }
        }
    }


}