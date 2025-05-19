package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.usbbackup.UriUtils
import de.felixnuesse.usbbackup.crypto.AES
import de.felixnuesse.usbbackup.database.AppDatabase
import de.felixnuesse.usbbackup.database.BackupTask
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class BackupWorker(private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {

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
    }

    private var mNotifications = Notifications(mContext)

    override fun doWork(): Result {

        var storageId = inputData.getString("storageId")
        var taskId = inputData.getInt("taskId", -1)

        if(storageId == null && taskId == -1) {
            return Result.failure()
        }


        val db = AppDatabase.Companion.getDatabase(mContext)


        var tasks = arrayListOf<BackupTask>()

        if(storageId != null) {
            tasks.addAll(db.backupDao().getAll().filter { UriUtils.getStorageId(it.targetUri.toUri()) == storageId })
        }

        if(taskId != -1) {
            tasks.add(db.backupDao().get(taskId))
        }


        Log.e("WORKER", "Scan tasks... ${tasks.size}")

        tasks.forEach {
            if(it.enabled){
                // todo: wrap and catch exceptions

                mNotifications.showNotification("Backup in Progress", "Task: ${it.name}")
                Log.e("WORKER", "Current Task: ${storageId?: taskId}, ${it.name}")

                try {
                    val sourceUri = it.sourceUri.toUri()
                    var unencryptedCacheFile = File(mContext.cacheDir, "cache.zip")
                    ZipOutputStream(FileOutputStream(unencryptedCacheFile)).use { out ->
                        if(UriUtils.isFolder(mContext, sourceUri)) {
                            val folder = DocumentFile.fromTreeUri(mContext, sourceUri)
                            addToZipRecursive(folder!!, out, "")
                            out.close()
                        }
                    }


                    var target = DocumentFile.fromTreeUri(mContext, it.targetUri.toUri())
                    var file = target?.createFile("", getName(it))!!


                    if(!it.containerPW.isNullOrBlank()) {
                        Log.e("Tag", "Encrypting...")
                        AES().aesEncrypt(unencryptedCacheFile, mContext.contentResolver.openOutputStream(file.uri)!!, it.containerPW!!.toCharArray())
                    } else {
                        Log.e("Tag", "Storing...")
                        mContext.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                            outputStream.write(unencryptedCacheFile.readBytes())
                            outputStream.flush()
                        }
                    }

                    unencryptedCacheFile.delete()

                    Log.e("Tag", "Done!")
                    mNotifications.showNotification("Backup Done!", "Task: ${it.name}")
                } catch (e: Exception) {
                    mNotifications.showNotification("Backup Failure!", "Task: ${it.name}, error: ${e.message}")
                }


                //mContext.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                //    val inputStream = mContext.contentResolver.openInputStream(it.uri)?.use { inputStream ->
                //        outputStream.write(inputStream.readBytes())
                //    }
                //    outputStream.flush()
                //}

                // todo

            }
        }

        return Result.success()
    }

    private fun addToZipRecursive(current: DocumentFile, zip: ZipOutputStream, path: String) {
        current.listFiles().forEach {
            Log.e("Tag", "Processing: $path${it.name}")
            if(it.isDirectory) {
                val entry = ZipEntry("$path${it.name}/")
                zip.putNextEntry(entry)
                zip.closeEntry()
                addToZipRecursive(it, zip, "$path${it.name}/")
            }
            if(it.isFile) {
                val entry = ZipEntry("$path${it.name}")
                zip.putNextEntry(entry)
                writeToZip(zip, it.uri)
                zip.closeEntry()
            }
        }

    }

    private fun writeToZip(zip: ZipOutputStream, uri: Uri) {
        var stream = mContext.contentResolver.openInputStream(uri)
        val buffer = ByteArray(16384)

        var bytesRead: Int
        while (stream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
            zip.write(buffer, 0, bytesRead)
        }
        stream?.close()
    }


    private fun getName(task: BackupTask): String {
        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("yyyy-MM-dd-hh-mm")

        val prefix = if(!task.containerPW.isNullOrBlank()) "encrypted_"  else ""

        return "$prefix${task.name}_" + format.format(date) + ".zip"
    }
}