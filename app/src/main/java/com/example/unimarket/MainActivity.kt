package com.example.unimarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.ui.theme.UniMarketTheme

private enum class Screen { Login, Home, Search, Detail, Post, Chat, Profile, Menu }
private data class Product(val id: Int, val title: String, val price: String, val category: String, val condition: String, val seller: String, val description: String, val mark: String, val color: Color)

private val products = listOf(
    Product(1, "Calculus: Early Transcendentals", "Rp85.000", "Buku", "Sangat baik", "Nadia Putri", "Buku kalkulus edisi 8. Beberapa bagian diberi stabilo, tetapi isi masih lengkap dan rapi.", "BK", Color(0xFFD7E7F5)),
    Product(2, "Scientific Calculator FX-991", "Rp135.000", "Elektronik", "Baik", "Raka Wijaya", "Kalkulator scientific berfungsi normal, termasuk cover pelindung.", "FX", Color(0xFFE9DFB9)),
    Product(3, "Jas Lab Putih — Ukuran M", "Rp45.000", "Perlengkapan", "Sangat baik", "Citra Lestari", "Jas lab dipakai satu semester, sudah dicuci dan siap pakai.", "JL", Color(0xFFE6E1ED)),
    Product(4, "Set Alat Gambar Teknik", "Rp60.000", "Alat Kuliah", "Baik", "Dimas Pratama", "Penggaris, jangka, dan alat gambar dalam pouch.", "AT", Color(0xFFD9EAD8)),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { UniMarketTheme { UniMarketApp() } }
    }
}

@Composable
private fun UniMarketApp() {
    var route by rememberSaveable { mutableStateOf(Screen.Login.name) }
    var productId by rememberSaveable { mutableStateOf(1) }
    val screen = Screen.valueOf(route)
    val go: (Screen) -> Unit = { route = it.name }
    val selected = products.first { it.id == productId }
    when (screen) {
        Screen.Login -> Login { go(Screen.Home) }
        Screen.Home -> Home(go) { productId = it.id; go(Screen.Detail) }
        Screen.Search -> Search({ go(Screen.Home) }) { productId = it.id; go(Screen.Detail) }
        Screen.Detail -> Detail(selected, { go(Screen.Home) }) { go(Screen.Chat) }
        Screen.Post -> Post({ go(Screen.Home) }) { go(Screen.Home) }
        Screen.Chat -> Chat(selected, { go(Screen.Detail) })
        Screen.Profile -> Profile({ go(Screen.Home) }) { go(Screen.Login) }
        Screen.Menu -> Menu(go) { go(Screen.Login) }
    }
}

