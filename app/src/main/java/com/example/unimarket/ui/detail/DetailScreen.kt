package com.example.unimarket.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.*
import com.example.unimarket.ui.util.categoryColor
import com.example.unimarket.ui.util.imageModel
import com.example.unimarket.ui.util.formatRupiah
import com.example.unimarket.ui.util.productMark
import com.example.unimarket.ui.util.buildWhatsAppUrl
import com.example.unimarket.ui.util.whatsAppGreeting
import com.example.unimarket.ui.theme.AppColors
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Membuka chat WhatsApp ke penjual dengan pesan pembuka.
 * @return pesan error (bahasa Indonesia) jika gagal, atau null jika berhasil.
 */
private fun openWhatsApp(context: Context, phone: String, productTitle: String): String? {
    val url = buildWhatsAppUrl(phone, whatsAppGreeting(productTitle))
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        null
    } catch (e: ActivityNotFoundException) {
        "Tidak ada aplikasi untuk membuka WhatsApp. Pasang WhatsApp atau browser terlebih dahulu."
    }
}

@Composable
fun DetailScreen(
    productId: Int,
    onBack: () -> Unit,
    /** Membuka chat dengan buyerId = user yang sedang login. */
    onChat: (buyerId: Long) -> Unit,
    onEdit: () -> Unit,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val product = uiState.product
    val isLoading = uiState.isLoading
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    if (isLoading) {
        Scaffold(
            topBar = { UniMarketTopBar(title = "Detail barang", onBack = onBack) }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (product == null) {
        Scaffold(
            topBar = { UniMarketTopBar(title = "Detail barang", onBack = onBack) }
        ) { paddingValues ->
            EmptyState(
                title = if (uiState.errorMessage != null) "Terjadi kesalahan" else "Barang tidak ditemukan",
                note = uiState.errorMessage ?: "Barang mungkin telah dihapus.",
                modifier = Modifier.padding(paddingValues)
            )
        }
        return
    }

    Scaffold(
        topBar = { UniMarketTopBar(title = "Detail barang", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                when {
                    // Pemilik: tidak perlu menghubungi diri sendiri, tampilkan tombol edit.
                    uiState.isOwner -> OutlinedButton(
                        onClick = onEdit,
                        modifier = buttonModifier,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EDIT BARANG")
                    }

                    product.status == ProductStatus.TERJUAL -> Button(
                        onClick = {},
                        enabled = false,
                        modifier = buttonModifier
                    ) {
                        Text("Barang sudah terjual")
                    }

                    else -> Row(
                        modifier = buttonModifier,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { uiState.currentUserId?.let(onChat) },
                            enabled = uiState.currentUserId != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("HUBUNGI PENJUAL", maxLines = 1)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                        if (product.sellerPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val error = openWhatsApp(context, product.sellerPhone, product.title)
                                    if (error != null) scope.launch { snackbarHostState.showSnackbar(error) }
                                },
                                border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.WhatsApp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.WhatsApp)
                            ) {
                                Text("WHATSAPP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(categoryColor(product.categoryId)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!product.imageUri.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = imageModel(product.imageUri),
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = productMark(product.title),
                            color = Color.Black,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Caption(product.categoryName.uppercase())
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = formatRupiah(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val (statusBg, statusText) = statusColors(product.status)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = product.status.label,
                            color = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Kondisi: ${product.condition}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📍 ${product.location}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Opsi Pengiriman",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "🚚 ${product.deliveryMethod}",
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Deskripsi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = product.description,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                SellerCard(name = product.sellerName, phone = product.sellerPhone)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
