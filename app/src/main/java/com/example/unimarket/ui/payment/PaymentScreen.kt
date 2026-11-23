package com.example.unimarket.ui.payment

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.data.model.Order
import com.example.unimarket.data.model.OrderStatus
import com.example.unimarket.data.model.isPaymentFinishUrl
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.EmptyState
import com.example.unimarket.ui.components.OrderStatusBadge
import com.example.unimarket.ui.components.UniMarketTopBar
import com.example.unimarket.ui.components.orderStatusColors
import com.example.unimarket.ui.util.formatDateTime
import com.example.unimarket.ui.util.formatRupiah

@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onOpenOrders: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: PaymentViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLeaveDialog by remember { mutableStateOf(false) }
    val inWebView = uiState.phase == PaymentPhase.WEBVIEW

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    // Di halaman Midtrans, tombol kembali (sistem maupun top bar) meminta konfirmasi dulu.
    BackHandler(enabled = inWebView) { showLeaveDialog = true }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Tinggalkan pembayaran?") },
            text = {
                Text(
                    "Jika sudah membayar, status akan diperbarui otomatis. " +
                        "Jika belum, kamu bisa melanjutkan pembayaran dari Riwayat Pembelian."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showLeaveDialog = false
                    viewModel.onLeavePaymentPage()
                }) { Text("Tinggalkan") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Lanjut bayar") }
            }
        )
    }

    Scaffold(
        topBar = {
            UniMarketTopBar(
                title = if (inWebView) "Pembayaran" else "Status pembayaran",
                subtitle = uiState.order?.orderId,
                onBack = { if (inWebView) showLeaveDialog = true else onBack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val order = uiState.order
        val contentModifier = Modifier.fillMaxSize().padding(paddingValues)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            order == null -> EmptyState(
                title = "Pesanan tidak ditemukan",
                note = "Pesanan mungkin sudah dihapus.",
                modifier = contentModifier.padding(horizontal = 20.dp)
            )

            inWebView && order.redirectUrl != null -> SnapWebView(
                url = order.redirectUrl,
                onFinished = viewModel::onLeavePaymentPage,
                modifier = contentModifier
            )

            else -> PaymentResult(
                order = order,
                isChecking = uiState.isChecking,
                canContinuePayment = uiState.canContinuePayment,
                onCheckStatus = viewModel::checkStatus,
                onContinuePayment = viewModel::onContinuePayment,
                onOpenOrders = onOpenOrders,
                onGoHome = onGoHome,
                modifier = contentModifier
            )
        }
    }
}

/**
 * Halaman Midtrans Snap di WebView. Saat Snap mengarah ke URL finish
 * (https://unimarket.app/payment-finish), halaman ditutup lewat [onFinished].
 */
@SuppressLint("SetJavaScriptEnabled")   // Snap membutuhkan JavaScript
@Composable
private fun SnapWebView(url: String, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    var isPageLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    }
                    webViewClient = object : WebViewClient() {
                        private var finished = false

                        /** @return true jika URL sudah ditangani (tidak dimuat di WebView). */
                        private fun handle(target: String?): Boolean {
                            if (isPaymentFinishUrl(target)) {
                                if (!finished) {
                                    finished = true
                                    currentOnFinished()
                                }
                                return true
                            }
                            val scheme = target?.let { Uri.parse(it).scheme }
                            if (scheme != null && scheme != "http" && scheme != "https") {
                                // Deeplink aplikasi pembayaran (mis. GoPay/ShopeePay) dibuka di luar WebView.
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                } catch (_: ActivityNotFoundException) {
                                    // Aplikasinya tidak terpasang: biarkan user memilih metode lain.
                                }
                                return true
                            }
                            return false
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            handle(request.url.toString())

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            if (isPaymentFinishUrl(url)) {
                                view.stopLoading()
                                handle(url)
                            } else {
                                isPageLoading = true
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            isPageLoading = false
                        }
                    }
                    loadUrl(url)
                }
            },
            onRelease = { it.destroy() }
        )
        if (isPageLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PaymentResult(
    order: Order,
    isChecking: Boolean,
    canContinuePayment: Boolean,
    onCheckStatus: () -> Unit,
    onContinuePayment: () -> Unit,
    onOpenOrders: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, note) = resultText(order.status)
    val (_, accent) = orderStatusColors(order.status)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(72.dp))
        Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center)
        Text(
            note,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        OrderStatusBadge(order.status, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(16.dp))
        Text(order.productTitle, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(formatRupiah(order.amount), fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(
            formatDateTime(order.createdAt),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        val buttonModifier = Modifier.fillMaxWidth().height(48.dp)
        val shape = RoundedCornerShape(9.dp)
        if (order.status == OrderStatus.PENDING) {
            Button(onClick = onCheckStatus, enabled = !isChecking, modifier = buttonModifier, shape = shape) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Mengecek…", fontWeight = FontWeight.Bold)
                } else {
                    Text("Cek Status", fontWeight = FontWeight.Bold)
                }
            }
            if (canContinuePayment) {
                OutlinedButton(
                    onClick = onContinuePayment,
                    modifier = buttonModifier,
                    shape = shape,
                    border = BorderStroke(2.dp, Color.Black)
                ) {
                    Text("Lanjutkan Pembayaran", fontWeight = FontWeight.Bold)
                }
            }
        }
        OutlinedButton(
            onClick = onOpenOrders,
            modifier = buttonModifier,
            shape = shape,
            border = BorderStroke(2.dp, Color.Black)
        ) {
            Text("Riwayat Pembelian", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onGoHome, modifier = buttonModifier) {
            Text("Kembali ke Beranda", fontWeight = FontWeight.Bold)
        }
    }
}

private fun resultText(status: OrderStatus): Triple<ImageVector, String, String> = when (status) {
    OrderStatus.PAID -> Triple(
        Icons.Outlined.CheckCircle,
        "Pembayaran berhasil",
        "Barang sudah menjadi milikmu. Hubungi penjual untuk mengatur serah terima."
    )
    OrderStatus.PENDING -> Triple(
        Icons.Outlined.HourglassTop,
        "Menunggu pembayaran",
        "Selesaikan pembayaran sesuai instruksi Midtrans, lalu tekan Cek Status."
    )
    OrderStatus.GAGAL -> Triple(
        Icons.Outlined.ErrorOutline,
        "Pembayaran gagal",
        "Pembayaran ditolak atau dibatalkan. Kamu bisa membeli ulang dari halaman barang."
    )
    OrderStatus.KEDALUWARSA -> Triple(
        Icons.Outlined.ErrorOutline,
        "Pembayaran kedaluwarsa",
        "Batas waktu pembayaran sudah lewat. Kamu bisa membeli ulang dari halaman barang."
    )
}
