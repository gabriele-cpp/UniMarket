package com.example.unimarket.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.ui.navigation.Routes

@Composable
fun UniMarketBottomBar(
    onHomeClick: () -> Unit,
    onPostClick: () -> Unit,
    onMenuClick: () -> Unit,
    currentRoute: String = Routes.HOME
) {
    Surface(
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .border(2.dp, Color.Black)
        ) {
            BottomBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Home,
                label = "BERANDA",
                isActive = currentRoute == Routes.HOME,
                onClick = onHomeClick
            )
            BottomBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AddCircleOutline,
                label = "JUAL",
                isActive = currentRoute == Routes.POST,
                onClick = onPostClick
            )
            BottomBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Menu,
                label = "MENU",
                isActive = currentRoute == Routes.MENU,
                onClick = onMenuClick
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.Black else Color.Gray
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Black else FontWeight.Normal,
            color = if (isActive) Color.Black else Color.Gray
        )
    }
}
