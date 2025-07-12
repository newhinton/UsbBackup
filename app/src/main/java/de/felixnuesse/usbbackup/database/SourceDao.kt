package de.felixnuesse.usbbackup.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SourceDao {

    @Query("SELECT * FROM Source")
    fun getAll(): List<Source>

    @Query("SELECT * FROM Source WHERE parentId=:id")
    fun getByParent(id: Int): List<Source>

    @Insert
    fun insert(entry: Source)

    @Update
    fun update(entry: Source)

    @Delete
    fun delete(entry: Source)

    @Query("DELETE FROM Source WHERE parentId = :id")
    fun deleteByParentId(id: Int)
    
}