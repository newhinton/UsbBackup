package de.felixnuesse.usbbackup.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(primaryKeys = ["parentId", "sourceUri"])
data class Source (
    @ColumnInfo(name = "parentId") var parentId: Int = -1,
    @ColumnInfo(name = "sourceUri") val uri: String,
    @ColumnInfo(name = "encrypt") var encrypt: Boolean
)