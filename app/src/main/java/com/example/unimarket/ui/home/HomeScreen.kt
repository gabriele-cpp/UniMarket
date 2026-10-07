package com.example.unimarket.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.*
import com.example.unimarket.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun Header(onProfileClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Brand()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "UNIMARKET",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, Color.Black)
            ) {
                IconButton(onClick = onProfileClick) {
                    Icon(Icons.Outlined.Person, contentDescription = "Profil")
                }
            }
        }
        HorizontalDivider(thickness = 2.dp, color = Color.Black)
    }
}

@Composable
fun HomeScreen(
    onProductClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onPostClick: () -> Unit,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            UniMarketBottomBar(
                onHomeClick = {},
                onPostClick = onPostClick,
                onMenuClick = onMenuClick,
                currentRoute = Routes.HOME
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Header(onProfileClick = onProfileClick)
                
                val today = remember {
                    val locale = Locale.forLanguageTag("id-ID")
                    val formatter = SimpleDateFormat("EEEE, d MMMM", locale)
                    formatter.format(Date()).uppercase()
                }
                
                Text(
                    text = today,
                    modifier = Modifier.padding(top = 24.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Temukan\nyang dibutuhkan.",
                    fontSize = 36.sp,
                    color = Color.Black,
                    lineHeight = 43.sp,
                    letterSpacing = (-1.6).sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                
                SearchButton(onClick = onSearchClick)
                
                Spacer(modifier = Modifier.height(32.dp))
                Section(title = "Kategori populer", action = "Lihat semua")
                
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProductRepository.getCategories()) { category ->
                        FilterButton(
                            title = category,
                            active = uiState.selectedCategory == category,
                            onClick = { viewModel.onCategorySelect(category) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Section(
                    title = if (uiState.selectedCategory == "Semua") "Baru ditambahkan" else "Kategori: ${uiState.selectedCategory}", 
                    action = "${uiState.products.size} barang"
                )
            }
            
            if (uiState.products.isEmpty()) {
                item {
                    EmptyState(
                        title = "Belum ada barang",
                        note = "Belum ada barang pada kategori ${uiState.selectedCategory}."
                    )
                }
            } else {
                items(uiState.products) { product ->
                    ProductCard(product = product, onClick = { onProductClick(product.id) })
                }
            }
        }
    }
}
