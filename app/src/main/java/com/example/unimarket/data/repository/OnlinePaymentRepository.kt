package com.example.unimarket.data.repository

import com.example.unimarket.data.local.OrderDao
import com.example.unimarket.data.local.OrderEntity
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.UserDao
import com.example.unimarket.data.model.Order
import com.example.unimarket.data.model.OrderStatus
import com.example.unimarket.data.model.PurchaseRules
import com.example.unimarket.data.model.generateOrderId
import com.example.unimarket.data.model.mapMidtransStatus
import com.example.unimarket.data.remote.CreatePaymentRequest
import com.example.unimarket.data.remote.PaymentApi
import com.google.gson.JsonParseException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class OnlinePaymentRepository(
    private val orderDao: OrderDao,
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val api: PaymentApi,
    private val clock: () -> Long = System::currentTimeMillis
) : PaymentRepository {

    override suspend fun createOrder(productId: Int, buyerId: Long): PaymentResult<Order> {
        val product = productDao.getProductById(productId)?.toProduct()
            ?: return PaymentResult.Error(PaymentErrors.PRODUCT_NOT_FOUND)
        PurchaseRules.blockReason(product, buyerId)?.let { return PaymentResult.Error(it) }

        val buyer = userDao.getUserById(buyerId)
        val now = clock()
        val orderId = generateOrderId(productId, now)
        orderDao.insert(
            OrderEntity(
                orderId = orderId,
                productId = productId,
                buyerId = buyerId,
                amount = product.price,
                status = OrderStatus.PENDING,
                createdAt = now
            )
        )

        val redirectUrl = try {
            api.createPayment(
                CreatePaymentRequest(
                    orderId = orderId,
                    amount = product.price,
                    itemName = product.title,
                    customerName = buyer?.name,
                    customerEmail = buyer?.email
                )
            ).redirectUrl
        } catch (e: Exception) {
            // Pesanan tanpa halaman bayar tidak berguna: hapus, juga saat layar ditutup
            // (coroutine dibatalkan). NonCancellable agar penghapusan tetap berjalan.
            withContext(NonCancellable) { orderDao.deleteByOrderId(orderId) }
            if (e is CancellationException) throw e
            return e.toPaymentError()
        }

        if (redirectUrl.isNullOrBlank()) {
            orderDao.deleteByOrderId(orderId)
            return PaymentResult.Error(PaymentErrors.INVALID_RESPONSE)
        }
        orderDao.updateRedirectUrl(orderId, redirectUrl)
        return loadOrder(orderId)
    }

    override suspend fun refreshStatus(orderId: String): PaymentResult<Order> {
        val order = orderDao.getByOrderId(orderId)
            ?: return PaymentResult.Error(PaymentErrors.ORDER_NOT_FOUND)
        if (order.status.isFinal) return loadOrder(orderId)

        val response = try {
            api.getStatus(orderId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return e.toPaymentError()
        }

        // Status tidak dikenal (null) = biarkan status di perangkat apa adanya.
        when (val newStatus = mapMidtransStatus(response.transactionStatus) ?: order.status) {
            order.status -> Unit
            OrderStatus.PAID -> orderDao.markPaid(orderId, order.productId)
            else -> orderDao.updateStatus(orderId, newStatus)
        }
        return loadOrder(orderId)
    }

    override fun observeOrder(orderId: String): Flow<Order?> =
        orderDao.observeWithProduct(orderId).map { it?.toOrder() }

    override fun observeOrdersByBuyer(buyerId: Long): Flow<List<Order>> =
        orderDao.observeByBuyer(buyerId).map { list -> list.map { it.toOrder() } }

    private suspend fun loadOrder(orderId: String): PaymentResult<Order> =
        orderDao.getWithProduct(orderId)?.let { PaymentResult.Success(it.toOrder()) }
            ?: PaymentResult.Error(PaymentErrors.ORDER_NOT_FOUND)

    private fun Exception.toPaymentError(): PaymentResult.Error {
        val message = when (this) {
            is IOException -> PaymentErrors.NETWORK          // server mati, salah alamat, timeout
            is HttpException -> if (code() >= 500) PaymentErrors.SERVER else PaymentErrors.REJECTED
            is JsonParseException -> PaymentErrors.INVALID_RESPONSE
            else -> PaymentErrors.UNKNOWN
        }
        return PaymentResult.Error(message, cause = this)
    }
}
