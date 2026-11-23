package com.example.unimarket.testutil

import com.example.unimarket.data.model.Order
import com.example.unimarket.data.model.OrderStatus
import com.example.unimarket.data.model.generateOrderId
import com.example.unimarket.data.model.mapMidtransStatus
import com.example.unimarket.data.repository.PaymentErrors
import com.example.unimarket.data.repository.PaymentRepository
import com.example.unimarket.data.repository.PaymentResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake untuk [PaymentRepository] (asli: OnlinePaymentRepository = Room + Retrofit ke payment-server).
 *
 * Pesanan disimpan di MutableStateFlow dalam memori; data barang diambil dari
 * [products] agar judul dan harga pesanan realistis. Tidak ada jaringan sama sekali.
 *
 * Saklar:
 *  - [serverError]      : backend "gagal", createOrder/refreshStatus mengembalikan PaymentResult.Error
 *                         dengan pesan ini (mis. PaymentErrors.NETWORK = server mati)
 *  - [unexpectedError]  : createOrder/refreshStatus MELEMPAR exception (bug tak terduga)
 *  - [delayMillis]      : operasi lambat, untuk menguji state loading (jam virtual runTest)
 *  - [midtransStatus]   : transaction_status yang "dikembalikan Midtrans" saat refreshStatus
 */
class FakePaymentRepository(
    private val products: FakeProductRepository = FakeProductRepository(),
    private val clock: () -> Long = { 1_700_000_000_000L },
) : PaymentRepository {

    private val orders = MutableStateFlow<List<Order>>(emptyList())

    var serverError: String? = null
    var unexpectedError: Exception? = null
    var delayMillis: Long = 0
    var midtransStatus: String = "pending"

    /** Isi "database" pesanan saat ini, untuk assertion state-based. */
    val current: List<Order> get() = orders.value

    /** Setiap panggilan createOrder (productId, buyerId), termasuk yang gagal. */
    val createCalls = mutableListOf<Pair<Int, Long>>()

    override suspend fun createOrder(productId: Int, buyerId: Long, quantity: Int): PaymentResult<Order> {
        createCalls += productId to buyerId
        simulateNetwork()?.let { return it }

        val product = products.getProductById(productId)
            ?: return PaymentResult.Error(PaymentErrors.PRODUCT_NOT_FOUND)
        val orderId = generateOrderId(productId, clock())
        val order = Order(
            id = orders.value.size + 1L,
            orderId = orderId,
            productId = productId,
            productTitle = product.title,
            productImageUri = product.imageUri,
            sellerName = product.sellerName,
            buyerId = buyerId,
            amount = product.price * quantity,
            quantity = quantity,
            status = OrderStatus.PENDING,
            redirectUrl = "https://app.sandbox.midtrans.com/snap/v4/redirection/token-$orderId",
            createdAt = clock(),
        )
        orders.update { it + order }
        return PaymentResult.Success(order)
    }

    override suspend fun refreshStatus(orderId: String): PaymentResult<Order> {
        simulateNetwork()?.let { return it }
        val existing = orders.value.firstOrNull { it.orderId == orderId }
            ?: return PaymentResult.Error(PaymentErrors.ORDER_NOT_FOUND)
        val updated = existing.copy(status = mapMidtransStatus(midtransStatus) ?: existing.status)
        orders.update { list -> list.map { if (it.orderId == orderId) updated else it } }
        return PaymentResult.Success(updated)
    }

    override fun observeOrder(orderId: String): Flow<Order?> =
        orders.map { list -> list.firstOrNull { it.orderId == orderId } }

    override fun observeOrdersByBuyer(buyerId: Long): Flow<List<Order>> =
        orders.map { list -> list.filter { it.buyerId == buyerId }.sortedByDescending { it.createdAt } }

    /** @return Error jika saklar [serverError] aktif; melempar jika [unexpectedError] aktif. */
    private suspend fun simulateNetwork(): PaymentResult.Error? {
        if (delayMillis > 0) delay(delayMillis)
        unexpectedError?.let { throw it }
        return serverError?.let { PaymentResult.Error(it) }
    }
}
