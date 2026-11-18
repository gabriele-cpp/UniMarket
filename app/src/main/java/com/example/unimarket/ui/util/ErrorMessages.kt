package com.example.unimarket.ui.util

import android.util.Log
import kotlin.coroutines.cancellation.CancellationException

/** Pesan error standar (bahasa Indonesia) untuk ditampilkan ke user. */
object ErrorMessages {
    const val LOAD_FAILED = "Gagal memuat data. Silakan tutup dan buka kembali halaman ini."
    const val SAVE_FAILED = "Gagal menyimpan data. Silakan coba lagi."
    const val DELETE_FAILED = "Gagal menghapus barang. Silakan coba lagi."
    const val STATUS_FAILED = "Gagal mengubah status barang. Silakan coba lagi."
    const val IMAGE_FAILED = "Foto tidak bisa disimpan. Coba pilih foto lain."
    const val LOGIN_FAILED = "Gagal masuk. Silakan coba lagi."
    const val REGISTER_FAILED = "Gagal mendaftar. Silakan coba lagi."
    const val SEND_FAILED = "Pesan gagal dikirim. Silakan coba lagi."
    const val SESSION_EXPIRED = "Sesi berakhir. Silakan login kembali."

    private const val TAG = "UniMarket"

    /** Pencatat bawaan aplikasi: ke Logcat. */
    val logcatLogger: (Throwable) -> Unit = { Log.e(TAG, it.message, it) }

    /**
     * Pencatat yang dipakai [log]. Default ke Logcat; di unit test JVM diganti
     * (lihat testutil/ErrorLogRule) karena android.util.Log tidak tersedia di sana
     * ("Method e in android.util.Log not mocked").
     */
    @Volatile
    var logger: (Throwable) -> Unit = logcatLogger

    /** Catat detail teknis; user hanya melihat pesan yang ramah. */
    fun log(throwable: Throwable) {
        logger(throwable)
    }
}

/**
 * Dipanggil di awal blok catch (e: Exception) di dalam coroutine:
 * pembatalan coroutine (mis. layar ditutup) harus diteruskan, bukan dianggap error.
 */
fun Exception.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
