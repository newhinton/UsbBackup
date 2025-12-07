package de.felixnuesse.usbbackup.mediascanning

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import de.felixnuesse.usbbackup.worker.BackupWorker

class Scanner(mContext: Context) {

    private val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    private val mPreMountVolumes = mStorageManager.storageVolumes

    fun getNewDrive(): StorageVolume? {
        // this requires timing, and a lot of assumptions. MEDIA_MOUNT would truly be better
        return mStorageManager.storageVolumes.filter { it !in mPreMountVolumes }.toList().firstOrNull()
    }
}