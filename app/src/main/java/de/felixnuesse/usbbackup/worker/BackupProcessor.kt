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
import de.felixnuesse.usbbackup.mediascanning.Scanner
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

    lateinit var mCurrentTask: BackupTask


    fun process(it: BackupTask): Boolean {

        Log.e("BackupProcessor", "Preparing: ${it.enabled}, ${it.name}")
        mCurrentTask = it
        if(!mCurrentTask.enabled){
            return false
        }

        if(mCurrentTask.id == null) {
            mNotificationMiddleware.error(
                mContext.getString(R.string.backup_notification_error),
                mContext.getString(R.string.backup_notification_noid)
            )
            // todo
            //return ListenableWorker.Result.failure()
            return false
        }
        mNotificationMiddleware.prepare(stateProvider.workerId(), mCurrentTask.id!!)

        Log.e("BackupProcessor", "Working Task: ${mCurrentTask.targetUri}, ${mCurrentTask.name}")

        return processTask()
    }

    fun shouldDeviceDisconnectEndTask(): Boolean {
        Log.e("BackupProcessor", "Recieved Device Disconnected call! Handling...")
        val targetVolume = UriUtils.getStorageId(mCurrentTask.targetUri.toUri())
        if(!Scanner(mContext).isVolumeConnected(targetVolume)) {
            mNotificationMiddleware.error(
                mContext.getString(R.string.backup_notification_ongoing_failure),
                mContext.getString(R.string.backup_notification_storage_was_removed, mCurrentTask.name)
            )
            return true
        }
        return false
    }


    private fun processTask(): Boolean {

        mNotificationMiddleware.log(
            mContext.getString(R.string.backup_notification_ongoing_title, mCurrentTask.name),
            mContext.getString(R.string.backup_notification_ongoing_),
            true
        )

        val targetFolder = try {
            DocumentFile.fromTreeUri(mContext, mCurrentTask.targetUri.toUri())?.createDirectory("USBBackup_${getFormattedDate()}")!!
        } catch (_: Exception) {
            mNotificationMiddleware.error(
                mContext.getString(R.string.backup_notification_ongoing_failure),
                mContext.getString(
                    R.string.backup_notification_task_could_not_write_to_storage,
                    mCurrentTask.name,
                    StorageUtils.state(mContext, mCurrentTask.targetUri.toUri())
                ))
            return false
        }

        val unencryptedCacheFile = File(mContext.cacheDir, "cache.zip")
        unencryptedCacheFile.createNewFile()

        mProgress = Progress()
        try {
            mCurrentTask.sources.forEach {
                mProgress.overallSize += mFsUtils.calculateSize(DocumentFile.fromTreeUri(mContext, it.uri.toUri())!!)
            }

            ZipOutputStream(FileOutputStream(unencryptedCacheFile)).use {
                mCurrentTask.sources.forEach { source ->

                    val sourceUri = source.uri.toUri()
                    mProgress.currentSource = UriUtils.getName(mContext, sourceUri)

                    val sourceDocument = DocumentFile.fromTreeUri(mContext, sourceUri)
                    if(sourceDocument?.canRead() != true) {
                        mNotificationMiddleware.error(
                            mContext.getString(R.string.backup_notification_ongoing_error_with_source),
                            mContext.getString(R.string.backup_notification_ongoing_error_with_source_details, mCurrentTask.name)
                        )
                        return false
                    }

                    if(UriUtils.isFolder(mContext, sourceUri)) {
                        if(source.encrypt) {
                            mZipUtils.addToZipRecursive(sourceDocument, it, "${mProgress.currentSource}/", mProgress)
                        } else {
                            mNotificationMiddleware.log(
                                mContext.getString(R.string.backup_notification_ongoing_title, mCurrentTask.name),
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

            val encryptedTasks = mCurrentTask.sources.filter { it.encrypt }.toList()


            if(!encryptedTasks.isEmpty()) {
                if(mCurrentTask.containerPW.isNullOrBlank()) {
                    Log.e("BackupProcessor", "Could not encrypt since password not set! Abort!")
                    return false
                }

                mNotificationMiddleware.log(
                    mContext.getString(R.string.backup_notification_ongoing_title, mCurrentTask.name),
                    mContext.getString(R.string.backup_notification_ongoing_encrypting),
                    ongoing = true,
                    cancelable = false
                )
                if(!handleEncryption(targetFolder, unencryptedCacheFile)) {
                    return false
                }
                handleEncryptionTools(targetFolder)
            }
            unencryptedCacheFile.delete()

            mNotificationMiddleware.success(
                mContext.getString(R.string.backup_notification_done),
                mContext.getString(R.string.backup_notification_done_remove_media, mCurrentTask.name)
            )
            return true
        } catch (e: Exception) {
            mNotificationMiddleware.error(
                mContext.getString(R.string.backup_notification_unspecified_failure),
                mContext.getString(R.string.backup_notification_unspecified_failure_description, mCurrentTask.name, e.message),
                mCurrentTask.id!!)
            e.printStackTrace()
        }
        return false
    }


    private fun handleEncryption(targetFolder: DocumentFile, source: File): Boolean {

        val password = mCurrentTask.containerPW!!.toCharArray()
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

    private fun handleEncryptionTools(targetFolder: DocumentFile) {
        val decryptToolPath = mContext.assets.list("")?.firstOrNull { it.startsWith("aes-tool") }
        val decryptReadmePath = mContext.assets.list("")?.firstOrNull { it.startsWith("README") }

        Log.e("BackupProcessor", "Update decrypt-tool...")
        writeSingleFile(decryptToolPath, targetFolder.parentFile!!, mCurrentTask.name, "Updating tool...", false, mCurrentTask.id)
        Log.e("BackupProcessor", "Update decrypt-readme...")
        writeSingleFile(decryptReadmePath, targetFolder.parentFile!!, mCurrentTask.name, "Updating readme...", true, mCurrentTask.id)
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
        return mProgress
    }
}