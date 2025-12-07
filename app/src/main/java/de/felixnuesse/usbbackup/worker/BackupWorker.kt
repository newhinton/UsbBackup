package de.felixnuesse.usbbackup.worker

import android.annotation.SuppressLint
import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.usbbackup.UriUtils
import de.felixnuesse.usbbackup.broadcasts.MediaBroadcastReceiver
import de.felixnuesse.usbbackup.broadcasts.MediaBroadcastRecieverCallback
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import java.util.UUID


class BackupWorker(private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams), StateProvider, MediaBroadcastRecieverCallback {

    companion object {
        fun now(context: Context, storageId: String) {
            val data = Data.Builder()
            data.putString("storageId", storageId)

            val request = OneTimeWorkRequestBuilder<BackupWorker>()
            request.setInputData(data.build())
            WorkManager.getInstance(context).enqueue(request.build())
        }

        fun now(context: Context, taskId: Int) {
            val data = Data.Builder()
            data.putInt("taskId", taskId)

            val request = OneTimeWorkRequestBuilder<BackupWorker>()
            request.setInputData(data.build())
            WorkManager.getInstance(context).enqueue(request.build())
        }

        fun stop(context: Context, uuid: UUID) {
            Log.e("BackupWorker", "Stop Work: $uuid")
            WorkManager.getInstance(context).cancelWorkById(uuid)
            MediaBroadcastReceiver.clear(uuid)
        }
    }

    private var mBackupProcessor: BackupProcessor? = null

    override fun doWork(): Result {

        val storageId = inputData.getString("storageId")
        val taskId = inputData.getInt("taskId", -1)

        if(storageId == null && taskId == -1) {
            return Result.failure()
        }

        val backupTaskMiddleware = BackupTaskMiddleware.get(mContext)
        val tasks = arrayListOf<BackupTask>()

        if(storageId != null) {
            tasks.addAll(backupTaskMiddleware.getAll().filter { UriUtils.getStorageId(it.targetUri.toUri()) == storageId })
        }

        if(taskId != -1) {
            tasks.add(backupTaskMiddleware.get(taskId))
        }

        Log.e("BackupWorker", "Scan tasks... Found ${tasks.size}!")

        MediaBroadcastReceiver.registerCallback(this.id, this)
        tasks.forEach {
            mBackupProcessor = BackupProcessor(mContext, this)
            if(mBackupProcessor!!.process(it)) {
                backupTaskMiddleware.updateSuccessTimestamp(it.id!!)
            }
        }

        // always return success, because this task does not fail. Individual Parts might, but they notify you.
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        Log.e("BackupWorker", "Was stopped! ${this.id}")
    }

    override fun wasStopped(): Boolean {
        return this.isStopped
    }

    override fun workerId(): UUID {
        return this.id
    }

    @SuppressLint("RestrictedApi")
    override fun onDisconnected() {
        Log.e("BackupWorker", "Recieved Device Disconnected call! Handling...")
        if(mBackupProcessor?.shouldDeviceDisconnectEndTask() ?: false) {
            this.stop(STOP_REASON_CONSTRAINT_CONNECTIVITY)
        }
    }

    override fun onNewVolume() {
        // unused
    }

}