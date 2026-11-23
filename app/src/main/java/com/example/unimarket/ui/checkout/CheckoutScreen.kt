package com.example.unimarket.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.unimarket.data.model.Product
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.Caption
import com.example.unimarket.ui.components.EmptyState
import com.example.unimarket.ui.components.SellerCard
import com.example.unimarket.ui.components.UniMarketTopBar
import com.example.unimarket.ui.theme.AppColors
import com.example.unimarket.ui.util.categoryColor
import com.example.unimarket.ui.util.formatRupiah
import com.example.unimarket.ui.util.imageModel
import com.example.unimarket.ui.util.productMark

/**
 * Route (tipis): menghubungkan CheckoutViewModel ke [CheckoutScreen] dan
 * berpindah ke layar pembayaran setelah pesanan dibuat. Tidak diuji di UI test.
 */
@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onOpenPayment: (orderId: String) -> Unit,
    viewModel: CheckoutViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdOrder) {
        uiState.createdOrder?.let { order ->
            viewModel.onPaymentOpened()
            onOpenPayment(order.orderId)
        }
    }

    CheckoutScreen(
        uiState = uiState,
        onBack = onBack,
        onPay = viewModel::onPay,
        onIncrement = viewModel::incrementQuantity,
        onDecrement = viewModel::decrementQuantity
    )
}

/** Layar Checkout stateless: menerima state + lambda, tanpa ViewModel. */
@Composable
fun CheckoutScreen(
    uiState: CheckoutUiState,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {}
) {
    val product = uiState.product
    Scaffold(
        topBar = { UniMarketTopBar(title = "Checkout", onBack = onBack) },
        bottomBar = {
            if (product != null) {
                PayBar(
                    total = uiState.totalAmount,
                    enabled = uiState.canPay,
                    isProcessing = uiState.isProcessing,
                    onPay = onPay
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            product == null -> EmptyState(
                title = "Terjadi kesalahan",
                note = uiState.errorMessage ?: uiState.blockReason ?: "Barang tidak ditemukan",
                modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Caption("RINGKASAN PESANAN")
                Spacer(modifier = Modifier.height(12.dp))
                ProductSummary(product)

                Spacer(modifier = Modifier.height(20.dp))
                Caption("JUMLAH PEMBELIAN")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stok: ${product.stock} barang",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecrement,
                            enabled = uiState.quantity > 1,
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${uiState.quantity}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        OutlinedButton(
                            onClick = onIncrement,
                            enabled = uiState.quantity < product.stock,
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                SellerCard(name = product.sellerName)

                Spacer(modifier = Modifier.height(24.dp))
                Caption("RINCIAN PEMBAYARAN")
                Spacer(modifier = Modifier.height(8.dp))
                PriceRow(label = "Harga satuan", value = formatRupiah(product.price))
                PriceRow(label = "Jumlah", value = "${uiState.quantity}x")
                PriceRow(label = "Biaya layanan", value = formatRupiah(0))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp, color = Color.Black)
                PriceRow(label = "Total", value = formatRupiah(uiState.totalAmount), bold = true)

                uiState.blockReason?.let { Notice(text = it, isError = true) }
                uiState.errorMessage?.let { Notice(text = it, isError = true) }

                Notice(
                    text = "Pembayaran diproses oleh Midtrans mode Sandbox (uji coba). " +
                        "Tidak ada uang sungguhan yang ditarik.",
                    isError = false
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProductSummary(product: Product) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(categoryColor(product.categoryId)),
            contentAlignment = Alignment.Center
        ) {
            if (!product.imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = imageModel(product.imageUri),
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(productMark(product.title), color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = product.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${product.categoryName} · ${product.condition}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Text(
                text = formatRupiah(product.price),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Black else FontWeight.Medium)
    }
}

@Composable
private fun Notice(text: String, isError: Boolean) {
    val background = if (isError) AppColors.StatusSoldBg else AppColors.InfoBg
    val content = if (isError) AppColors.StatusSoldText else AppColors.InfoText
    Text(
        text = text,
        color = content,
        fontSize = 13.sp,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(12.dp)
            .then(if (isError) Modifier.testTag(CheckoutTags.ERROR) else Modifier)
    )
}

@Composable
private fun PayBar(total: Long, enabled: Boolean, isProcessing: Boolean, onPay: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(formatRupiah(total), fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Button(
                onClick = onPay,
                enabled = enabled,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.height(48.dp).testTag(CheckoutTags.PAY_BUTTON)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).testTag(CheckoutTags.PROGRESS),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Memproses…", fontWeight = FontWeight.Bold)
                } else {
                    Text("Bayar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** testTag untuk UI test CheckoutScreen. */
object CheckoutTags {
    const val PAY_BUTTON = "checkout_pay_button"
    const val PROGRESS = "checkout_progress"
    const val ERROR = "checkout_error"
}
