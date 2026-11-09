package org.blanket.android

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import org.blanket.android.data.CustomSoundImporter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

class AudioDocumentProvider : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri) =
        if (uri.lastPathSegment == "text") "text/plain" else "audio/ogg"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor =
        MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf("Ocean.ogg")) }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (uri.lastPathSegment == "missing") throw java.io.FileNotFoundException("Gone")
        val file =
            File(context!!.cacheDir, "sample.ogg").apply {
                writeBytes(context!!.assets.open("sounds/rain.ogg").use { it.readBytes() })
            }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CustomSoundImporterTest {
    private fun importer(): CustomSoundImporter {
        val context = RuntimeEnvironment.getApplication()
        val provider = AudioDocumentProvider()
        provider.attachInfo(
            context,
            android.content.pm.ProviderInfo().apply { authority = "blanket.test.documents" },
        )
        ShadowContentResolver.registerProviderInternal("blanket.test.documents", provider)
        return CustomSoundImporter(context.contentResolver)
    }

    @Test
    fun audioDocumentGetsDisplayNameAndStableUri() {
        val sound = importer().read(Uri.parse("content://blanket.test.documents/audio"))
        assertEquals("Ocean", sound.name)
        assertEquals("content://blanket.test.documents/audio", sound.uri)
        assertTrue(sound.id.isNotBlank())
        assertFalse(sound.builtIn)
    }

    @Test
    fun nonAudioIsRejected() {
        try {
            importer().read(Uri.parse("content://blanket.test.documents/text"))
            fail("Expected invalid audio")
        } catch (error: IllegalArgumentException) {
            assertEquals("Choose an audio file", error.message)
        }
    }

    @Test
    fun unavailableFileIsReported() {
        try {
            importer().read(Uri.parse("content://blanket.test.documents/missing"))
            fail("Expected unavailable file")
        } catch (error: java.io.FileNotFoundException) {
            assertEquals("Gone", error.message)
        }
    }
}
