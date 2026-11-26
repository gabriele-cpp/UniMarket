package com.example.unimarket.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** Kejadian dari channel realtime pesan satu barang. */
sealed interface RealtimeMessageEvent {
    /** Channel siap menerima perubahan (juga dikirim ulang setiap kali tersambung kembali). */
    data object Subscribed : RealtimeMessageEvent

    /** Ada baris baru di tabel `messages` untuk barang yang didengarkan. */
    data class Inserted(val message: SupabaseMessageDto) : RealtimeMessageEvent
}

/**
 * Klien kecil Supabase Realtime (protokol Phoenix di atas WebSocket) khusus untuk
 * mendengarkan INSERT di tabel `messages`. Memakai OkHttp yang sudah ada, tanpa SDK tambahan.
 *
 * Supaya kuota Supabase gratis tetap hemat, koneksi hanya dibuka selama Flow dikumpulkan
 * (yaitu selama layar chat terlihat), difilter per barang, dan hanya untuk event INSERT.
 */
class SupabaseRealtime(
    baseUrl: String,
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val gson = Gson()
    private val socketUrl: String =
        baseUrl.trimEnd('/')
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
            "/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"

    fun messageInserts(productId: Int): Flow<RealtimeMessageEvent> = callbackFlow {
        val topic = "realtime:chat-$productId"
        val ref = AtomicInteger(1)

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(joinMessage(topic, productId))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return
                if (json.string("topic") != topic) return
                val payload = json.getAsJsonObject("payload")
                when (json.string("event")) {
                    "phx_reply" -> if (json.string("ref") == JOIN_REF && payload?.string("status") != "ok") {
                        close(IOException("Gagal bergabung ke channel realtime: $text"))
                    }
                    "system" -> if (payload?.string("extension") == "postgres_changes") {
                        if (payload.string("status") == "ok") {
                            trySend(RealtimeMessageEvent.Subscribed)
                        } else {
                            close(IOException("Realtime menolak langganan: ${payload.string("message")}"))
                        }
                    }
                    "postgres_changes" -> {
                        val record = payload?.getAsJsonObject("data")?.getAsJsonObject("record") ?: return
                        runCatching { gson.fromJson(record, SupabaseMessageDto::class.java) }
                            .getOrNull()
                            ?.let { trySend(RealtimeMessageEvent.Inserted(it)) }
                    }
                    "phx_error", "phx_close" -> close(IOException("Channel realtime ditutup server"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close(IOException("Koneksi realtime tertutup ($code)"))
            }
        }

        val socket = client.newWebSocket(Request.Builder().url(socketUrl).build(), listener)

        // Server memutus koneksi yang tidak mengirim heartbeat dalam ~60 detik.
        val heartbeat = launch {
            while (isActive) {
                delay(HEARTBEAT_MS)
                socket.send("""{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"${ref.incrementAndGet()}"}""")
            }
        }

        awaitClose {
            heartbeat.cancel()
            socket.close(NORMAL_CLOSURE, null)
        }
    }.retryWhen { cause, attempt ->
        if (cause is CancellationException) return@retryWhen false
        // Sinyal hilang / server menutup koneksi: sambung lagi dengan jeda yang makin panjang.
        delay(minOf(MAX_RETRY_DELAY_MS, BASE_RETRY_DELAY_MS shl attempt.coerceAtMost(4).toInt()))
        true
    }

    private fun joinMessage(topic: String, productId: Int): String = """
        {"topic":"$topic","event":"phx_join","ref":"$JOIN_REF","join_ref":"$JOIN_REF",
         "payload":{"config":{"broadcast":{"self":false},"presence":{"key":""},"private":false,
           "postgres_changes":[{"event":"INSERT","schema":"public","table":"messages","filter":"product_id=eq.$productId"}]}}}
    """.trimIndent()

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private companion object {
        const val JOIN_REF = "1"
        const val HEARTBEAT_MS = 25_000L
        const val BASE_RETRY_DELAY_MS = 2_000L
        const val MAX_RETRY_DELAY_MS = 30_000L
        const val NORMAL_CLOSURE = 1000
    }
}
