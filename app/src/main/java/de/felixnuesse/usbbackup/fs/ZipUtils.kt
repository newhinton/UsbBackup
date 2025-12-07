package de.felixnuesse.usbbackup.fs

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import de.felixnuesse.usbbackup.worker.NotificationMiddleware
import de.felixnuesse.usbbackup.worker.Progress
import de.felixnuesse.usbbackup.worker.StateProvider
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipUtils(private var mContext: Context, private var mStateProvider: StateProvider, private var mNotificationMiddleware: NotificationMiddleware) {



    fun addToZipRecursive(current: DocumentFile, zip: ZipOutputStream, path: String, progress: Progress) {
        if(mStateProvider.wasStopped()) return

        current.listFiles().forEach {

            mNotificationMiddleware.onProgressed("Zipping...")
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


    fun writeToZip(zip: ZipOutputStream, uri: Uri) {
        val stream = mContext.contentResolver.openInputStream(uri)
        val buffer = ByteArray(16384)

        var bytesRead: Int
        while (stream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
            zip.write(buffer, 0, bytesRead)
        }
        stream?.close()
    }


}