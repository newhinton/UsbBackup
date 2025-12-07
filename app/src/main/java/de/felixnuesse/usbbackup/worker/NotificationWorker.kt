package de.felixnuesse.usbbackup.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.felixnuesse.usbbackup.database.BackupTask.Companion.NEVER
import de.felixnuesse.usbbackup.database.BackupTask.Companion.WARNING_DISABLED
import de.felixnuesse.usbbackup.database.BackupTaskMiddleware
import de.felixnuesse.usbbackup.utils.DateFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationWorker (private var mContext: Context, workerParams: WorkerParameters): Worker(mContext, workerParams) {


    companion object {

        private const val NOTIFICATION_WORKER_WORK_TAG = "NOTIFICATION_WORKER_WORK_TAG"
        private const val DAILY_HOUR_TO_RUN = 19

        fun now(context: Context) {
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            request.setInputData(workDataOf("forceRun" to true))
            request.setInitialDelay(0, TimeUnit.MILLISECONDS)
            WorkManager.getInstance(context).enqueueUniqueWork("test", ExistingWorkPolicy.REPLACE, request.build())
        }

        fun schedule(context: Context) {

            val manager = WorkManager.getInstance(context)
            manager.cancelAllWorkByTag(NOTIFICATION_WORKER_WORK_TAG)

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
            workRequest.addTag(NOTIFICATION_WORKER_WORK_TAG)

            manager.enqueueUniquePeriodicWork("Test For Outdated Backups", ExistingPeriodicWorkPolicy.UPDATE, workRequest.build())
        }
    }

    override fun doWork(): Result {
        val backupTaskMiddleware = BackupTaskMiddleware.get(mContext)
        backupTaskMiddleware.getAll().forEach {
            // dont warn when we don't have a warning
            if(it.warningTimeout == WARNING_DISABLED) {
                return@forEach
            }

            //  or if we never ran to begin with.
            if(it.lastSuccessfulBackup == NEVER) {
                return@forEach
            }

            val daysSinceLastRun = DateFormatter.daysDifference(it.getLastSuccessfulBackup()) * -1
            if(daysSinceLastRun < it.getWarningTimeout()) {
                // the timeout is bigger than the passed time since the last run
                return@forEach
            }

            Notifications(mContext, 0).notifyOutdatedBackup(it)
        }
        return Result.success()
    }


}