package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.usbbackup.UriUtils
import de.felixnuesse.usbbackup.database.AppDatabase
import java.io.ByteArrayOutputStream
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

                    var target = DocumentFile.fromTreeUri(mContext, it.targetUri.toUri())?.findFile("Backups")
                    if(target == null) {
                        target = DocumentFile.fromTreeUri(mContext, it.targetUri.toUri())?.createDirectory("Backups")
                    }

                    val sourceUri = it.sourceUri.toUri()

                    val baos = ByteArrayOutputStream()
                    val out = ZipOutputStream(baos)

                    if(UriUtils.isFolder(mContext, sourceUri)) {
                        val folder = DocumentFile.fromTreeUri(mContext, sourceUri)
                        addToZipRecursive(folder!!, out, "")
                        out.close();

                        var file = target?.createFile("", getName())!!
                        mContext.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                            outputStream.write(baos.toByteArray())
                            outputStream.flush()
                        }
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
        }

        return Result.success()
    }


    private fun addToZipRecursive(current: DocumentFile, zip: ZipOutputStream, path: String) {

        current.listFiles().forEach {

            if(it.isDirectory) {
                val e = ZipEntry(path+it.name.toString())
                zip.putNextEntry(e)
                zip.closeEntry()
                addToZipRecursive(it, zip, "$path${it.name}/")
            }
            if(it.isFile) {
                val e = ZipEntry(it.name.toString())
                zip.putNextEntry(e)
                mContext.contentResolver.openInputStream(it.uri)?.use { inputStream ->
                    zip.write(inputStream.readBytes())
                }
                zip.closeEntry()
            }
        }

    }

    private fun getName(): String {
        val date = Date(System.currentTimeMillis())
        val dateFormat = DateFormat.getDateFormat(applicationContext)
        return "target-" + dateFormat.format(date) + ".zip"
    }
}