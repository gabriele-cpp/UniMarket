package com.example.unimarket.ui.util

import java.net.URLEncoder

/*
 * Fungsi murni (tanpa dependensi Android) untuk nomor HP & link WhatsApp,
 * sehingga mudah diuji dengan unit test JVM (lihat PhoneUtilsTest).
 */

/**
 * Menyeragamkan nomor HP ke format internasional Indonesia tanpa "+".
 * "0812-3456-7890" -> "6281234567890", "+62 812 3456 7890" -> "6281234567890",
 * "6281234567890" tetap.
 */
fun normalizePhone(value: String): String {
    val digits = value.filter { it.isDigit() }
    return if (digits.startsWith("0")) "62" + digits.drop(1) else digits
}

/** Menampilkan nomor tersimpan (62xxx) dalam format lokal 08xxx. */
fun phoneForDisplay(stored: String): String =
    if (stored.startsWith("62")) "0" + stored.drop(2) else stored

/** Pesan pembuka WhatsApp untuk sebuah barang. */
fun whatsAppGreeting(productTitle: String): String =
    "Halo, saya tertarik dengan $productTitle di UniMarket."

/**
 * Membuat URL https://wa.me/<nomor>?text=<pesan ter-encode>.
 * Spasi di-encode sebagai %20 (bukan "+") agar tampil benar di WhatsApp.
 */
fun buildWhatsAppUrl(phone: String, message: String): String {
    val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
    return "https://wa.me/${normalizePhone(phone)}?text=$encoded"
}
