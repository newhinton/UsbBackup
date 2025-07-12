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
import de.felixnuesse.crypto.Crypto
import de.felixnuesse.usbbackup.StorageUtils
import de.felixnuesse.usbbackup.UriUtils
import de.felixnuesse.usbbackup.database.AppDatabase
import de.felixnuesse.usbbackup.database.BackupTask
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupWorker(private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {


    inner class Progress() {
        var overallSize = 0L
        var calculatedSize = 0L

        fun getProgress(): Int {
            val progress = (100L * calculatedSize) / overallSize
            Log.e("Tag", "progress: ${progress.toInt()} $calculatedSize $overallSize")
            return progress.toInt()
        }
    }


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
            Log.e("TAG", "Stop Work: $uuid")
            WorkManager.getInstance(context).cancelWorkById(uuid)
        }
    }

    private var mNotifications = Notifications(mContext, 0)

    override fun doWork(): Result {

        var storageId = inputData.getString("storageId")
        var taskId = inputData.getInt("taskId", -1)

        if(storageId == null && taskId == -1) {
            return Result.failure()
        }


        val db = AppDatabase.getDatabase(mContext)


        var tasks = arrayListOf<BackupTask>()

        if(storageId != null) {
            tasks.addAll(db.backupDao().getAll().filter { UriUtils.getStorageId(it.targetUri.toUri()) == storageId })
        }

        if(taskId != -1) {
            tasks.add(db.backupDao().get(taskId))
        }


        Log.e("WORKER", "Scan tasks... ${tasks.size}")

        val id = (0..Integer.MAX_VALUE).random()
        mNotifications = Notifications(mContext, id)
        mNotifications.mUuid = this.id

        var finalMessage = ""

        tasks.forEach {
            if(it.enabled){
                Log.e("WORKER", "Current Task: ${storageId?: taskId}, ${it.name}")
                if(processTask(it)) {
                    finalMessage += "${it.name} was sucessfully backed up.\n"

                } else {
                    finalMessage += "Warning: ${it.name} was NOT backed up!\n"
                }
            }
        }

        if(finalMessage.isNotBlank()) {
            finalMessage += "\nYou can safely remove the media."
            mNotifications.showNotification("Backup Done!", finalMessage)
            return Result.success()
        }

        return Result.failure()
    }


    override fun onStopped() {
        super.onStopped()
        Log.e("Tag", "Was stopped! ${this.id}")
    }


    private fun processTask(backupTask: BackupTask): Boolean {


        // todo: wrap and catch exceptions

        mNotifications.showNotification("Backing up ${backupTask.name}...", "Zipping...", true)

        try {
            // todo: was source
            val sourceUri = backupTask.targetUri.toUri()

            var sourceDocument = DocumentFile.fromTreeUri(mContext, sourceUri)

            if(sourceDocument?.canRead() != true) {
                mNotifications.showError("Error: Could not backup!", "There was an error backing up ${backupTask.name}. We could not read the source folder!")
                return false
            }

            val progress = Progress()
            progress.overallSize = calculateSize(sourceDocument!!)

            var unencryptedCacheFile = File(mContext.cacheDir, "cache.zip")
            ZipOutputStream(FileOutputStream(unencryptedCacheFile)).use { out ->
                if(UriUtils.isFolder(mContext, sourceUri)) {
                    addToZipRecursive(sourceDocument, out, "", progress)
                    out.close()
                } else {
                    //todo: files????
                }
            }

            if(this.isStopped) {
                unencryptedCacheFile.delete()
                mNotifications.dismiss()
                return false
            }

            var targetFolder = DocumentFile.fromTreeUri(mContext, backupTask.targetUri.toUri())

            var file = try {
                targetFolder?.createFile("", getName(backupTask))!!
            } catch (e: Exception) {
                mNotifications.showNotification("Backup Failure!", "Task: ${backupTask.name}, could not write to storage: ${StorageUtils.state(mContext, backupTask.targetUri.toUri())}")
                return false
            }

            if(!backupTask.containerPW.isNullOrBlank()) {
                mNotifications.showNotification("Backing up ${backupTask.name}...", "Encrypting...", true, false)
                Log.e("Tag", "Encrypting...")
                Crypto().aesEncrypt(unencryptedCacheFile.inputStream(), mContext.contentResolver.openOutputStream(file.uri)!!, backupTask.containerPW!!.toCharArray())

                Log.e("Tag", "Decrypting...")
                val decrypted = mContext.contentResolver.openInputStream(file.uri)!!
                val target = File(mContext.externalCacheDir, "de_"+getName(backupTask))
                Crypto().aesDecrypt(decrypted, target.outputStream(), backupTask.containerPW!!.toCharArray())

                Log.e("Tag", "Update decrypt-tool...")
                var decryptToolPath = mContext.assets.list("")?.firstOrNull { it.startsWith("aes-tool") }
                writeSingleFile(decryptToolPath, targetFolder, backupTask.name, "Updating tool...", false)
                Log.e("Tag", "Update decrypt-readme...")
                var decryptReadmePath = mContext.assets.list("")?.firstOrNull { it.startsWith("README") }
                writeSingleFile(decryptReadmePath, targetFolder, backupTask.name, "Updating readme...", true)

            } else {
                mNotifications.showNotification("Backing up ${backupTask.name}...", "Storing...", true, false)
                Log.e("Tag", "Storing...")
                mContext.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    outputStream.write(unencryptedCacheFile.readBytes())
                    outputStream.flush()
                }
            }

            unencryptedCacheFile.delete()
            Log.e("Tag", "Done!")
            mNotifications.showNotification("Backup Done!", "${backupTask.name} was sucessfully backed up. You can safely remove the media.")
            return true
        } catch (e: Exception) {
            Log.e("Tag", "Error: ${e.message}")
            e.printStackTrace()
            mNotifications.showNotification(backupTask.id!!, "Backup Failure!", "Task: ${backupTask.name}, error: ${e.message}")
        }
        return false
    }


    private fun addToZipRecursive(current: DocumentFile, zip: ZipOutputStream, path: String, progress: Progress) {
        if(this.isStopped) return
        current.listFiles().forEach {
            mNotifications.showNotification("Backing up ${it.name}...", "Zipping...", true, false, progress.getProgress())
            //Log.e("Tag", "Processing: $path${it.name}")
            if(it.isDirectory) {
                val entry = ZipEntry("$path${it.name}/")
                zip.putNextEntry(entry)
                zip.closeEntry()
                // overall size is wrong here. That "resets" the percentage. ideally, we use a "global" variable for both overallSize and calculated size.
                addToZipRecursive(it, zip, "$path${it.name}/", progress)
            }
            if(it.isFile) {
                progress.calculatedSize += it.length()
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

    private fun writeSingleFile(path: String?, targetFolder: DocumentFile, taskname: String, task: String, replace: Boolean) {
        path?.let { fileName ->
            var targetTool = targetFolder.findFile(fileName)
            if(targetTool==null || replace) {
                mNotifications.showNotification("Backing up $taskname...", task, true)
                var decryptTool = targetFolder.createFile("", fileName)!!
                if(replace) {
                    targetTool?.delete()
                }
                mContext.contentResolver.openOutputStream(decryptTool.uri, "w")?.use { outputStream ->
                    outputStream.write(mContext.assets.open(fileName).readAllBytes())
                    outputStream.flush()
                }
            }
        }
    }

    private fun getName(task: BackupTask): String {
        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm")

        val prefix = if(!task.containerPW.isNullOrBlank()) "encrypted_"  else ""

        return "$prefix${task.name}_" + format.format(date) + ".zip"
    }

    fun calculateSize(root: DocumentFile): Long {
        var folderSize = 0L
        if (root.isDirectory) {
            root.listFiles().forEach {
                folderSize += calculateSize(it)
            }
        } else {
            folderSize += root.length()
        }
        return folderSize
    }
}