@Composable
private fun Login(onSuccess: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var register by rememberSaveable { mutableStateOf(false) }
    if (register) AlertDialog(onDismissRequest = { register = false }, title = { Text("Buat akun") }, text = { Text("Mode demo: Anda akan langsung masuk ke UniMarket.") }, confirmButton = { TextButton(onClick = { register = false; onSuccess() }) { Text("Lanjut") } }, dismissButton = { TextButton(onClick = { register = false }) { Text("Batal") } })
    Column(Modifier.fillMaxSize().padding(28.dp, 52.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Brand(); Spacer(Modifier.height(38.dp))
            Caption("01 — MASUK"); Text("Selamat datang.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("Masuk untuk menemukan barang kampus yang masih berguna.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 9.dp))
            Spacer(Modifier.height(30.dp))
            OutlinedTextField(email, { email = it; error = false }, Modifier.fillMaxWidth(), label = { Text("Email kampus") }, placeholder = { Text("nama@mahasiswa.ac.id") }, singleLine = true, isError = error, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(password, { password = it; error = false }, Modifier.fillMaxWidth(), label = { Text("Kata sandi") }, placeholder = { Text("••••••••") }, singleLine = true, isError = error, visualTransformation = PasswordVisualTransformation())
            if (error) Text("Isi email dan kata sandi untuk melanjutkan.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
            Spacer(Modifier.height(20.dp))
            Button(onClick = { if (email.isBlank() || password.isBlank()) error = true else onSuccess() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(9.dp)) { Text("MASUK     →", fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Text("Belum punya akun? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); Text("Daftar sekarang", Modifier.clickable { register = true }, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}

@Composable
private fun Home(go: (Screen) -> Unit, onProduct: (Product) -> Unit) = Scaffold(bottomBar = { BottomBar(Screen.Home, go) }) { pad ->
    LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { Header { go(Screen.Menu) }; Spacer(Modifier.height(34.dp)); Caption("SELASA, 30 SEPTEMBER"); Text("Temukan\nyang dibutuhkan.", fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 43.sp, letterSpacing = (-1.6).sp, modifier = Modifier.padding(top = 8.dp)); SearchButton { go(Screen.Search) }; Spacer(Modifier.height(37.dp)); Section("Kategori populer", "Lihat semua"); Categories(); Spacer(Modifier.height(39.dp)); Section("Baru ditambahkan", "12 barang") }
        items(products) { ProductCard(it) { onProduct(it) } }
    }
}

@Composable
private fun Header(onMenu: () -> Unit) { Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 7.dp, bottom = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Brand(); Spacer(Modifier.width(9.dp)); Text("UNIMARKET", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }; Box(Modifier.size(40.dp).border(2.dp, Color.Black).clickable(onClick = onMenu), contentAlignment = Alignment.Center) { Text("☰", fontSize = 23.sp) } }; HorizontalDivider(thickness = 2.dp, color = Color.Black) }

@Composable
private fun Brand() = Box(Modifier.size(40.dp).border(2.dp, Color.Black), contentAlignment = Alignment.Center) { Text("U", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 19.sp) }

@Composable
private fun Caption(value: String) = Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.4.sp)

@Composable
private fun SearchButton(onClick: () -> Unit) = Box(Modifier.fillMaxWidth().height(67.dp).padding(top = 0.dp).border(2.dp, Color.Black).clickable(onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { Row(verticalAlignment = Alignment.CenterVertically) { Text("⌕", fontSize = 20.sp); Spacer(Modifier.width(8.dp)); Text("Cari barang di kampus", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) } }

@Composable
private fun Section(title: String, action: String) = Row(Modifier.fillMaxWidth().padding(bottom = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title.uppercase(), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp); Text(action, color = if (action == "Lihat semua") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (action == "Lihat semua") FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }

@Composable
private fun Categories(selected: String = "Buku", onSelect: (String) -> Unit = {}) = Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Buku", "Elektronik", "Alat Kuliah").forEach { name -> FilterButton(name, name == selected) { onSelect(name) } } }

@Composable
private fun FilterButton(title: String, active: Boolean, onClick: () -> Unit) = OutlinedButton(onClick, shape = RoundedCornerShape(0.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (active) Color.Black else Color.White, contentColor = if (active) Color.White else Color.Black), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp)) { Text(title.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp) }

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) = Card(Modifier.fillMaxWidth().padding(bottom = 13.dp).clickable(onClick = onClick), shape = RoundedCornerShape(0.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(76.dp).background(Color(0xFFE6E6EA)), contentAlignment = Alignment.Center) { Text(product.mark, fontWeight = FontWeight.Black, fontSize = 17.sp) }; Column(Modifier.weight(1f).padding(horizontal = 15.dp)) { Text(product.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Black, fontSize = 16.sp); Text(product.price, Modifier.padding(top = 5.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(product.category.uppercase(), Modifier.padding(top = 9.dp).border(1.dp, Color(0xFF6E7180)).padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF555966), fontFamily = FontFamily.Monospace, fontSize = 9.sp) }; Text("›", fontSize = 24.sp, color = Color.Black) } }

@Composable
private fun Search(back: () -> Unit, onProduct: (Product) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf("Semua") }
    val results = products.filter { (category == "Semua" || it.category == category) && (query.isBlank() || it.title.contains(query, true) || it.category.contains(query, true)) }
    Scaffold(topBar = { Top("Cari barang", back) }) { pad -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp)) { item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text("Kata kunci") }, placeholder = { Text("Buku, kalkulator, jas lab...") }, leadingIcon = { Text("⌕") }, singleLine = true); Spacer(Modifier.height(18.dp)); Caption("KATEGORI"); Spacer(Modifier.height(7.dp)); CategoryGrid(category) { category = it }; Spacer(Modifier.height(21.dp)); Section("${results.size} hasil ditemukan", "Urutkan") }; if (results.isEmpty()) item { Empty("Barang tidak ditemukan", "Coba kata kunci atau kategori lain.") }; items(results) { ProductCard(it) { onProduct(it) } } } }
}

