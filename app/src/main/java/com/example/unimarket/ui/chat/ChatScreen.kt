package com.example.unimarket.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.ChatBubble
import com.example.unimarket.ui.components.EmptyState
import com.example.unimarket.ui.components.UniMarketTopBar
import com.example.unimarket.ui.util.formatTime

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputState by viewModel.inputState.collectAsState()
    val listState = rememberLazyListState()

    // Gulir otomatis ke pesan terbaru.
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.lastIndex)
    }

    Scaffold(
        topBar = {
            UniMarketTopBar(
                title = uiState.otherUserName.ifBlank { "Chat" },
                onBack = onBack,
                subtitle = uiState.product?.let { "Mengenai ${it.title}" }
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.accessError == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    inputState.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputState.draft,
                            onValueChange = viewModel::onDraftChange,
                            placeholder = { Text("Tulis pesan...") },
                            maxLines = 4,
                            isError = inputState.error != null,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = viewModel::sendMessage,
                            enabled = inputState.draft.isNotBlank() && !inputState.isSending
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.accessError != null -> EmptyState(
                title = "Chat tidak tersedia",
                note = uiState.accessError!!,
                modifier = Modifier.padding(paddingValues)
            )

            uiState.messages.isEmpty() -> EmptyState(
                title = "Belum ada pesan",
                note = "Mulai percakapan, misalnya tanyakan apakah barang masih tersedia.",
                modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(
                        text = message.text,
                        isMine = message.senderId == uiState.currentUserId,
                        time = formatTime(message.timestamp)
                    )
                }
            }
        }
    }
}
