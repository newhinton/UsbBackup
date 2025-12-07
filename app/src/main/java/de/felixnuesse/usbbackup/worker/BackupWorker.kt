package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.crypto.Crypto
import de.felixnuesse.usbbackup.R
import de.felixnuesse.usbbackup.StorageUtils
import de.felixnuesse.usbbackup.UriUtils
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import de.felixnuesse.usbbackup.fs.FsUtils
import de.felixnuesse.usbbackup.fs.ZipUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.zip.ZipOutputStream


class BackupWorker(private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams), StateProvider {

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
        }
    }

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

        Log.e("WORKER", "Scan tasks... ${tasks.size}")


        tasks.forEach {
            if(BackupProcessor(mContext, this).process(it)) {
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

}