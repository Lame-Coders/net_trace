package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Define the constraints: MUST be on Unmetered Wi-Fi
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        // Create the hourly repeating request
        val speedTestRequest = PeriodicWorkRequestBuilder<SpeedTestWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Queue it up in WorkManager
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "HourlySpeedTest",
            ExistingPeriodicWorkPolicy.KEEP, // Don't duplicate if app is opened again
            speedTestRequest
        )
    }
}