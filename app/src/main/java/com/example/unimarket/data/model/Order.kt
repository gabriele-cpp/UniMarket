package com.example.unimarket.data.model

/**
 * Status pesanan di UniMarket. Disimpan di Room sebagai String (lihat Converters).
 * Status Midtrans dipetakan ke enum ini lewat [mapMidtransStatus].
 */
enum class OrderStatus(val label: String) {
    PENDING("Menunggu Pembayaran"),
    PAID("Lunas"),
    GAGAL("Gagal"),
    KEDALUWARSA("Kedaluwarsa");

    /** PAID, GAGAL, dan KEDALUWARSA tidak akan berubah lagi, jadi tidak perlu dicek ke server. */
    val isFinal: Boolean get() = this != PENDING
}

/**
 * Model pesanan untuk lapisan UI, sudah digabung dengan data barang dan penjual.
 */
data class Order(
    val id: Long,
    /** order_id yang dikirim ke Midtrans, format "UM-<timestamp>-<productId>". */
    val orderId: String,
    val productId: Int,
    val productTitle: String,
    val productImageUri: String?,
    val sellerName: String,
    val buyerId: Long,
    /** Harga saat pesanan dibuat, dalam Rupiah. */
    val amount: Long,
    val quantity: Int = 1,
    val status: OrderStatus,
    /** Halaman pembayaran Midtrans Snap. null jika backend belum memberikan. */
    val redirectUrl: String?,
    val createdAt: Long
)

/**
 * URL tujuan Snap setelah pembayaran selesai (callbacks.finish di payment-server).
 * Halaman ini tidak benar-benar ada: WebView mencegatnya untuk menutup halaman pembayaran.
 */
const val PAYMENT_FINISH_URL = "https://unimarket.app/payment-finish"

fun isPaymentFinishUrl(url: String?): Boolean = url?.startsWith(PAYMENT_FINISH_URL) == true

/** Membuat order_id unik untuk Midtrans. Fungsi murni supaya mudah dites. */
fun generateOrderId(productId: Int, timestampMillis: Long): String = "UM-$timestampMillis-$productId"

/**
 * Memetakan transaction_status Midtrans ke [OrderStatus].
 * Fungsi murni (tanpa Android/Room/jaringan) supaya mudah diuji.
 *
 * @return null jika status tidak dikenal (mis. "authorize", "refund"),
 *         artinya status pesanan di perangkat tidak diubah.
 */
fun mapMidtransStatus(transactionStatus: String?): OrderStatus? =
    when (transactionStatus?.trim()?.lowercase()) {
        "settlement", "capture" -> OrderStatus.PAID
        "pending" -> OrderStatus.PENDING
        "deny", "cancel", "failure" -> OrderStatus.GAGAL
        "expire" -> OrderStatus.KEDALUWARSA
        else -> null
    }

/** Alasan barang tidak bisa dibeli, dalam bahasa Indonesia siap tampil. */
object PurchaseRules {
    const val OWN_PRODUCT = "Anda tidak bisa membeli barang milik sendiri"
    const val NOT_AVAILABLE = "Barang ini sudah tidak tersedia untuk dibeli"

    /**
     * @return null jika [buyerId] boleh membeli [product], selain itu pesan alasannya.
     * Hanya barang TERSEDIA milik orang lain yang bisa dibeli.
     */
    fun blockReason(product: Product, buyerId: Long): String? = when {
        product.sellerId == buyerId -> OWN_PRODUCT
        product.status != ProductStatus.TERSEDIA || product.stock <= 0 -> NOT_AVAILABLE
        else -> null
    }

    fun canBuy(product: Product, buyerId: Long?): Boolean =
        buyerId != null && blockReason(product, buyerId) == null
}
