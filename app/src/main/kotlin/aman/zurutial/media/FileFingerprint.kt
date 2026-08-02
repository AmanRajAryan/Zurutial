package aman.zurutial.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class PickedFile(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val customFingerprint: String? = null
) {
    val fingerprint: String
        get() = customFingerprint ?: "${sizeBytes}_${durationMs}"
}

object FileFingerprint {

    /**
     * Call after the user picks a file via ACTION_OPEN_DOCUMENT.
     * Persists read permission across app restarts so the file stays accessible.
     */
    fun inspect(context: Context, uri: Uri): PickedFile? {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val docFile = DocumentFile.fromSingleUri(context, uri) ?: return null
            val sizeBytes = docFile.length()
            val fileName = docFile.name ?: "unknown"

            val retriever = MediaMetadataRetriever()
            val durationMs = try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }

            if (sizeBytes <= 0L || durationMs <= 0L) return null

            PickedFile(
                uri = uri,
                fileName = fileName,
                sizeBytes = sizeBytes,
                durationMs = durationMs
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Compares a locally-picked file's fingerprint against the one stored in the room. */
    fun matches(picked: PickedFile, roomFileHash: String): Boolean {
        return picked.fingerprint == roomFileHash
    }
}
