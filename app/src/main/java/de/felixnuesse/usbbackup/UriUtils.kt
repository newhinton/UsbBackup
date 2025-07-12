package de.felixnuesse.usbbackup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

class UriUtils {
    companion object {
        fun getStorageId(uri: Uri): String {
            try {
                val id = DocumentsContract.getTreeDocumentId(uri)
                return id.toString().split(":")[0]
            } catch (e: Exception) {
                try {
                    val id = DocumentsContract.getDocumentId(uri)
                    return id.toString().split(":")[0]
                } catch (e: Exception) {
                    return ""
                }
            }
        }

        fun isFolder(context: Context, uri: Uri): Boolean {
            try {
                val folder = DocumentFile.fromTreeUri(context, uri)
                return true
            } catch (e: Exception) {
                val file = DocumentFile.fromSingleUri(context, uri)
                return true
            }
        }

        fun getName(context: Context, uri: Uri): String {
            try {
                val folder = DocumentFile.fromTreeUri(context, uri)
                return folder?.name.toString()
            } catch (e: Exception) { }

            val file = DocumentFile.fromSingleUri(context, uri)
            return file?.name.toString()
        }

        fun getUriMetadata(context: Context, uri: Uri?): String {
            if(uri == null) {
                return "Uri is null!"
            }

            val id = getStorageId(uri)
            try {
                val folder = DocumentFile.fromTreeUri(context, uri)
                return "$id: ${folder?.name}"
            } catch (e: Exception) {
                val file = DocumentFile.fromSingleUri(context, uri)
                return "$id: ${file?.name}"
            }
        }
    }
}