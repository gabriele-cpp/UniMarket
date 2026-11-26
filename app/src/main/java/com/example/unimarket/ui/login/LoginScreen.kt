package com.example.unimarket.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.unimarket.ui.components.Brand
import com.example.unimarket.ui.components.Caption

/**
 * Route (tipis): menghubungkan ViewModel ke [LoginScreen] dan navigasi setelah login.
 * Tidak diuji di UI test; logikanya ada di LoginViewModel (unit test).
 */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    LoginScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::onLogin,
        onRegisterClick = onRegisterClick
    )
}

/** Layar login stateless: hanya menampilkan [uiState] dan meneruskan aksi lewat lambda. */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 52.dp)
    ) {
        Brand()
        Spacer(modifier = Modifier.height(24.dp))
        Caption("01 — MASUK")
        Text(
            text = "Selamat datang kembali",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Silakan masuk untuk melanjutkan",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email Kampus") },
            placeholder = { Text("nama@mahasiswa.ac.id") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth()
        )

        uiState.generalError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLogin,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("MASUK")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Masuk")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Belum punya akun? Daftar",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onRegisterClick)
                .padding(vertical = 8.dp)
        )
    }
}
