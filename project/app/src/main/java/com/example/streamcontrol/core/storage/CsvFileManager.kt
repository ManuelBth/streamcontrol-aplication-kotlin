package com.example.streamcontrol.core.storage

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.example.streamcontrol.domain.model.ProcessSample
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvFileManager(
    private val context: Context
) {
    private val directoryName = "StreamControl"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun saveToCsv(fileName: String, samples: List<ProcessSample>): Result<String> {
        return try {
            val csvContent = buildString {
                appendLine("tiempo_ms,temperatura,angulo_disparo,pwm_ventiladores")
                samples.forEach { sample ->
                    appendLine("${sample.elapsedTimeMs.toLong()},${sample.temperature},${sample.firingAngle},${sample.fanPwm}")
                }
            }

            val sanitizedFileName = fileName.takeIf { it.isNotBlank() } ?: "control_data"
            val finalFileName = if (sanitizedFileName.endsWith(".csv")) sanitizedFileName else "$sanitizedFileName.csv"

            val result = saveToDocuments(finalFileName, csvContent)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToDocuments(fileName: String, content: String): String {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(documentsDir, directoryName)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val file = File(appDir, fileName)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(content.toByteArray())
        }

        return file.absolutePath
    }

    fun getExistingFiles(): List<FileInfo> {
        return try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appDir = File(documentsDir, directoryName)
            if (!appDir.exists()) {
                return emptyList()
            }

            appDir.listFiles()
                ?.filter { it.extension == "csv" }
                ?.map { file ->
                    FileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        lastModified = Date(file.lastModified())
                    )
                }
                ?.sortedByDescending { it.lastModified }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteFile(path: String): Result<Unit> {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class FileInfo(
        val name: String,
        val path: String,
        val lastModified: Date
    )
}