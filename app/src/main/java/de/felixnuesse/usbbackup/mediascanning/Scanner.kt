package de.felixnuesse.usbbackup.mediascanning

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import de.felixnuesse.usbbackup.worker.BackupWorker

class Scanner(private var mContext: Context) {

    private val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    private val mPreMountVolumes = mStorageManager.storageVolumes


    fun tryFindingNewVolume(delay: Long, iterations: Int) {
        tryFindingNewVolume(delay, mStorageManager, mPreMountVolumes, iterations)
    }

    fun tryFindingNewVolume(delay: Long, storageManager: StorageManager, preMountVolumes: List<StorageVolume>, iterationsLeft: Int) {

        // todo: handle notification
        Handler(Looper.getMainLooper()).postDelayed({

            val newDrive = getNewDrive()
            if(newDrive != null) {
                BackupWorker.now(mContext, newDrive.uuid.toString())
            } else {
                if(iterationsLeft > 0) {
                    tryFindingNewVolume(delay, storageManager, preMountVolumes, iterationsLeft - 1)
                }
            }
        }, delay)

    }

    fun getNewDrive(): StorageVolume? {
        // this requires timing, and a lot of assumptions. MEDIA_MOUNT would truly be better
        return mStorageManager.storageVolumes.filter { it !in mPreMountVolumes }.toList().firstOrNull()
    }
}