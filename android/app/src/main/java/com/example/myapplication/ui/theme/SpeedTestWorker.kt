package com.yourusername.nettrace // Update this!

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SpeedTestWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val deviceName = "Android-Phone"
    private val fileName = "nettrace_log.csv"

    override fun doWork(): Result {
        Log.d("NetTrace", "Background task triggered!")
        
        // Use external files dir so the user can access it via a File Manager
        val directory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, fileName)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:00", Locale.getDefault())
        val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault())
        val now = Date()

        try {
            // 1. Create file & headers if it doesn't exist
            if (!file.exists()) {
                FileWriter(file, true).use { writer ->
                    writer.append("Date,Time,Device,Download_Mbps,Upload_Mbps,Ping_ms,Network_Name\n")
                }
            } else {
                // 2. Gap Detection (Backfill)
                val lines = file.readLines()
                if (lines.size > 1) {
                    val lastLine = lines.last().split(",")
                    val lastDateStr = "${lastLine[0]} ${lastLine[1]}"
                    
                    val lastDate = fullFormat.parse(lastDateStr)
                    val currentDate = fullFormat.parse(fullFormat.format(now))

                    if (lastDate != null && currentDate != null) {
                        val diffInMillis = currentDate.time - lastDate.time
                        val gapHours = TimeUnit.MILLISECONDS.toHours(diffInMillis).toInt()

                        if (gapHours > 1) {
                            FileWriter(file, true).use { writer ->
                                for (i in 1 until gapHours) {
                                    val missingTime = Date(lastDate.time + (i * 3600000L))
                                    writer.append("${dateFormat.format(missingTime)},${timeFormat.format(missingTime)},$deviceName,0.0,0.0,0,OFFLINE_OR_TIMEOUT\n")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Run the Speed Test
            val engine = SpeedTestEngine()
            val downloadSpeed = engine.runDownloadTest()
            
            // Note: Since we are using a lightweight file download for the engine, 
            // we will log Upload and Ping as 0.0 to save battery, but keep the schema identical.
            val status = if (downloadSpeed > 0.0) "Active_Connection" else "OFFLINE_OR_TIMEOUT"

            // 4. Append the active result
            FileWriter(file, true).use { writer ->
                writer.append("${dateFormat.format(now)},${timeFormat.format(now)},$deviceName,$downloadSpeed,0.0,0,$status\n")
            }
            
            Log.d("NetTrace", "Logged: $downloadSpeed Mbps")
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}