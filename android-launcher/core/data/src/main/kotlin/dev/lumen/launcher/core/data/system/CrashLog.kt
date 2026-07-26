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
    private const val MAX_CHARS = 200_000

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
            append("time: ").append(System.currentTimeMillis()).append('\n')
            append(trace)
            append('\n')
        }
        val file = File(context.filesDir, FILE)
        val existing = if (file.exists()) file.readText().take(MAX_CHARS / 2) else ""
        file.writeText((entry + existing).take(MAX_CHARS))
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
