package com.lamecoders.nettrace

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class SpeedTestEngine {

    // We use Cloudflare's dedicated speed test endpoint. 
    // This URL requests exactly 50,000,000 bytes (50MB) of dummy data.
    private val testUrl = "https://speed.cloudflare.com/__down?bytes=50000000"

    fun runDownloadTest(): Double {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null

        try {
            val url = URL(testUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000 // 10 seconds to connect
            connection.readTimeout = 30000    // 30 seconds to finish reading

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return 0.0 // Connection failed
            }

            inputStream = connection.inputStream
            val buffer = ByteArray(8192) // Read in 8KB chunks
            var totalBytesRead = 0L
            var bytesRead: Int

            // Start the timer
            val startTime = System.currentTimeMillis()

            // Stream the data and immediately discard it
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
            }

            // Stop the timer
            val endTime = System.currentTimeMillis()
            val durationInSeconds = (endTime - startTime) / 1000.0

            if (durationInSeconds == 0.0) return 0.0

            // Calculate Speed: (Bytes -> Megabits) / Seconds
            val megabits = (totalBytesRead * 8) / 1_000_000.0
            val mbps = megabits / durationInSeconds

            // Return rounded to 2 decimal places
            return (mbps * 100.0).roundToInt() / 100.0

        } catch (e: Exception) {
            e.printStackTrace()
            return 0.0
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
    }
}