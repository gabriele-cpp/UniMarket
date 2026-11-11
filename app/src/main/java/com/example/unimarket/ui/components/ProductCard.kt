package com.example.unimarket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.ui.theme.AppColors
import com.example.unimarket.ui.util.categoryColor
import com.example.unimarket.ui.util.imageModel
import com.example.unimarket.ui.util.formatRupiah
import com.example.unimarket.ui.util.productMark

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    alwaysShowStatus: Boolean = false,
    /** Konten tambahan di bawah kartu, misalnya tombol aksi di My Items. */
    footer: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 13.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 15.dp)
            ) {
                Text(
                    text = product.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = formatRupiah(product.price),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.categoryName.uppercase(),
                        modifier = Modifier
                            .border(1.dp, AppColors.TagBorder)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = AppColors.TagText
                    )
                    // Di katalog, badge hanya muncul untuk status selain TERSEDIA (mis. NEGOSIASI).
                    if (alwaysShowStatus || product.status != ProductStatus.TERSEDIA) {
                        StatusBadge(status = product.status)
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Detail"
            )
        }
        if (footer != null) {
            HorizontalDivider(thickness = 1.dp, color = AppColors.DividerLight)
            footer()
        }
    }
}
