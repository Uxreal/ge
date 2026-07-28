package dev.lumen.launcher.core.data.system

import android.content.Context
import android.content.Intent
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Last-resort crash capture. A launcher failing silently is undebuggable in the field — "it does
 * not load" carries no stack trace. So the default uncaught-exception handler writes the trace to
 * a file before the process dies, and the next launch surfaces it with a share action.
 *
 * Deliberately primitive: no library, no network (the §0 rule), just a file.
 */
object CrashLog {

    private const val FILE = "crash-log.txt"
    private const val EXPORT_MARKER = "crash-export.marker"
    private const val MAX_CHARS = 200_000
    private const val EXPORT_THROTTLE_MS = 60_000L

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(appContext, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val entry = buildString {
            append("Lumen crash — thread ").append(thread.name).append('\n')
            append("version: ").append(versionOf(context)).append('\n')
            append("time: ").append(System.currentTimeMillis()).append('\n')
            append(trace)
            append('\n')
        }
        val file = File(context.filesDir, FILE)
        val existing = if (file.exists()) file.readText().take(MAX_CHARS / 2) else ""
        file.writeText((entry + existing).take(MAX_CHARS))
        // D44: a crash during composition means the in-app share card never gets a frame to
        // exist on, so the trace must reach somewhere the user can see without Lumen's help.
        runCatching { exportToDownloads(context, entry) }
    }

    /**
     * Drops the trace into the system Downloads collection as `lumen-crash-<epoch>.txt` — open
     * the Files app, it is right there, no working launcher required. Throttled so a crash loop
     * produces about one file a minute, not one per death. MediaStore needs no permission for
     * app-contributed Downloads on this minSdk.
     */
    private fun exportToDownloads(context: Context, entry: String) {
        val marker = File(context.filesDir, EXPORT_MARKER)
        val now = System.currentTimeMillis()
        if (marker.exists() && now - marker.lastModified() < EXPORT_THROTTLE_MS) return
        marker.writeText("")

        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "lumen-crash-${now / 1000}.txt")
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values,
        ) ?: return
        resolver.openOutputStream(uri)?.use { it.write(entry.toByteArray()) }
    }

    private fun versionOf(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")

    /** A guarded failure worth recording without dying: same file, marked non-fatal. */
    fun note(context: Context, where: String, throwable: Throwable) {
        runCatching {
            val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
            val entry = "Lumen non-fatal — $where\ntime: ${System.currentTimeMillis()}\n$trace\n"
            val file = File(context.filesDir, FILE)
            val existing = if (file.exists()) file.readText().take(MAX_CHARS / 2) else ""
            file.writeText((entry + existing).take(MAX_CHARS))
        }
    }

    fun read(context: Context): String? = runCatching {
        File(context.filesDir, FILE).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }

    /** Hands the log to any share target (Messages, email, Keep …) — no network of our own. */
    fun share(context: Context) {
        val text = read(context) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Lumen crash log")
            putExtra(Intent.EXTRA_TEXT, text.take(12_000))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share crash log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
