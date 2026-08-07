package dev.rtrcompanion.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.rtrcompanion.protocol.PacketLogger
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves the current packet log to a file and returns a share [Intent].
 *
 * The file is written to the app's internal cache directory and shared
 * via a FileProvider URI — no WRITE_EXTERNAL_STORAGE permission required.
 *
 * Usage:
 * ```kotlin
 * val intent = PacketExporter.createShareIntent(context, packetLogger)
 * if (intent != null) context.startActivity(Intent.createChooser(intent, "Share capture"))
 * ```
 */
object PacketExporter {

    private val filenameFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Exports the current log to a `.txt` file in the app cache and returns a
     * share-sheet [Intent] pointing to that file.
     *
     * Returns `null` if the log is empty or if an I/O error occurs.
     */
    fun createShareIntent(context: Context, logger: PacketLogger): Intent? {
        val exportText = logger.export()
        if (exportText.isBlank()) {
            Timber.w("PacketExporter: nothing to export — log is empty")
            return null
        }

        return try {
            val filename = "rtr_capture_${filenameFmt.format(Date())}.txt"
            val cacheDir = File(context.cacheDir, "captures").also { it.mkdirs() }
            val file = File(cacheDir, filename)
            file.writeText(exportText, Charsets.UTF_8)

            Timber.i("PacketExporter: wrote %d bytes to %s", file.length(), filename)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "RTR Companion Packet Capture — $filename")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Timber.e(e, "PacketExporter: failed to create share intent")
            null
        }
    }
}
