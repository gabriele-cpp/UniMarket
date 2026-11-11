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

    /** Catat detail teknis ke Logcat; user hanya melihat pesan yang ramah. */
    fun log(throwable: Throwable) {
        Log.e(TAG, throwable.message, throwable)
    }
}

/**
 * Dipanggil di awal blok catch (e: Exception) di dalam coroutine:
 * pembatalan coroutine (mis. layar ditutup) harus diteruskan, bukan dianggap error.
 */
fun Exception.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
