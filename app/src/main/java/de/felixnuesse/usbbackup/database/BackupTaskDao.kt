package de.felixnuesse.usbbackup.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BackupTaskDao {

    @Query("SELECT * FROM BackupTask")
    fun getAll(): List<BackupTask>

    @Query("SELECT * FROM BackupTask WHERE id=:id")
    fun get(id: Int): BackupTask

    @Insert
    fun insertAll(vararg entry: BackupTask)

    @Insert
    fun insert(entry: BackupTask)

    @Delete
    fun delete(entry: BackupTask)
}