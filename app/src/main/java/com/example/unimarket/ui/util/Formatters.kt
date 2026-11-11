package com.example.unimarket.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val indonesianLocale: Locale = Locale.forLanguageTag("id-ID")

/** Jam pesan, contoh "14:05". */
fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", indonesianLocale).format(Date(timestamp))

/**
 * Waktu ringkas untuk daftar chat: jam jika hari ini, "Kemarin", atau tanggal ("5 Nov").
 */
fun formatChatTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    fun Calendar.sameDayAs(other: Calendar) =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        then.sameDayAs(today) -> formatTime(timestamp)
        then.sameDayAs(yesterday) -> "Kemarin"
        else -> SimpleDateFormat("d MMM", indonesianLocale).format(Date(timestamp))
    }
}

private val rupiahFormat: DecimalFormat by lazy {
    val symbols = DecimalFormatSymbols().apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    DecimalFormat("#,##0", symbols)
}

/** 85000 -> "Rp85.000" */
fun formatRupiah(amount: Long): String = "Rp" + rupiahFormat.format(amount)

/** Batas harga agar tidak overflow / input tidak masuk akal. */
const val MAX_PRICE = 1_000_000_000L

/**
 * Parse input harga dari form (hanya digit). Mengembalikan null jika kosong, bukan angka,
 * atau di luar rentang 1..MAX_PRICE.
 */
fun parsePrice(input: String): Long? {
    val digits = input.trim()
    if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
    val value = digits.toLongOrNull() ?: return null
    return value.takeIf { it in 1..MAX_PRICE }
}
