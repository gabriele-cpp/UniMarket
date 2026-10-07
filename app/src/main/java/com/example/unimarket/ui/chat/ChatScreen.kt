package com.example.unimarket.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.ChatBubble
import com.example.unimarket.ui.components.UniMarketTopBar

@Composable
fun ChatScreen(
    productId: Int,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    val product = uiState.product

    Scaffold(
        topBar = {
            UniMarketTopBar(
                title = product?.seller ?: "Chat",
                onBack = onBack,
                subtitle = product?.let { "Mengenai ${it.title}" }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.currentMessage,
                    onValueChange = viewModel::onMessageChange,
                    placeholder = { Text("Tulis pesan...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = viewModel::sendMessage,
                    enabled = uiState.currentMessage.isNotBlank()
                ) {
                    Text("Kirim")
                }
            }
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        LaunchedEffect(uiState.sentMessages.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Hari ini",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
            )
            ChatBubble(
                text = "Halo, apakah barang ini masih tersedia?",
                fromSeller = false
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChatBubble(
                text = "Masih tersedia, silakan jika ingin bertanya.",
                fromSeller = true
            )
            uiState.sentMessages.forEach { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                ChatBubble(text = msg, fromSeller = false)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
