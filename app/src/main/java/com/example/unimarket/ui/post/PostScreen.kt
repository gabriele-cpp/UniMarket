package com.example.unimarket.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.data.model.ProductDefaults
import com.example.unimarket.ui.components.CategoryGrid
import com.example.unimarket.ui.components.Caption
import com.example.unimarket.ui.util.imageModel
import com.example.unimarket.ui.components.UniMarketTopBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage

@Composable
fun PostScreen(
    onBack: () -> Unit,
    onPostSuccess: () -> Unit,
    viewModel: PostViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showSuccess) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onDismissSuccess()
                onPostSuccess()
            },
            title = { Text(if (uiState.isEditMode) "Perubahan disimpan" else "Barang berhasil diposting") },
            text = {
                Text(
                    if (uiState.isEditMode) "Data barang sudah diperbarui."
                    else "Barangmu sekarang tampil di beranda dan bisa dicari pembeli."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDismissSuccess()
                        onPostSuccess()
                    }
                ) {
                    Text(if (uiState.isEditMode) "Kembali" else "Kembali ke beranda")
                }
            }
        )
    }

    uiState.errorMessage?.let { message ->
        // Jika barang gagal dimuat (tidak ada / bukan milik user), tutup layar setelah dialog ditutup.
        val dismiss = {
            viewModel.onDismissError()
            if (uiState.loadFailed) onBack()
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Terjadi kesalahan") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = dismiss) {
                    Text("OK")
                }
            }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            viewModel.onImageSelected(uri?.toString())
        }
    )

    Scaffold(
        topBar = {
            UniMarketTopBar(
                title = if (uiState.isEditMode) "Edit barang" else "Pasang iklan",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        if (uiState.isLoadingProduct || uiState.loadFailed) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoadingProduct) CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "Jual barang yang masih berguna untuk mahasiswa lain.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 19.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.imageUri != null) {
                        AsyncImage(
                            model = imageModel(uiState.imageUri),
                            contentDescription = "Selected photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Upload",
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Pilih foto barang dari galeri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Klik untuk membuka foto",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(19.dp))

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Judul barang") },
                    singleLine = true,
                    isError = uiState.titleError != null,
                    supportingText = uiState.titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                Spacer(modifier = Modifier.height(13.dp))

                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = { viewModel.onPriceChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Harga") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.priceError != null,
                    supportingText = uiState.priceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                Spacer(modifier = Modifier.height(17.dp))
                Caption("KATEGORI")
                Spacer(modifier = Modifier.height(7.dp))

                CategoryGrid(
                    selected = uiState.categories.firstOrNull { it.id == uiState.categoryId }?.name.orEmpty(),
                    onSelected = { name ->
                        uiState.categories.firstOrNull { it.name == name }?.let { viewModel.onCategoryChange(it.id) }
                    },
                    categories = uiState.categories.map { it.name }
                )

                if (uiState.categoryError != null) {
                    Text(
                        text = uiState.categoryError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(17.dp))
                Caption("KONDISI BARANG")
                Spacer(modifier = Modifier.height(7.dp))

                // Daftar kondisi dari ProductDefaults (sama dengan data seed). Grid agar tidak terpotong di layar sempit.
                CategoryGrid(
                    selected = uiState.condition,
                    onSelected = viewModel::onConditionChange,
                    categories = ProductDefaults.CONDITIONS
                )

                Spacer(modifier = Modifier.height(17.dp))
                Caption("OPSI PENGIRIMAN & LOKASI")
                Spacer(modifier = Modifier.height(7.dp))

                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = { viewModel.onLocationChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Titik Temu COD Kampus") },
                    placeholder = { Text("Misal: Perpustakaan Utama, Kantin Gedung C") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = uiState.allowCod,
                        onCheckedChange = { viewModel.onToggleCod(it) }
                    )
                    Text("COD di Kampus", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    Checkbox(
                        checked = uiState.allowGoSend,
                        onCheckedChange = { viewModel.onToggleGoSend(it) }
                    )
                    Text("GoSend / GrabExpress", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (uiState.deliveryError != null) {
                    Text(
                        text = uiState.deliveryError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Deskripsi") },
                    placeholder = { Text("Ceritakan kondisi dan detail barang") },
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.onPublish() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(51.dp),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = when {
                            uiState.isSaving -> "MENYIMPAN..."
                            uiState.isEditMode -> "SIMPAN PERUBAHAN"
                            else -> "TERBITKAN IKLAN"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
