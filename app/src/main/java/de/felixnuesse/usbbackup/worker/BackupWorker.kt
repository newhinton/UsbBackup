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
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import de.felixnuesse.usbbackup.fs.FsUtils
import de.felixnuesse.usbbackup.fs.ZipUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.format

class BackupWorker(private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams), StateCallback {

    private var zipUtils = ZipUtils(mContext, this)
    private var fsUtils = FsUtils(mContext, this)

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

        val id = (0..Integer.MAX_VALUE).random()
        mNotifications = Notifications(mContext, id)
        mNotifications.mUuid = this.id

        var finalMessage = ""

        tasks.forEach {
            if(it.enabled){
                Log.e("WORKER", "Current Task: ${storageId?: taskId}, ${it.name}")
                finalMessage += if(processTask(it)) {
                    "${it.name} was sucessfully backed up.\n"
                } else {
                    "Warning: ${it.name} was NOT backed up!\n"
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

        mNotifications.showNotification("Backing up ${backupTask.name}...", "Zipping...", true)

        val targetFolder = try {
            DocumentFile.fromTreeUri(mContext, backupTask.targetUri.toUri())?.createDirectory("date_${getFormattedDate()}")!!
        } catch (e: Exception) {
            mNotifications.showNotification("Backup Failure!", "Task: ${backupTask.name}, could not write to storage: ${StorageUtils.state(mContext, backupTask.targetUri.toUri())}")
            return false
        }

        val unencryptedCacheFile = File(mContext.cacheDir, "cache.zip")
        unencryptedCacheFile.createNewFile()

        val progress = Progress()
        try {
            backupTask.sources.forEach {
                progress.overallSize += calculateSize(DocumentFile.fromTreeUri(mContext, it.uri.toUri())!!)
            }

            // todo: maybe use following scheme:
            //      ./date/encrypted.zip
            //      ./date/decryptedA
            //      ./date/decryptedB
            //      ./readme,etc

            // todo: create the date folder in the cache dir, and then move that after everything else.
            // That way, we dont leave half a backup if something breaks!

            ZipOutputStream(FileOutputStream(unencryptedCacheFile)).use {
                backupTask.sources.forEach { source ->

                    val sourceUri = source.uri.toUri()
                    val sourceDocument = DocumentFile.fromTreeUri(mContext, sourceUri)
                    if(sourceDocument?.canRead() != true) {
                        mNotifications.showError("Error: Could not backup!", "There was an error backing up ${backupTask.name}. We could not read the source folder!")
                        return false
                    }

                    if(UriUtils.isFolder(mContext, sourceUri)) {
                        val namedPath = UriUtils.getName(mContext, sourceUri)
                        if(source.encrypt) {
                            zipUtils.addToZipRecursive(sourceDocument, it, "$namedPath/", progress)
                        } else {
                            mNotifications.showNotification("Backing up ${backupTask.name}...", "Storing Folder...", true, false)
                            Log.e("Tag", "Storing folder...")
                            fsUtils.copyFolder(sourceDocument, targetFolder.createDirectory(namedPath)!!)
                        }
                    } else {
                        //todo: files????
                    }

                    if(this.isStopped) {
                        unencryptedCacheFile.delete()
                        mNotifications.dismiss()
                        return false
                    }
                }
            }

            val encryptedTasks = backupTask.sources.filter { it.encrypt }.toList()


            if(!encryptedTasks.isEmpty()) {
                if(backupTask.containerPW.isNullOrBlank()) {
                    Log.e("Tag", "Could not encrypt since password not set! Abort!")
                    return false
                }

                mNotifications.showNotification("Backing up ${backupTask.name}...", "Encrypting...", true, false)
                if(!handleEncryption(targetFolder, backupTask, unencryptedCacheFile)) {
                    return false
                }
                handleEncryptionTools(targetFolder, backupTask)
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

    private fun handleEncryption(targetFolder: DocumentFile, backupTask: BackupTask, source: File): Boolean {

        val password = backupTask.containerPW!!.toCharArray()
        val encryptedZipUri = targetFolder.createFile("", "encrypted_backup.zip")!!.uri


        Log.e("Tag", "Encrypting...")
        val targetFile = mContext.contentResolver.openOutputStream(encryptedZipUri)!!
        Crypto().aesEncrypt(source.inputStream(), targetFile, password)

        Log.e("Tag", "Decrypting...")
        val decrypted = mContext.contentResolver.openInputStream(encryptedZipUri)!!
        val target = File(mContext.externalCacheDir, "de_"+getName(backupTask))

        Log.e("Tag", "Decrypting: ${target.absolutePath}")
        Crypto().aesDecrypt(decrypted, target.outputStream(), password)
        return true
    }

    private fun handleEncryptionTools(targetFolder: DocumentFile, backupTask: BackupTask) {
        val decryptToolPath = mContext.assets.list("")?.firstOrNull { it.startsWith("aes-tool") }
        val decryptReadmePath = mContext.assets.list("")?.firstOrNull { it.startsWith("README") }

        Log.e("Tag", "Update decrypt-tool...")
        writeSingleFile(decryptToolPath, targetFolder, backupTask.name, "Updating tool...", false)
        Log.e("Tag", "Update decrypt-readme...")
        writeSingleFile(decryptReadmePath, targetFolder, backupTask.name, "Updating readme...", true)
    }



    fun writeSingleFile(path: String?, targetFolder: DocumentFile, taskname: String, task: String, replace: Boolean) {
        path?.let { fileName ->
            val targetTool = targetFolder.findFile(fileName)
            if(targetTool==null || replace) {
                mNotifications.showNotification("Backing up $taskname...", task, true)
                val decryptTool = targetFolder.createFile("", fileName)!!
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

    private fun getFormattedDate(): String {
        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm")
        return format.format(date)
    }


    private fun getName(task: BackupTask): String {
        val prefix = if(!task.containerPW.isNullOrBlank()) "encrypted_"  else ""
        return "$prefix${task.name}_" + getFormattedDate() + ".zip"
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

    override fun onProgressed() {
        TODO("Not yet implemented")
    }

    override fun wasStopped(): Boolean {
        return this.isStopped
    }
}