package de.felixnuesse.usbbackup

import android.content.Context
import android.net.Uri
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import de.felixnuesse.usbbackup.database.StorageNameDatabase


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

        private fun resolveSafName(context: Context, uri: Uri): String? {
            val authority = uri.authority
            if(authority == null) return null
            val providerInfo = context.packageManager.resolveContentProvider(authority, 0)
            if (providerInfo == null) return null
            val label = providerInfo.loadLabel(context.packageManager)
            return label.toString()
        }

        fun getStorageLabel(context: Context, uri: Uri): String {

            val id = getStorageName(context, getStorageId(uri))
            if(id != null) {
                return id
            }

            if(uri.host.equals("com.android.externalstorage.documents")) {
                return getStorageId(uri)
            }

            val name = resolveSafName(context, uri)
            return name?: getStorageId(uri)
        }


        private fun getStorageName(context: Context, id: String): String? {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val maybeVolume = storageManager.storageVolumes.find { it.uuid == id }

            var name = maybeVolume?.getDescription(context)
            if(maybeVolume == null) {
                val maybeCachedName = StorageNameDatabase(context).getName(id)
                if(maybeCachedName != null) {
                    name = maybeCachedName
                }
            }
            if(name != null) {
                StorageNameDatabase(context).cacheName(id, name)
            }
            return name
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

            val id = getStorageLabel(context, uri)
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