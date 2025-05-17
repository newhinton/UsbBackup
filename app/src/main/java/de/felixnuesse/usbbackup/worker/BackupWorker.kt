package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.usbbackup.UriUtils
import de.felixnuesse.usbbackup.database.AppDatabase
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupWorker(private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {

   companion object {

       fun now(context: Context, storageId: String) {

           val data = Data.Builder()
           data.putString("storageId", storageId)

           val request = OneTimeWorkRequestBuilder<BackupWorker>()
           request.setInputData(data.build())
           WorkManager.getInstance(context).enqueue(request.build())
       }
   }

    override fun doWork(): Result {

        var storageId = inputData.getString("storageId")

        if(storageId == null) {
            return Result.failure()
        }


        val db = AppDatabase.Companion.getDatabase(mContext)
        var tasks = db.backupDao().getAll()

        Log.e("WORKER", "Scan tasks... ${tasks.size}")

        tasks.forEach {
            var fSid = UriUtils.getStorageId(it.targetUri.toUri())
            Log.e("WORKER", "Scan tasks... ${fSid}")
            if(storageId == fSid) {
                if(it.enabled){
                    Log.e("WORKER", "Current Task: $storageId, ${it.name}")

                    val target = DocumentFile.fromTreeUri(mContext, it.targetUri.toUri())?.createDirectory("NEW FOLDER ${System.currentTimeMillis()}")
                    val uri = it.sourceUri.toUri()
                    if(UriUtils.isFolder(mContext, uri)) {
                        val folder = DocumentFile.fromTreeUri(mContext, uri)

                        folder?.listFiles()?.forEach {
                            if(it.isDirectory) {
                                target?.createDirectory(it.name.toString())
                            }
                            if(it.isFile) {
                                // todo remove !!
                                var file = target?.createFile("", it.name.toString())!!

                                mContext.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                                    val inputStream = mContext.contentResolver.openInputStream(it.uri)?.use { inputStream ->
                                        outputStream.write(inputStream.readBytes())
                                    }
                                    outputStream.flush()
                                }

                            }
                        }



                    }

                    // todo
                }
            }
        }

        return Result.success()
    }

}