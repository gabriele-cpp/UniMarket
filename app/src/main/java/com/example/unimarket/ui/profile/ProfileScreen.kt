package com.example.unimarket.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarket.ui.AppViewModelProvider
import com.example.unimarket.ui.components.UniMarketTopBar
import com.example.unimarket.ui.theme.AppColors
import com.example.unimarket.ui.util.initials
import com.example.unimarket.ui.util.phoneForDisplay

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onMyItemsClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val user = uiState.user

    Scaffold(
        topBar = {
            UniMarketTopBar(
                title = "Profil saya",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(AppColors.SellerAvatar),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.let { initials(it.name) } ?: "",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user?.email ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    if (!user?.phone.isNullOrBlank()) {
                        Text(
                            text = phoneForDisplay(user!!.phone),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = viewModel::onEditClick, enabled = user != null) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit Profil")
                }
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            ProfileItem(
                label = "Barang saya",
                value = "${uiState.activeListings} aktif · ${uiState.soldListings} terjual",
                onClick = onMyItemsClick
            )
            ProfileItem(
                label = "Pengaturan profil",
                isArrow = true,
                onClick = viewModel::onEditClick
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onSignedOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(49.dp),
                shape = RoundedCornerShape(9.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("KELUAR", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (editState.isVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onEditDismiss,
            title = { Text("Edit Profil Pengguna") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editState.name,
                        onValueChange = viewModel::onEditNameChange,
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        isError = editState.nameError != null,
                        supportingText = editState.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editState.phone,
                        onValueChange = viewModel::onEditPhoneChange,
                        label = { Text("Nomor HP (WhatsApp)") },
                        placeholder = { Text("08xxxxxxxxxx") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = editState.phoneError != null,
                        supportingText = editState.phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    editState.saveError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onEditSave,
                    enabled = !editState.isSaving
                ) {
                    Text(if (editState.isSaving) "Menyimpan..." else "Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onEditDismiss) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProfileItem(
    label: String,
    value: String = "",
    isArrow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(vertical = 17.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Bold)
            if (isArrow) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            } else {
                Text(text = value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        HorizontalDivider()
    }
}
