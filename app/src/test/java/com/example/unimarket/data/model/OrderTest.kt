package com.example.unimarket.data.model

import com.example.unimarket.testutil.product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fungsi murni pembayaran: pemetaan status Midtrans, order_id, URL finish, dan aturan beli.
 * Tidak butuh coroutine, Room, atau jaringan.
 */
class OrderTest {

    // ── mapMidtransStatus ────────────────────────────────────────────────────
    @Test
    fun `settlement dan capture berarti lunas`() {
        assertEquals(OrderStatus.PAID, mapMidtransStatus("settlement"))   // VA, QRIS, e-wallet
        assertEquals(OrderStatus.PAID, mapMidtransStatus("capture"))      // kartu kredit
    }

    @Test
    fun `pending tetap menunggu pembayaran`() {
        assertEquals(OrderStatus.PENDING, mapMidtransStatus("pending"))
    }

    @Test
    fun `deny cancel dan failure berarti gagal`() {
        assertEquals(OrderStatus.GAGAL, mapMidtransStatus("deny"))
        assertEquals(OrderStatus.GAGAL, mapMidtransStatus("cancel"))
        assertEquals(OrderStatus.GAGAL, mapMidtransStatus("failure"))
    }

    @Test
    fun `expire berarti kedaluwarsa`() {
        assertEquals(OrderStatus.KEDALUWARSA, mapMidtransStatus("expire"))
    }

    @Test
    fun `huruf besar dan spasi di sekitar status tetap dikenali`() {
        assertEquals(OrderStatus.PAID, mapMidtransStatus("  SETTLEMENT "))
        assertEquals(OrderStatus.KEDALUWARSA, mapMidtransStatus("Expire"))
    }

    @Test
    fun `status tidak dikenal atau kosong tidak mengubah status pesanan`() {
        assertNull(mapMidtransStatus("refund"))
        assertNull(mapMidtransStatus("authorize"))
        assertNull(mapMidtransStatus(""))
        assertNull(mapMidtransStatus(null))
    }

    @Test
    fun `hanya PENDING yang belum final`() {
        assertFalse(OrderStatus.PENDING.isFinal)
        assertTrue(OrderStatus.PAID.isFinal)
        assertTrue(OrderStatus.GAGAL.isFinal)
        assertTrue(OrderStatus.KEDALUWARSA.isFinal)
    }

    // ── generateOrderId & URL finish ─────────────────────────────────────────
    @Test
    fun `order id berformat UM-timestamp-productId`() {
        val orderId = generateOrderId(productId = 42, timestampMillis = 1_731_900_000_000L)

        assertEquals("UM-1731900000000-42", orderId)
        // Aturan payment-server/Midtrans: hanya huruf, angka, - _ ~ . dan maksimal 50 karakter
        assertTrue(orderId.matches(Regex("^[A-Za-z0-9\\-_~.]{1,50}$")))
    }

    @Test
    fun `url finish Midtrans dikenali beserta query string`() {
        assertTrue(isPaymentFinishUrl("https://unimarket.app/payment-finish"))
        assertTrue(isPaymentFinishUrl("https://unimarket.app/payment-finish?order_id=UM-1-2&transaction_status=settlement"))
    }

    @Test
    fun `url lain bukan url finish`() {
        assertFalse(isPaymentFinishUrl("https://app.sandbox.midtrans.com/snap/v4/redirection/abc"))
        assertFalse(isPaymentFinishUrl("http://unimarket.app/payment-finish"))
        assertFalse(isPaymentFinishUrl(null))
    }

    // ── PurchaseRules ────────────────────────────────────────────────────────
    @Test
    fun `barang tersedia milik orang lain boleh dibeli`() {
        val item = product(sellerId = 1L, status = ProductStatus.TERSEDIA)

        assertNull(PurchaseRules.blockReason(item, buyerId = 2L))
        assertTrue(PurchaseRules.canBuy(item, buyerId = 2L))
    }

    @Test
    fun `barang milik sendiri atau tidak tersedia tidak boleh dibeli`() {
        val milikSendiri = product(sellerId = 2L)
        val terjual = product(sellerId = 1L, status = ProductStatus.TERJUAL)
        val negosiasi = product(sellerId = 1L, status = ProductStatus.NEGOSIASI)

        assertEquals(PurchaseRules.OWN_PRODUCT, PurchaseRules.blockReason(milikSendiri, buyerId = 2L))
        assertEquals(PurchaseRules.NOT_AVAILABLE, PurchaseRules.blockReason(terjual, buyerId = 2L))
        assertEquals(PurchaseRules.NOT_AVAILABLE, PurchaseRules.blockReason(negosiasi, buyerId = 2L))
        assertFalse(PurchaseRules.canBuy(product(sellerId = 1L), buyerId = null))   // belum login
    }
}
