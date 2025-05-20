package de.felixnuesse.usbbackup.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BackupTask (
    @PrimaryKey val id: Int? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sourceUri") val sourceUri: String,
    @ColumnInfo(name = "targetUri") val targetUri: String,
    @ColumnInfo(name = "containerPW") var containerPW: String?,
    @ColumnInfo(name = "enabled") var enabled: Boolean
) {
    companion object {
        fun new(name: String, sourceUri: String, targetUri: String): BackupTask {
            return BackupTask(null, name, sourceUri, targetUri, null, true)
        }
    }
}