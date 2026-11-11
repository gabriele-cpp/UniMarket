package com.example.unimarket.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Penyimpanan foto barang. URI dari photo picker hanya berlaku sementara, jadi foto
 * disalin ke penyimpanan internal aplikasi dan path file-nya yang disimpan di database.
 */
interface ImageStorage {
    /**
     * Menyalin foto dari [sourceUri] (content://...) ke penyimpanan internal.
     * @return path absolut file hasil salinan.
     * @throws IOException jika foto tidak bisa dibaca/ditulis.
     */
    suspend fun importImage(sourceUri: String): String

    /** Menghapus file foto jika file itu milik penyimpanan ini; path lain diabaikan. */
    suspend fun deleteImage(path: String?)

    /** true jika [path] sudah berupa file di penyimpanan internal ini. */
    fun isStoredImage(path: String?): Boolean
}

/** Menyimpan foto di filesDir/images/<uuid>.jpg. Semua operasi file berjalan di Dispatchers.IO. */
class InternalImageStorage(private val context: Context) : ImageStorage {

    private val imagesDir: File by lazy { File(context.filesDir, "images") }

    override suspend fun importImage(sourceUri: String): String = withContext(Dispatchers.IO) {
        if (!imagesDir.exists() && !imagesDir.mkdirs()) {
            throw IOException("Folder foto tidak bisa dibuat")
        }
        val target = File(imagesDir, "${UUID.randomUUID()}.jpg")
        try {
            val input = context.contentResolver.openInputStream(Uri.parse(sourceUri))
                ?: throw IOException("Foto tidak bisa dibaca")
            input.use { source ->
                target.outputStream().use { output -> source.copyTo(output) }
            }
            target.absolutePath
        } catch (e: Exception) {
            target.delete() // jangan tinggalkan file setengah jadi
            throw if (e is IOException) e else IOException("Gagal menyalin foto", e)
        }
    }

    override suspend fun deleteImage(path: String?) {
        if (!isStoredImage(path)) return
        withContext(Dispatchers.IO) {
            File(path!!).delete()
        }
    }

    override fun isStoredImage(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        // Hanya file di dalam folder images kita (mencegah menghapus file lain).
        val file = File(path)
        return file.isAbsolute &&
            file.canonicalPath.startsWith(imagesDir.canonicalPath + File.separator)
    }
}
