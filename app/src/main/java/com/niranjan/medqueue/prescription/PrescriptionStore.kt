package com.niranjan.medqueue.prescription

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Owns prescription photos on disk.
 *
 * Photos live in internal storage (`files/prescriptions/`), not in the
 * database and not in a shared media folder: a prescription is customer
 * health information, so it should not land in the device gallery where it
 * would sync to cloud backups or show up in other apps' pickers.
 *
 * Camera captures are staged in `cache/camera/` — a camera app needs a URI it
 * can write to before the photo exists — and only promoted into the permanent
 * directory once the capture succeeds.
 */
object PrescriptionStore {

    private const val TAG = "PrescriptionStore"
    private const val PERMANENT_DIR = "prescriptions"
    private const val CAMERA_DIR    = "camera"

    // ── Camera staging ───────────────────────────────────────────────────────

    /**
     * Creates an empty staging file and returns a FileProvider URI the camera
     * app may write into. Pair with [promoteCapture] once the capture returns.
     */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val dir = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    /**
     * Moves a successful capture out of the cache into permanent storage.
     * Returns the stored path, or null if the capture file never materialised.
     */
    fun promoteCapture(context: Context, staged: File): String? {
        if (!staged.exists() || staged.length() == 0L) {
            staged.delete()
            return null
        }
        return try {
            val target = newPermanentFile(context)
            staged.copyTo(target, overwrite = true)
            staged.delete()
            target.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to promote camera capture", e)
            null
        }
    }

    // ── Gallery import ───────────────────────────────────────────────────────

    /**
     * Copies a picked image into internal storage and returns the stored path.
     *
     * The copy is the point: the picker hands back a URI whose read grant dies
     * with the activity, so holding onto it would leave the detail screen
     * unable to render the photo on a later launch.
     */
    fun importFromUri(context: Context, uri: Uri): String? = try {
        val target = newPermanentFile(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        if (target.length() > 0L) target.absolutePath else { target.delete(); null }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to import prescription from $uri", e)
        null
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /** Deletes a stored photo. Safe to call with null or an already-gone path. */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
            .onFailure { Log.w(TAG, "Could not delete prescription at $path", it) }
    }

    /** True when the path still points at a readable file. */
    fun exists(path: String?): Boolean =
        !path.isNullOrBlank() && File(path).let { it.exists() && it.length() > 0L }

    private fun newPermanentFile(context: Context): File {
        val dir = File(context.filesDir, PERMANENT_DIR).apply { mkdirs() }
        return File(dir, "rx_${System.currentTimeMillis()}.jpg")
    }
}
