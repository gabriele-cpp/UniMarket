package com.example.unimarket.data.remote

import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SupabaseApi {

    @POST("auth/v1/signup")
    suspend fun signUp(@Body request: SupabaseSignUpRequest): SupabaseAuthResponse

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(@Body request: SupabaseLoginRequest): SupabaseAuthResponse

    @GET("rest/v1/profiles")
    suspend fun getProfile(@Query("id") idFilter: String): List<SupabaseProfileDto>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body body: SupabaseUpdateProfileRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<SupabaseProfileDto>

    @GET("rest/v1/products?order=id.desc")
    suspend fun getProducts(
        @Query("category") categoryFilter: String? = null
    ): List<SupabaseProductDto>

    @GET("rest/v1/products?order=id.desc")
    suspend fun searchProducts(
        @Query("or") orFilter: String
    ): List<SupabaseProductDto>

    @GET("rest/v1/products")
    suspend fun getProductById(
        @Query("id") idFilter: String
    ): List<SupabaseProductDto>

    @POST("rest/v1/products")
    suspend fun insertProduct(
        @Body product: SupabaseProductDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<SupabaseProductDto>

    @PATCH("rest/v1/products")
    suspend fun updateProduct(
        @Query("id") idFilter: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<SupabaseProductDto>

    @DELETE("rest/v1/products")
    suspend fun deleteProduct(
        @Query("id") idFilter: String
    )

    @POST("storage/v1/object/product-images/{path}")
    suspend fun uploadImage(
        @Path("path") path: String,
        @Body imageBytes: RequestBody,
        @Header("x-upsert") upsert: String = "true"
    ): SupabaseStorageUploadResponse

    companion object {
        fun create(
            baseUrl: String,
            apiKey: String,
            tokenProvider: () -> String?
        ): SupabaseApi {
            val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val authInterceptor = Interceptor { chain ->
                val request = chain.request()
                val userToken = tokenProvider()
                val currentToken = if (!userToken.isNullOrBlank()) userToken else apiKey
                val newRequest = request.newBuilder()
                    .header("apikey", apiKey)
                    .apply {
                        if (request.header("Authorization") == null) {
                            header("Authorization", "Bearer $currentToken")
                        }
                    }
                    .build()

                val response = chain.proceed(newRequest)

                // Jika token JWT user telah kedaluwarsa (HTTP 401) pada REST request,
                // ulangi otomatis dengan apiKey (anon/publishable key) agar pembacaan katalog tidak terblokir
                if (response.code == 401 && !userToken.isNullOrBlank() && !request.url.encodedPath.contains("auth/v1/")) {
                    response.close()
                    val retryRequest = request.newBuilder()
                        .header("apikey", apiKey)
                        .header("Authorization", "Bearer $apiKey")
                        .build()
                    return@Interceptor chain.proceed(retryRequest)
                }

                response
            }

            val client = OkHttpClient.Builder()
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseApi::class.java)
        }
    }
}

// ── DTOs ─────────────────────────────────────────────────────────────────

data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>
)

data class SupabaseLoginRequest(
    val email: String,
    val password: String
)

data class SupabaseAuthResponse(
    val access_token: String?,
    val refresh_token: String?,
    val user: SupabaseUser?
)

data class SupabaseUser(
    val id: String?,
    val email: String?,
    val user_metadata: Map<String, Any?>?
) {
    val fullName: String?
        get() = user_metadata?.get("full_name") as? String

    val phone: String?
        get() = user_metadata?.get("phone") as? String
}

data class SupabaseProfileDto(
    val id: String,
    val full_name: String?,
    val email: String?,
    val phone: String?,
    val created_at: String?
)

data class SupabaseUpdateProfileRequest(
    val full_name: String,
    val phone: String
)

data class SupabaseProductDto(
    val id: Long? = null,
    val seller_id: String? = null,
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val seller: String,
    val description: String,
    val location: String = "Kampus Utama",
    val delivery_method: String = "COD Kampus",
    val image_uri: String? = null,
    val mark: String = "UM",
    val color_hex: String = "#D7E7F5",
    val status: String? = "TERSEDIA",
    val created_at: String? = null
)

data class SupabaseStorageUploadResponse(
    val Key: String?,
    val Id: String?
)