@Composable
private fun CategoryGrid(selected: String, onSelected: (String) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Semua", "Buku", "Elektronik", "Perlengkapan", "Alat Kuliah").chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { row.forEach { FilterButton(it, it == selected) { onSelected(it) } } } } }

@Composable
private fun Detail(product: Product, back: () -> Unit, onChat: () -> Unit) = Scaffold(topBar = { Top("Detail barang", back) }, bottomBar = { Surface(shadowElevation = 8.dp) { Button(onChat, Modifier.fillMaxWidth().padding(16.dp).height(50.dp), shape = RoundedCornerShape(9.dp)) { Text("HUBUNGI PENJUAL  →", fontWeight = FontWeight.Bold) } } }) { pad -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 85.dp)) { item { Box(Modifier.fillMaxWidth().height(235.dp).padding(top = 18.dp).clip(RoundedCornerShape(16.dp)).background(product.color), contentAlignment = Alignment.Center) { Text(product.mark, fontWeight = FontWeight.Black, fontSize = 54.sp) }; Spacer(Modifier.height(22.dp)); Caption(product.category.uppercase()); Text(product.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(product.price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp)); Spacer(Modifier.height(19.dp)); Text("Kondisi: ${product.condition}", Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(10.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(21.dp)); Text("Deskripsi", fontWeight = FontWeight.Bold); Text(product.description, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(23.dp)); Seller(product.seller) } } }

@Composable
private fun Seller(name: String) = Card(shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(21.dp)).background(Color(0xFFCDE8D1)), contentAlignment = Alignment.Center) { Text(name.take(1), fontWeight = FontWeight.Black) }; Column(Modifier.weight(1f).padding(start = 11.dp)) { Text("Dijual oleh", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(name, fontWeight = FontWeight.Bold) }; Text("›", fontSize = 24.sp) } }

@Composable
private fun Post(back: () -> Unit, done: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }; var price by rememberSaveable { mutableStateOf("") }; var description by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf("Buku") }; var success by rememberSaveable { mutableStateOf(false) }
    if (success) AlertDialog(onDismissRequest = { success = false; done() }, title = { Text("Iklan berhasil dibuat") }, text = { Text("Barang Anda akan muncul di beranda setelah ditinjau.") }, confirmButton = { TextButton(onClick = { success = false; done() }) { Text("Kembali ke beranda") } })
    Scaffold(topBar = { Top("Pasang iklan", back) }) { pad -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) { item { Text("Jual barang yang masih berguna untuk mahasiswa lain.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 19.dp)); Upload(); Spacer(Modifier.height(19.dp)); OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Judul barang") }, singleLine = true); Spacer(Modifier.height(13.dp)); OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("Harga") }, prefix = { Text("Rp ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Spacer(Modifier.height(17.dp)); Caption("KATEGORI"); Spacer(Modifier.height(7.dp)); CategoryGrid(category) { category = it }; Spacer(Modifier.height(14.dp)); OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Deskripsi") }, placeholder = { Text("Ceritakan kondisi dan detail barang") }, minLines = 3); Spacer(Modifier.height(20.dp)); Button({ success = true }, Modifier.fillMaxWidth().height(51.dp), enabled = title.isNotBlank() && price.isNotBlank(), shape = RoundedCornerShape(9.dp)) { Text("TERBITKAN IKLAN", fontWeight = FontWeight.Bold) } } } }
}

@Composable
private fun Upload() = Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("＋", fontSize = 25.sp); Text("Tambah foto barang", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("Maks. 5 foto", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } }

@Composable
private fun Chat(product: Product, back: () -> Unit) {
    var message by rememberSaveable { mutableStateOf("") }; var sent by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { Top(product.seller, back, "Mengenai ${product.title}") }, bottomBar = { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(message, { message = it }, Modifier.weight(1f), placeholder = { Text("Tulis pesan...") }, singleLine = true); Spacer(Modifier.width(8.dp)); Button({ sent = message; message = "" }, enabled = message.isNotBlank()) { Text("Kirim") } } }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(20.dp)) { Text("Hari ini", Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); Spacer(Modifier.height(18.dp)); Bubble("Halo, apakah barang ini masih tersedia?", false); Spacer(Modifier.height(10.dp)); Bubble("Masih tersedia, silakan jika ingin bertanya.", true); if (sent.isNotBlank()) { Spacer(Modifier.height(10.dp)); Bubble(sent, false) } } }
}

@Composable
private fun Bubble(text: String, fromSeller: Boolean) = Box(Modifier.fillMaxWidth(), contentAlignment = if (fromSeller) Alignment.CenterEnd else Alignment.CenterStart) { Text(text, Modifier.clip(RoundedCornerShape(12.dp)).background(if (fromSeller) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary).padding(12.dp), color = if (fromSeller) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp) }

