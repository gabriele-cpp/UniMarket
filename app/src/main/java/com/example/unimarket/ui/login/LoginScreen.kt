package com.example.unimarket.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.UniMarketApplication
import com.example.unimarket.ui.components.Brand
import com.example.unimarket.ui.components.Caption

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val userPreferences = remember { (context.applicationContext as UniMarketApplication).container.userPreferences }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var showRegister by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 52.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
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
                value = email,
                onValueChange = { 
                    email = it
                    emailError = null
                },
                label = { Text("Email Kampus") },
                placeholder = { Text("nama@mahasiswa.ac.id") },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = null
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    var hasError = false
                    if (email.isBlank()) {
                        emailError = "Email kampus tidak boleh kosong"
                        hasError = true
                    } else if (!email.contains("@")) {
                        emailError = "Format email tidak valid (harus mengandung @)"
                        hasError = true
                    }
                    
                    if (password.isBlank()) {
                        passwordError = "Password tidak boleh kosong"
                        hasError = true
                    } else if (password.length < 4) {
                        passwordError = "Password minimal 4 karakter"
                        hasError = true
                    }

                    if (!hasError) {
                        // Save login session permanently
                        userPreferences.setLoggedIn(true, email = email)
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("MASUK")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Masuk")
            }
        }
        
        Text(
            text = "Belum punya akun? Daftar sekarang",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { showRegister = true }
        )
    }

    if (showRegister) {
        AlertDialog(
            onDismissRequest = { showRegister = false },
            title = { Text("Daftar Akun") },
            text = { Text("Fitur pendaftaran akun mahasiswa baru akan segera hadir.") },
            confirmButton = {
                TextButton(onClick = { showRegister = false }) {
                    Text("OK")
                }
            }
        )
    }
}
