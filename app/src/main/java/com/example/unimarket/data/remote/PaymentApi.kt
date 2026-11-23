package com.example.unimarket.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoint payment-server (folder payment-server/ di repo). Server Key Midtrans
 * hanya ada di server tersebut, tidak pernah di aplikasi.
 */
interface PaymentApi {

    @POST("payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): CreatePaymentResponse

    @GET("payments/{orderId}/status")
    suspend fun getStatus(@Path("orderId") orderId: String): PaymentStatusResponse

    companion object {
        /** [baseUrl] wajib diakhiri "/", contoh "http://10.0.2.2:3000/". */
        fun create(baseUrl: String): PaymentApi {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("bypass-tunnel-reminder", "true")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PaymentApi::class.java)
        }
    }
}

data class CreatePaymentRequest(
    val orderId: String,
    /** Rupiah, bilangan bulat > 0. */
    val amount: Long,
    val itemName: String,
    val customerName: String?,
    val customerEmail: String?
)

// Field respons dibuat nullable: Gson tidak memeriksa null-safety Kotlin,
// jadi respons yang tidak lengkap dicek manual di repository.

data class CreatePaymentResponse(
    val token: String?,
    val redirectUrl: String?
)

data class PaymentStatusResponse(
    val orderId: String?,
    /** transaction_status Midtrans: pending, settlement, capture, deny, cancel, expire, failure. */
    val transactionStatus: String?,
    val paymentType: String?
)