@Composable
private fun Profile(back: () -> Unit, signOut: () -> Unit) = Scaffold(topBar = { Top("Profil saya", back) }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(68.dp).clip(RoundedCornerShape(34.dp)).background(Color(0xFFCDE8D1)), contentAlignment = Alignment.Center) { Text("GE", fontWeight = FontWeight.Black, fontSize = 20.sp) }; Column(Modifier.padding(start = 14.dp)) { Text("Gabriel Emil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("01082240002@mahasiswa.ac.id", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } }; Spacer(Modifier.height(28.dp)); ProfileItem("Barang yang dijual", "2 iklan aktif"); ProfileItem("Barang tersimpan", "4 barang"); ProfileItem("Pengaturan akun", "›"); Spacer(Modifier.weight(1f)); OutlinedButton(signOut, Modifier.fillMaxWidth().height(49.dp), shape = RoundedCornerShape(9.dp)) { Text("KELUAR", fontWeight = FontWeight.Bold) } } }

@Composable
private fun ProfileItem(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 17.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontWeight = FontWeight.Bold); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }; HorizontalDivider() }

@Composable
private fun Menu(go: (Screen) -> Unit, signOut: () -> Unit) = Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
    Text("← Kembali", Modifier.clickable { go(Screen.Home) }.padding(top = 10.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E1DE))
    Spacer(Modifier.height(39.dp))
    Caption("NAVIGASI")
    Spacer(Modifier.height(13.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Menu", fontSize = 36.sp, letterSpacing = (-1.8).sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Box(Modifier.size(48.dp).border(2.dp, Color.Black, CircleShape), contentAlignment = Alignment.Center) { Text("GE", fontSize = 18.sp) }
    }
    Spacer(Modifier.height(22.dp))
    HorizontalDivider(thickness = 2.dp, color = Color.Black)
    Spacer(Modifier.height(33.dp))
    MenuItem("⌂", "Beranda", "Barang terbaru untukmu") { go(Screen.Home) }
    MenuItem("⌕", "Cari barang", "Cari buku, alat & elektronik") { go(Screen.Search) }
    MenuItem("+", "Jual barang", "Pasang iklan baru") { go(Screen.Post) }
    MenuItem("◌", "Pesan", "Chat dengan penjual") { go(Screen.Chat) }
    MenuItem("◎", "Profil saya", "Akun & barang jualanmu") { go(Screen.Profile) }
    HorizontalDivider(thickness = 2.dp, color = Color.Black)
    Spacer(Modifier.weight(1f))
    Box(Modifier.fillMaxWidth().height(52.dp).border(2.dp, Color.Black).clickable(onClick = signOut), contentAlignment = Alignment.Center) { Text("KELUAR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp) }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun MenuItem(symbol: String, title: String, note: String, click: () -> Unit) { Row(Modifier.fillMaxWidth().height(97.dp).clickable(onClick = click), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(49.dp).border(2.dp, Color.Black), contentAlignment = Alignment.Center) { Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold) }; Column(Modifier.weight(1f).padding(start = 19.dp)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(note, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }; Text("›", fontSize = 25.sp, color = Color.Black) }; HorizontalDivider(color = Color(0xFFD9DAD7)) }

@Composable
private fun BottomBar(current: Screen, go: (Screen) -> Unit) = Surface(color = Color.White, shadowElevation = 0.dp) { Row(Modifier.fillMaxWidth().height(66.dp).border(width = 2.dp, color = Color.Black), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) { Nav("⌂", "Beranda", current == Screen.Home) { go(Screen.Home) }; Nav("＋", "Jual", false) { go(Screen.Post) }; Nav("☰", "Menu", false) { go(Screen.Menu) } } }

@Composable
private fun Nav(symbol: String, label: String, active: Boolean, click: () -> Unit) = Column(Modifier.clickable(onClick = click).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("$symbol ${label.uppercase()}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Bold) }

@Composable
private fun Top(title: String, back: () -> Unit, subtitle: String? = null) = Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clickable(onClick = back), contentAlignment = Alignment.CenterStart) { Text("‹", fontSize = 34.sp, lineHeight = 30.sp) }; Column(Modifier.padding(start = 6.dp)) { Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp); if (subtitle != null) Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } }

@Composable
private fun Empty(title: String, note: String) = Column(Modifier.fillMaxWidth().padding(top = 52.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, fontWeight = FontWeight.Bold); Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewUniMarket() { UniMarketTheme { UniMarketApp() } }
