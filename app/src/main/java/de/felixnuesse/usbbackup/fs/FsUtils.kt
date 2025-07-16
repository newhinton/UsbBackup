package de.felixnuesse.usbbackup.fs

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import de.felixnuesse.usbbackup.worker.StateCallback

class FsUtils(private var mContext: Context, private var mCallback: StateCallback) {


    fun copyFolder(sourceFile: DocumentFile, target: DocumentFile) {
        sourceFile.listFiles().forEach { it ->
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
}