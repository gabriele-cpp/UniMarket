package com.example.unimarket.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.data.model.Conversation
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.EmptyState
import com.example.unimarket.ui.components.UniMarketTopBar
import com.example.unimarket.ui.theme.AppColors
import com.example.unimarket.ui.util.formatChatTime
import com.example.unimarket.ui.util.initials

@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onConversationClick: (productId: Int, buyerId: Long) -> Unit,
    viewModel: ChatListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { UniMarketTopBar(title = "Pesan", onBack = onBack) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.errorMessage != null -> EmptyState(
                title = "Terjadi kesalahan",
                note = uiState.errorMessage!!,
                modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)
            )

            uiState.conversations.isEmpty() -> EmptyState(
                title = "Belum ada percakapan",
                note = "Hubungi penjual dari halaman detail barang untuk mulai chat.",
                modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(uiState.conversations, key = { "${it.productId}-${it.buyerId}" }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        isLastFromMe = conversation.lastSenderId == uiState.currentUserId,
                        isMeBuyer = conversation.buyerId == uiState.currentUserId,
                        onClick = { onConversationClick(conversation.productId, conversation.buyerId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isLastFromMe: Boolean,
    isMeBuyer: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AppColors.SellerAvatar),
                contentAlignment = Alignment.Center
            ) {
                Text(initials(conversation.otherUserName), color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = conversation.otherUserName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // Tandai peran saya di percakapan ini.
                    text = (if (isMeBuyer) "Membeli: " else "Menjual: ") + conversation.productTitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = AppColors.TagText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (if (isLastFromMe) "Anda: " else "") + conversation.lastMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = formatChatTime(conversation.lastTimestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        HorizontalDivider(color = AppColors.DividerLight)
    }
}
