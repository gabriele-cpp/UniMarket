package com.example.unimarket.data.repository

import com.example.unimarket.data.model.Order
import kotlinx.coroutines.flow.Flow

/**
 * Pembelian barang dan pembayaran lewat Midtrans Snap (melalui payment-server).
 * Implementasi utama: OnlinePaymentRepository (Room + Retrofit).
 * Untuk testing dibuat FakePaymentRepository yang mengimplementasikan interface ini.
 */
interface PaymentRepository {
    /**
     * Membuat pesanan PENDING di Room, lalu meminta halaman pembayaran ke payment-server.
     * Jika backend gagal, pesanan dihapus lagi agar tidak muncul di riwayat.
     * Sukses = [Order] dengan redirectUrl terisi.
     */
    suspend fun createOrder(productId: Int, buyerId: Long): PaymentResult<Order>

    /**
     * Menanyakan status terbaru ke payment-server lalu menyimpannya di Room.
     * Jika menjadi PAID, barang otomatis berstatus TERJUAL.
     * Pesanan yang statusnya sudah final tidak dicek ulang ke server.
     */
    suspend fun refreshStatus(orderId: String): PaymentResult<Order>

    fun observeOrder(orderId: String): Flow<Order?>

    /** Riwayat pembelian [buyerId], terbaru dulu. */
    fun observeOrdersByBuyer(buyerId: Long): Flow<List<Order>>
}

/** Hasil operasi pembayaran. Pesan error sudah dalam bahasa Indonesia, siap tampil di UI. */
sealed interface PaymentResult<out T> {
    data class Success<T>(val data: T) : PaymentResult<T>

    /** [cause] = detail teknis untuk log, tidak ditampilkan ke user. */
    data class Error(val message: String, val cause: Throwable? = null) : PaymentResult<Nothing>
}

object PaymentErrors {
    const val NETWORK = "Gagal terhubung ke server pembayaran. Pastikan payment-server berjalan."
    const val SERVER = "Server pembayaran sedang bermasalah. Silakan coba lagi."
    const val REJECTED = "Permintaan pembayaran ditolak server. Silakan coba lagi."
    const val INVALID_RESPONSE = "Respons server pembayaran tidak valid."
    const val PRODUCT_NOT_FOUND = "Barang tidak ditemukan"
    const val ORDER_NOT_FOUND = "Pesanan tidak ditemukan"
    const val UNKNOWN = "Terjadi kesalahan, silakan coba lagi"
}
