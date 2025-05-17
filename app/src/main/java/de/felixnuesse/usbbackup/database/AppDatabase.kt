package de.felixnuesse.usbbackup.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BackupTask::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun backupDao(): BackupTaskDao

    companion object {
        fun getDatabase(context: Context): AppDatabase {
            val db = Room.databaseBuilder(
                context,
                AppDatabase::class.java, "usbbackup"
            )
                .fallbackToDestructiveMigration()
                .build()
            return db
        }
    }
}