package com.example.unimarket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.ui.theme.AppColors

/** Pasangan warna (latar, teks) untuk tiap status barang. */
fun statusColors(status: ProductStatus): Pair<Color, Color> = when (status) {
    ProductStatus.TERSEDIA -> AppColors.StatusAvailableBg to AppColors.StatusAvailableText
    ProductStatus.NEGOSIASI -> AppColors.StatusNegotiationBg to AppColors.StatusNegotiationText
    ProductStatus.TERJUAL -> AppColors.StatusSoldBg to AppColors.StatusSoldText
}

@Composable
fun StatusBadge(status: ProductStatus, modifier: Modifier = Modifier) {
    val (background, content) = statusColors(status)
    Text(
        text = status.label.uppercase(),
        color = content,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
