package de.felixnuesse.usbbackup.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class BackupTask (
    @PrimaryKey var id: Int? = null,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "targetUri") val targetUri: String,
    @ColumnInfo(name = "containerPW") var containerPW: String?,
    @ColumnInfo(name = "enabled") var enabled: Boolean,
    @ColumnInfo(name = "lastSuccessfulBackup") var lastSuccessfulBackup: Long? = NEVER
) {

    @Ignore var sources: ArrayList<Source> = arrayListOf()

    @Ignore fun getLastSuccessfulBackup(): Long {
        return lastSuccessfulBackup?: NEVER
    }

    companion object {
        const val NEVER = -1L
        fun new(name: String, sources: ArrayList<Source>, targetUri: String): BackupTask {
            val task = BackupTask(null, name, targetUri, null, true, NEVER)
            task.sources = sources
            return task
        }
    }
}