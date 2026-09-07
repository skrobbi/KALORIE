package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.model.UserProfile
import com.example.data.repository.NutritionRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyNutritionAnalysisWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "DailyNutritionAnalysisWorker started running at 23:00 background schedule")
        return try {
            val repository = NutritionRepository(applicationContext)

            // Default or stored profile
            val profile = UserProfile(
                diet = "обычная",
                allergiesOrIntolerance = listOf("лактоза"),
                dislikedFoods = listOf("печень", "брокколи"),
                budgetPreference = "экономные простые продукты"
            )

            // Analyze 3 days of accumulated micronutrients
            val result = repository.performDeficitAnalysis(days = 3, userProfile = profile)
            Log.d(TAG, "Worker successfully completed deficit analysis. Deficits: ${result.primaryDeficits.size}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily deficit analysis worker: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "NutritionWorker"
        const val WORK_NAME = "DailyNutritionAnalysisWork"

        fun schedule(context: Context) {
            // Calculate delay until 23:00
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.HOUR_OF_DAY, 24)
            }

            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyNutritionAnalysisWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
            Log.d(TAG, "Scheduled DailyNutritionAnalysisWorker for 23:00 (delay: ${timeDiff / 1000 / 60} minutes)")
        }
    }
}
