package com.example.unimarket.ui.myitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.EmptyState
import com.example.unimarket.ui.components.ProductCard
import com.example.unimarket.ui.components.Section
import com.example.unimarket.ui.components.StatusBadge
import com.example.unimarket.ui.components.UniMarketTopBar

@Composable
fun MyItemsScreen(
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    onEditClick: (Int) -> Unit,
    onPostClick: () -> Unit,
    viewModel: MyItemsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(dialogState.message) {
        dialogState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = { UniMarketTopBar(title = "Barang saya", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> EmptyState(
                title = "Terjadi kesalahan",
                note = uiState.errorMessage!!,
                modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)
            )

            uiState.items.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState(
                    title = "Belum ada barang",
                    note = "Barang yang kamu jual akan muncul di sini."
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onPostClick, shape = RoundedCornerShape(9.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JUAL BARANG", fontWeight = FontWeight.Bold)
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item {
                    val active = uiState.items.count { it.status != ProductStatus.TERJUAL }
                    Section(
                        title = "Iklan saya",
                        action = "$active aktif · ${uiState.items.size - active} terjual"
                    )
                }
                items(uiState.items, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        alwaysShowStatus = true,
                        footer = {
                            ItemActions(
                                onEdit = { onEditClick(product.id) },
                                onChangeStatus = { viewModel.onStatusRequest(product) },
                                onDelete = { viewModel.onDeleteRequest(product) }
                            )
                        }
                    )
                }
            }
        }
    }

    dialogState.pendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("Hapus barang?") },
            text = { Text("\"${product.title}\" akan dihapus permanen dan tidak bisa dikembalikan.") },
            confirmButton = {
                Button(
                    onClick = viewModel::onDeleteConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) { Text("Batal") }
            }
        )
    }

    dialogState.statusPickerFor?.let { product ->
        StatusPickerDialog(
            product = product,
            onSelect = viewModel::onStatusSelected,
            onDismiss = viewModel::onStatusDismiss
        )
    }
}

@Composable
private fun ItemActions(
    onEdit: () -> Unit,
    onChangeStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ActionButton(Icons.Outlined.Edit, "EDIT", onEdit, Modifier.weight(1f))
        ActionButton(Icons.Outlined.SwapHoriz, "STATUS", onChangeStatus, Modifier.weight(1f))
        ActionButton(
            Icons.Outlined.Delete, "HAPUS", onDelete, Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun StatusPickerDialog(
    product: Product,
    onSelect: (ProductStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah status") },
        text = {
            Column {
                Text(
                    text = product.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ProductStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(status) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = product.status == status, onClick = { onSelect(status) })
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusBadge(status = status)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
