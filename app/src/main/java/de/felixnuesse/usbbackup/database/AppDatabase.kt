package de.felixnuesse.usbbackup.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [BackupTask::class, Source::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun backupDao(): BackupTaskDao
    abstract fun sourceDao(): SourceDao


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