package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import de.felixnuesse.crypto.Crypto
import de.felixnuesse.usbbackup.R
import de.felixnuesse.usbbackup.StorageUtils
import de.felixnuesse.usbbackup.UriUtils
import de.felixnuesse.usbbackup.database.BackupTask
import de.felixnuesse.usbbackup.fs.FsUtils
import de.felixnuesse.usbbackup.fs.ZipUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipOutputStream

class BackupProcessor(private var mContext: Context, private var stateProvider: StateProvider): ProgressProvider {



    private var mNotificationMiddleware = NotificationMiddleware(this.mContext, this)

    private var mZipUtils = ZipUtils(mContext, stateProvider, mNotificationMiddleware)
    private var mFsUtils = FsUtils(mContext, mNotificationMiddleware)
    var mProgress = Progress()


    fun process(it: BackupTask): Boolean {

        if(!it.enabled){
            return false
        }

        if(it.id == null) {
            mNotificationMiddleware.error(
                mContext.getString(R.string.backup_notification_error),
                mContext.getString(R.string.backup_notification_noid)
            )
            // todo
            //return ListenableWorker.Result.failure()
            return false
        }
        mNotificationMiddleware.prepare(stateProvider.workerId(), it.id!!)

        Log.e("WORKER", "Current Task: ${it.targetUri}, ${it.name}")

        return processTask(it)
    }


    private fun processTask(backupTask: BackupTask): Boolean {

        mNotificationMiddleware.log(
            mContext.getString(R.string.backup_notification_ongoing_title, backupTask.name),
            mContext.getString(R.string.backup_notification_ongoing_),
            true
        )

        val targetFolder = try {
            DocumentFile.fromTreeUri(mContext, backupTask.targetUri.toUri())?.createDirectory("USBBackup_${getFormattedDate()}")!!
        } catch (_: Exception) {
            mNotificationMiddleware.error(
                mContext.getString(R.string.backup_notification_ongoing_failure),
                mContext.getString(
                    R.string.backup_notification_task_could_not_write_to_storage,
                    backupTask.name,
                    StorageUtils.state(mContext, backupTask.targetUri.toUri())
                ))
            return false
        }

        val unencryptedCacheFile = File(mContext.cacheDir, "cache.zip")
        unencryptedCacheFile.createNewFile()

        mProgress = Progress()
        try {
            backupTask.sources.forEach {
                mProgress.overallSize += mFsUtils.calculateSize(DocumentFile.fromTreeUri(mContext, it.uri.toUri())!!)
            }

            ZipOutputStream(FileOutputStream(unencryptedCacheFile)).use {
                backupTask.sources.forEach { source ->

                    val sourceUri = source.uri.toUri()
                    mProgress.currentSource = UriUtils.getName(mContext, sourceUri)

                    val sourceDocument = DocumentFile.fromTreeUri(mContext, sourceUri)
                    if(sourceDocument?.canRead() != true) {
                        mNotificationMiddleware.error(
                            mContext.getString(R.string.backup_notification_ongoing_error_with_source),
                            mContext.getString(R.string.backup_notification_ongoing_error_with_source_details, backupTask.name)
                        )
                        return false
                    }

                    if(UriUtils.isFolder(mContext, sourceUri)) {
                        if(source.encrypt) {
                            mZipUtils.addToZipRecursive(sourceDocument, it, "${mProgress.currentSource}/", mProgress)
                        } else {
                            mNotificationMiddleware.log(
                                mContext.getString(R.string.backup_notification_ongoing_title, backupTask.name),
                                mContext.getString(R.string.backup_notification_ongoing_storing),
                                true,
                                false
                            )
                            mFsUtils.copyFolder(sourceDocument, targetFolder.createDirectory(mProgress.currentSource)!!)
                        }
                    } else {
                        //todo: files????
                    }

                    if(stateProvider.wasStopped()) {
                        unencryptedCacheFile.delete()
                        mNotificationMiddleware.endTask()
                        return false
                    }
                }
            }

            val encryptedTasks = backupTask.sources.filter { it.encrypt }.toList()


            if(!encryptedTasks.isEmpty()) {
                if(backupTask.containerPW.isNullOrBlank()) {
                    Log.e("BackupProcessor", "Could not encrypt since password not set! Abort!")
                    return false
                }

                mNotificationMiddleware.log(
                    mContext.getString(R.string.backup_notification_ongoing_title, backupTask.name),
                    mContext.getString(R.string.backup_notification_ongoing_encrypting),
                    ongoing = true,
                    cancelable = false
                )
                if(!handleEncryption(targetFolder, backupTask, unencryptedCacheFile)) {
                    return false
                }
                handleEncryptionTools(targetFolder, backupTask)
            }
            unencryptedCacheFile.delete()

            mNotificationMiddleware.success(
                mContext.getString(R.string.backup_notification_done),
                mContext.getString(R.string.backup_notification_done_remove_media, backupTask.name)
            )
            return true
        } catch (e: Exception) {
            mNotificationMiddleware.error(
                mContext.getString(R.string.backup_notification_unspecified_failure),
                mContext.getString(R.string.backup_notification_unspecified_failure_description, backupTask.name, e.message),
                backupTask.id!!)
            e.printStackTrace()
        }
        return false
    }


    private fun handleEncryption(targetFolder: DocumentFile, backupTask: BackupTask, source: File): Boolean {

        val password = backupTask.containerPW!!.toCharArray()
        val encryptedZipUri = targetFolder.createFile("", "encrypted_backup.zip")!!.uri


        Log.e("BackupProcessor", "Encrypting...")
        val targetFile = mContext.contentResolver.openOutputStream(encryptedZipUri)!!
        Crypto().aesEncrypt(source.inputStream(), targetFile, password)

        Log.e("BackupProcessor", "Decrypting...")
        val decrypted = mContext.contentResolver.openInputStream(encryptedZipUri)!!
        val target = File(mContext.externalCacheDir, "de_" + getFormattedDate() + ".zip")

        Log.e("BackupProcessor", "Decrypting: ${target.absolutePath}")
        Crypto().aesDecrypt(decrypted, target.outputStream(), password)
        return true
    }

    private fun handleEncryptionTools(targetFolder: DocumentFile, backupTask: BackupTask) {
        val decryptToolPath = mContext.assets.list("")?.firstOrNull { it.startsWith("aes-tool") }
        val decryptReadmePath = mContext.assets.list("")?.firstOrNull { it.startsWith("README") }

        Log.e("BackupProcessor", "Update decrypt-tool...")
        writeSingleFile(decryptToolPath, targetFolder.parentFile!!, backupTask.name, "Updating tool...", false, backupTask.id)
        Log.e("BackupProcessor", "Update decrypt-readme...")
        writeSingleFile(decryptReadmePath, targetFolder.parentFile!!, backupTask.name, "Updating readme...", true, backupTask.id)
    }

    fun writeSingleFile(path: String?, targetFolder: DocumentFile, taskname: String, task: String, replace: Boolean, id: Int?) {
        path?.let { fileName ->
            val targetTool = targetFolder.findFile(fileName)
            if(targetTool==null || replace) {
                mNotificationMiddleware.log(mContext.getString(R.string.backup_notification_ongoing_title, taskname), task, true)
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
        val format = SimpleDateFormat("yyyy-MM-dd_HH-mm")
        return format.format(date)
    }

    override fun getProgress(): Progress {
        return mProgress;
    }
}