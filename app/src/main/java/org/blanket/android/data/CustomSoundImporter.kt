package org.blanket.android.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

class CustomSoundImporter(private val resolver: ContentResolver) {
    fun read(uri: Uri): Sound {
        require(uri.scheme == "content") { "Choose an audio document from storage" }
        val mimeType = resolver.getType(uri)
        require(mimeType == null || mimeType.startsWith("audio/")) { "Choose an audio file" }
        resolver.openAssetFileDescriptor(uri, "r")?.use {} ?: error("The file could not be opened")
        val displayName =
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            } ?: "Custom Sound"
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val name =
            displayName
                .substringBeforeLast('.', displayName)
                .trim()
                .ifBlank { "Custom Sound" }
                .take(80)
        return Sound(java.util.UUID.randomUUID().toString(), name, uri.toString())
    }
}
