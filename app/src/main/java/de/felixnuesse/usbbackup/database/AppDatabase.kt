package de.felixnuesse.usbbackup.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(
    entities = [BackupTask::class, Source::class],
    version = 4,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun backupDao(): BackupTaskDao
    abstract fun sourceDao(): SourceDao

    companion object {

        fun getDatabase(context: Context): AppDatabase {
            val db = Room.databaseBuilder(
                context,
                AppDatabase::class.java, "usbbackup"
            )
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .build()

            return db
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE BackupTask ADD COLUMN lastSuccessfulBackup INTEGER DEFAULT -1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE BackupTask ADD COLUMN warningTimeout INTEGER DEFAULT 0")
            }
        }
    }
}