package de.felixnuesse.usbbackup.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class BackupTask (
    @PrimaryKey var id: Int? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "targetUri") val targetUri: String,
    @ColumnInfo(name = "containerPW") var containerPW: String?,
    @ColumnInfo(name = "enabled") var enabled: Boolean
) {

    @Ignore var sources: ArrayList<Source> = arrayListOf()

    companion object {
        fun new(name: String, sources: ArrayList<Source>, targetUri: String): BackupTask {
            val task = BackupTask(null, name, targetUri, null, true)
            task.sources = sources
            return task
        }
    }
}