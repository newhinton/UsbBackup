package de.felixnuesse.usbbackup.mediascanning

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume

class Scanner(mContext: Context) {

    private val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    private val mPreMountVolumes = mStorageManager.storageVolumes

    fun getNewDrive(): StorageVolume? {
        // this requires timing, and a lot of assumptions. MEDIA_MOUNT would truly be better
        return mStorageManager.storageVolumes.filter { it !in mPreMountVolumes }.toList().firstOrNull()
    }

    fun isVolumeConnected(targetVolume: String): Boolean {
        return mStorageManager.storageVolumes
            .map { it.uuid }
            .filter { it.equals(targetVolume) }
            .toList()
            .isNotEmpty()
    }
}