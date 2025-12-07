package de.felixnuesse.usbbackup.fs

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import de.felixnuesse.usbbackup.worker.NotificationMiddleware
import de.felixnuesse.usbbackup.worker.StateProvider

class FsUtils(private var mContext: Context, private var mNotificationMiddleware: NotificationMiddleware) {


    fun copyFolder(sourceFile: DocumentFile, target: DocumentFile) {
        sourceFile.listFiles().forEach { it ->

            mNotificationMiddleware.onProgressed("Copy...")

            if(it.isFile) {
                writeFile(it, target.createFile("", it.name ?: "New File")!!, false)
            }

            if(it.isDirectory) {
                copyFolder(it, target.createDirectory(it.name ?: "New Directory")!!)
            }
        }
    }

    fun writeFile(source: DocumentFile, target: DocumentFile, replace: Boolean) {
        mContext
            .contentResolver
            .openOutputStream(target.uri)
            ?.use { outputStream ->
                mContext.contentResolver.openInputStream(source.uri)?.use {
                    inputStream ->
                    outputStream.write(inputStream.readAllBytes())
                    outputStream.flush()
                }
            }
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