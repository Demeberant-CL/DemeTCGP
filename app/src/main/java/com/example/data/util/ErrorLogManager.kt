package com.example.data.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorLogManager {

  private const val FILE_NAME = "error_logs.txt"
  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

  fun init(context: Context) {
    val appContext = context.applicationContext
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        logFatalCrashSync(appContext, thread, throwable)
      } catch (e: Exception) {
        Log.e("ErrorLogManager", "Error al guardar crash fatal de forma síncrona", e)
      }
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }

  private fun logFatalCrashSync(context: Context, thread: Thread, throwable: Throwable) {
    val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
    val deviceModel = Build.MODEL
    val manufacturer = Build.MANUFACTURER
    val sdkInt = Build.VERSION.SDK_INT
    val stackTrace = throwable.stackTraceToString()

    val logBuilder = StringBuilder().apply {
      append("=== FATAL CRASH ===\n")
      append("Fecha/Hora: $timestamp\n")
      append("Dispositivo: $manufacturer $deviceModel (Android SDK $sdkInt)\n")
      append("Hilo: ${thread.name}\n")
      append("Mensaje: ${throwable.localizedMessage}\n")
      append("Stacktrace:\n$stackTrace\n")
      append("====================\n\n")
    }

    writeLogSync(context, logBuilder.toString())
  }

  fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
    logError(context, tag, message, throwable)
  }

  fun logError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
    val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
    val deviceModel = Build.MODEL
    val manufacturer = Build.MANUFACTURER
    val sdkInt = Build.VERSION.SDK_INT
    val stacktrace = throwable?.stackTraceToString()?.let { "\n$it" } ?: ""
    val logEntry = "[$timestamp] [$tag] Device: $manufacturer $deviceModel (API $sdkInt) - $message$stacktrace\n"

    try {
      Log.e("TcgPocket-$tag", message, throwable)
      writeLogSync(context, logEntry)
    } catch (e: Exception) {
      Log.e("ErrorLogManager", "Fallo al escribir en log de errores", e)
    }
  }

  /**
   * Escritura síncrona con truncado automático a un máximo de 200 líneas (mantiene las más recientes).
   */
  private fun writeLogSync(context: Context, entry: String) {
    try {
      val file = File(context.filesDir, FILE_NAME)
      val existingLines = if (file.exists()) {
        try {
          file.readLines().toMutableList()
        } catch (_: Exception) {
          mutableListOf()
        }
      } else {
        mutableListOf()
      }

      existingLines.add(entry.trimEnd('\n'))

      // Truncar si supera las 200 líneas (mantener las últimas 200)
      val maxLines = 200
      val linesToWrite = if (existingLines.size > maxLines) {
        existingLines.takeLast(maxLines)
      } else {
        existingLines
      }

      FileWriter(file, false).use { writer ->
        for (line in linesToWrite) {
          writer.append(line).append("\n")
        }
        writer.flush()
      }
    } catch (e: Exception) {
      Log.e("ErrorLogManager", "Fallo en escritura síncrona de log", e)
    }
  }

  suspend fun readLogs(context: Context): String = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, FILE_NAME)
    if (file.exists()) {
      try {
        file.readText()
      } catch (e: Exception) {
        "Error al leer archivo de logs: ${e.localizedMessage}"
      }
    } else {
      "No hay registros de error almacenados."
    }
  }

  suspend fun clearLogs(context: Context): Boolean = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, FILE_NAME)
    if (file.exists()) {
      file.delete()
    } else {
      true
    }
  }

  fun exportErrorLogs(context: Context) {
    try {
      val file = File(context.filesDir, FILE_NAME)
      if (!file.exists()) {
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val header = "[$timestamp] [INFO] Inicialización de log de errores Demeberant TCG Pocket (Device: ${Build.MANUFACTURER} ${Build.MODEL}, SDK ${Build.VERSION.SDK_INT}).\n"
        FileWriter(file, true).use { it.append(header) }
      }

      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Reporte de Errores - Demeberant TCG Pocket")
        putExtra(Intent.EXTRA_TEXT, "Adjunto el registro de errores de la aplicación Demeberant TCG Pocket.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(shareIntent, "Exportar Log de Errores")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      logError(context, "EXPORT_LOGS", "Fallo al exportar log de errores", e)
    }
  }
}
