package com.example.unimarket.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.delay

/**
 * Menggulir kolom input ke atas keyboard saat kolom itu fokus dan keyboard sudah terbuka.
 *
 * Layar yang memakai imePadding() biasanya sudah melakukan ini sendiri, tetapi di sebagian HP
 * (animasi keyboard berbeda / keyboard muncul setelah fokus) kolom di bagian bawah form tetap
 * tertutup. Modifier ini meminta guliran secara eksplisit setelah animasi keyboard selesai.
 * Pasang pada kolom input yang berada di dalam container yang bisa digulir.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
fun Modifier.keepAboveKeyboard(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isFocused, isImeVisible) {
        if (isFocused && isImeVisible) {
            delay(KEYBOARD_ANIMATION_MS)
            requester.bringIntoView()
        }
    }

    this
        .bringIntoViewRequester(requester)
        .onFocusEvent { isFocused = it.isFocused }
}

private const val KEYBOARD_ANIMATION_MS = 300L
