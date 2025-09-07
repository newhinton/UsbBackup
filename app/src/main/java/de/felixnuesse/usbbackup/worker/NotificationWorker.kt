package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.felixnuesse.usbbackup.database.BackupTask.Companion.NEVER
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import de.felixnuesse.usbbackup.utils.DateFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationWorker (private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {



    companion object {
        private const val DAILY_HOUR_TO_RUN = 19

        fun now(context: Context) {
            val data = Data.Builder()
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            request.setInputData(data.build())
            WorkManager.getInstance(context).enqueue(request.build())
        }

        fun schedule(context: Context) {

            val manager = WorkManager.getInstance(context)
            manager.cancelAllWork()



            val repeatInterval = 24L


            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            calendar.set(Calendar.HOUR_OF_DAY, DAILY_HOUR_TO_RUN)
            if(currentHour >= DAILY_HOUR_TO_RUN) {
                calendar.add(Calendar.HOUR, 24)
            }

            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND,0)
            val delay = calendar.timeInMillis - now

            val workRequest = PeriodicWorkRequest.Builder(
                NotificationWorker::class.java,
                repeatInterval,
                TimeUnit.HOURS
            )

            workRequest.setInitialDelay(delay, TimeUnit.MILLISECONDS)
            workRequest.build()

            manager.enqueueUniquePeriodicWork("Test Outdated Backups", ExistingPeriodicWorkPolicy.UPDATE, workRequest.build())
        }
    }

    override fun doWork(): Result {

        val backupTaskMiddleware = BackupTaskMiddleware.get(mContext)
        backupTaskMiddleware.getAll().forEach {
            val hasRunBefore = it.lastSuccessfulBackup != NEVER
            val daysDifference = DateFormatter.daysDifference(it.lastSuccessfulBackup)
            val wasntToday = daysDifference != 0L
            val wasThreeMonthAgo = (daysDifference % 90 == 0L)
            if( hasRunBefore && wasntToday && wasThreeMonthAgo) {
                Notifications(mContext, 0).notifyOutdatedBackup(it)
            }
        }
        return Result.success()
    }


}