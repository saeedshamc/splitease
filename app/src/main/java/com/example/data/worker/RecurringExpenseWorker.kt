package com.example.data.worker

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.*
import com.example.data.database.AppDatabase
import com.example.data.model.Expense
import com.example.data.model.ExpenseSplit
import com.example.data.repository.ExpenseRepository
import java.util.concurrent.TimeUnit

class RecurringExpenseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE expenses ADD COLUMN actualPayerName TEXT DEFAULT NULL")
                }
            }
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "splitease_db"
            ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration().build()

            val repository = ExpenseRepository(db)
            val now = System.currentTimeMillis()
            val schedules = repository.getAllActiveSchedulesSync()
            var processedCount = 0

            schedules.forEach { schedule ->
                if (schedule.nextDueDate <= now && schedule.isActive) {
                    val membersList = repository.getMembersForGroupSync(schedule.groupId)
                    if (membersList.isNotEmpty()) {
                        val splits = mutableListOf<ExpenseSplit>()
                        val portion = schedule.amount / membersList.size
                        membersList.forEach { member ->
                            splits.add(ExpenseSplit(expenseId = 0, memberId = member.id, amount = portion))
                        }
                        val expense = Expense(
                            groupId = schedule.groupId,
                            title = schedule.title,
                            amount = schedule.amount,
                            category = schedule.category,
                            payerId = schedule.payerId,
                            splitType = schedule.splitType,
                            timestamp = now,
                            isRecurring = true,
                            dueDate = now + getIntervalMillis(schedule.frequency)
                        )
                        repository.insertExpense(expense, splits)
                        
                        val prefs = applicationContext.getSharedPreferences("SplitEasePrefs", Context.MODE_PRIVATE)
                        if (prefs.getBoolean("enableDebtAlerts", true)) {
                            com.example.util.NotificationHelper.showDebtAlert(
                                applicationContext,
                                "🔔 Recurring Bill Due / قبض دوره‌ای",
                                "New bill added: ${schedule.title} (${schedule.amount})"
                            )
                        }

                        val interval = getIntervalMillis(schedule.frequency)
                        var updatedDue = schedule.nextDueDate + interval
                        while (updatedDue <= now) {
                            updatedDue += interval
                        }
                        repository.updateSchedule(schedule.copy(nextDueDate = updatedDue))
                        processedCount++
                    }
                }
            }
            db.close()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun getIntervalMillis(frequency: String): Long {
        return when (frequency.uppercase()) {
            "WEEKLY", "هفتگی" -> 86400000L * 7L
            "YEARLY", "ساله", "سالانه" -> 86400000L * 365L
            else -> 86400000L * 30L // MONTHLY default
        }
    }
}

object RecurringWorkScheduler {
    private const val WORK_NAME = "RecurringExpenseWorkPeriodic"
    private const val IMMEDIATE_WORK_NAME = "RecurringExpenseWorkImmediate"

    fun setupPeriodicWork(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun runImmediateCheck(context: Context) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<RecurringExpenseWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
