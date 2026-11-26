package com.example.unimarket.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.data.model.Order
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.unimarket.ui.components.EmptyState
import com.example.unimarket.ui.components.OrderStatusBadge
import com.example.unimarket.ui.components.Section
import com.example.unimarket.ui.components.UniMarketTopBar
import com.example.unimarket.ui.util.formatDateTime
import com.example.unimarket.ui.util.formatRupiah

@Composable
fun RiwayatPembelianScreen(
    onBack: () -> Unit,
    onOrderClick: (orderId: String) -> Unit,
    viewModel: RiwayatPembelianViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { UniMarketTopBar(title = "Riwayat pembelian", onBack = onBack) }
    ) { paddingValues ->
        val contentModifier = Modifier.fillMaxSize().padding(paddingValues)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> EmptyState(
                title = "Terjadi kesalahan",
                note = uiState.errorMessage!!,
                modifier = contentModifier.padding(horizontal = 20.dp)
            )

            uiState.orders.isEmpty() -> EmptyState(
                title = "Belum ada pembelian",
                note = "Barang yang kamu beli akan muncul di sini.",
                modifier = contentModifier.padding(horizontal = 20.dp)
            )

            else -> LazyColumn(
                modifier = contentModifier.padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item {
                    Section(title = "Pesanan saya", action = "${uiState.orders.size} pesanan")
                }
                items(uiState.orders, key = { it.orderId }) { order ->
                    OrderRow(order = order, onClick = { onOrderClick(order.orderId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = order.productTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Penjual: ${order.sellerName}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDateTime(order.createdAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
            Text(formatRupiah(order.amount), fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(6.dp))
            OrderStatusBadge(order.status)
        }
    }
}
