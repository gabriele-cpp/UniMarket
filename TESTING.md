# Rencana & Laporan Testing — Pertemuan 13

> Salin berkas ini ke **root repository proyek kelompok** dengan nama `TESTING.md`,
> isi bagian A–C **sebelum** menulis test, lalu bagian D–E setelah selesai.
> Berkas ini ikut dinilai.

**Kelompok:**
**Nama aplikasi:** UniMarket

| Anggota (Nama / NIM) | Username GitHub | Tugas testing | Berkas test yang ditulis |
|----------------------|-----------------|---------------|--------------------------|
| Gabriel Emil (PM) / NIM: … | gabriele-cpp | Unit test `PostViewModel`, `LoginViewModel`, `RegisterViewModel` (lihat C2–C4) | `ui/post/PostViewModelTest.kt`, `ui/login/LoginViewModelTest.kt`, `ui/register/RegisterViewModelTest.kt` *(belum)* |
| Kenny Lay (Backend) / NIM: … | michael-zen (commit sebagai `Michael-Zen`) | Audit + refactor testability, dependensi, `MainDispatcherRule`, semua Fake + `TestData`, unit test `HomeViewModel` & `SearchViewModel`, file workflow CI | `testutil/*`, `ui/home/HomeViewModelTest.kt` (7), `ui/search/SearchViewModelTest.kt` (9), `ui/util/ValidatorsTest.kt` (+2 email) |
| Daniel Gilberth Octavianus Latupeirissa (Frontend) / NIM: … | … | Pisah Route + Screen stateless untuk `LoginScreen` & `MyItemsScreen`, 2 UI test Compose, verifikasi GitHub Actions (hijau, badge, uji merah→revert) | `androidTest/…/ui/LoginScreenTest.kt`, `androidTest/…/ui/MyItemsScreenTest.kt` *(belum)* |

**Link repository:** https://github.com/gabriele-cpp/UniMarket
**Jenis proyek** (centang yang sesuai, boleh lebih dari satu):
- [x] Room (data lokal)
- [ ] Retrofit / REST API
- [ ] Firebase (Auth / Firestore / Storage)

---

## A. Audit Testability (hasil `cek_proyek.sh`)

Centang kondisi proyek Anda **saat ini**. Setiap "Belum" harus diperbaiki
(refactor) sebelum atau selama menulis test, lalu tulis commit-nya.

| No | Pemeriksaan | Sudah | Belum | Commit perbaikan |
|----|-------------|:-----:|:-----:|------------------|
| 1 | Setiap ViewModel menerima repository lewat **constructor** (bukan membuat sendiri / `getInstance()`) | ✔ | | — (12 ViewModel dibuat lewat `AppViewModelProvider` + `AppContainer`) |
| 2 | Repository punya **interface** (ViewModel bergantung pada interface, bukan `...Impl`) | ✔ | | — (`ProductRepository`, `CategoryRepository`, `UserRepository`, `SessionRepository`, `MessageRepository`; `Offline…Repository` hanya dirujuk `AppContainer`) |
| 3 | ViewModel **tidak** memakai `Context`, `android.net.Uri`, `Log`, `Toast`, `FirebaseFirestore` langsung | ✔ (setelah refactor) | ✘ (awal) | `756d4b7 refactor: logger ErrorMessages dapat diganti agar ViewModel teruji di JVM` — sebelumnya semua ViewModel memanggil `ErrorMessages.log()` yang memakai `android.util.Log.e` sehingga skenario error gagal di test JVM (`Method e … not mocked`). `imageUri` sudah `String`. |
| 4 | Logika (validasi, filter, hitung, format) ada di **fungsi murni**, bukan di Composable | ✔ | | — (`Validators`, `Formatters`, `PhoneUtils`; filter/sort dilakukan query Room) |
| 5 | Screen yang akan diuji **stateless** (menerima state + lambda, bukan ViewModel) | | ⚠ | Semua 12 `…Screen` menerima ViewModel (signature multi-baris; tidak terdeteksi skrip). Dikerjakan Daniel: pisah Route + Screen untuk 2 screen yang diuji. |
| 6 | Semua `Icon`/`IconButton` yang dapat diketuk punya **`contentDescription`** | ✔ | | — 8 `contentDescription = null` sudah diperiksa: semuanya dekoratif (ikon di samping teks tombol/baris, `leadingIcon` pencarian) |

**Hasil `cek_proyek.sh` pertama (Tahap 1):** ✔ 8 · ⚠ 2 · ✘ 7 — ✘: `kotlinx-coroutines-test`, `MainDispatcherRule`, kelas Fake, test ViewModel (0/5), UI test Compose (0/2), workflow CI, `TESTING.md`.
**Setelah Tahap 2 (dependensi, MainDispatcherRule, refactor logger):** ✔ 12 · ⚠ 1 · ✘ 4 — sisa ✘: kelas Fake, test ViewModel, UI test Compose, workflow CI.
**Setelah Tahap 3 (Fake + test Home/Search):** ✔ 14 · ⚠ 1 · ✘ 2 — sisa ✘: UI test Compose, workflow CI.

**Temuan tambahan (baris 3):** `Validators.email()` memakai `android.util.Patterns`, padahal dipanggil `LoginViewModel` dan `RegisterViewModel` → test Login/Register dengan email valid akan crash di JVM. Diperbaiki di `0ebdad1 refactor: validasi email tanpa android.util.Patterns agar teruji di JVM` (regex disalin persis, perilaku sama, + 2 test email).

## B. Daftar Unit yang Akan Diuji

| Jenis | Nama kelas / fungsi | Berkas | Prioritas (1–3) |
|-------|---------------------|--------|:---------------:|
| ViewModel | `HomeViewModel` — **Kenny ✔** | `app/src/test/java/com/example/unimarket/ui/home/HomeViewModelTest.kt` | 1 |
| ViewModel | `SearchViewModel` — **Kenny ✔** | `app/src/test/java/com/example/unimarket/ui/search/SearchViewModelTest.kt` | 1 |
| ViewModel | `PostViewModel` — Gabriel | `app/src/test/java/com/example/unimarket/ui/post/PostViewModelTest.kt` | 1 |
| ViewModel | `LoginViewModel` — Gabriel | `app/src/test/java/com/example/unimarket/ui/login/LoginViewModelTest.kt` | 1 |
| ViewModel | `RegisterViewModel` — Gabriel | `app/src/test/java/com/example/unimarket/ui/register/RegisterViewModelTest.kt` | 2 |
| Fungsi murni | `Validators`, `Formatters`, `PhoneUtils` (sudah ada; + test email oleh Kenny) | `app/src/test/java/com/example/unimarket/ui/util/` | 2 |
| Mapper | — (proyek Room tanpa DTO API; tidak ada mapper terpisah yang perlu diuji) | — | 3 |
| Screen (UI) | `LoginScreen` (stateless) — Daniel | `app/src/androidTest/java/com/example/unimarket/ui/LoginScreenTest.kt` | 1 |
| Screen (UI) | `MyItemsScreen` (stateless) — Daniel | `app/src/androidTest/java/com/example/unimarket/ui/MyItemsScreenTest.kt` | 1 |
| DAO (tantangan) | `ProductDao.searchProducts` / `getMarketplaceProducts` (bonus, belum dibagi) | `app/src/androidTest/java/com/example/unimarket/data/ProductDaoTest.kt` | 3 |

## C. Matriks Skenario ViewModel

Minimal **5 skenario** dari ViewModel proyek Anda. Wajib ada sukses, gagal, dan kosong.

### C1. HomeViewModel & SearchViewModel — Kenny ✔ (16 test lulus)

| No | ViewModel | Skenario (Given / When / Then) | Jenis | Nama test |
|----|-----------|--------------------------------|-------|-----------|
| 1 | Home | Given 3 barang & 3 kategori / When layar dibuka / Then barang tampil terbaru di atas, kategori "Semua" | Sukses | `barang dan kategori dari repository tampil dengan barang terbaru di atas` |
| 2 | Home | Given repository barang gagal / When layar dibuka / Then `LOAD_FAILED`, error tercatat | Gagal | `database barang gagal dibaca menampilkan pesan gagal memuat dan mencatat error` |
| 3 | Home | Given repository kategori gagal / Then `LOAD_FAILED` | Gagal | `kategori gagal dibaca juga menampilkan pesan gagal memuat` |
| 4 | Home | Given belum ada barang / Then daftar kosong, kategori tetap ada, tanpa error | Kosong | `belum ada barang menghasilkan daftar kosong tanpa pesan error` |
| 5 | Home | When pilih kategori Buku / Then hanya barang Buku | Aksi pengguna | `memilih kategori hanya menampilkan barang di kategori itu` |
| 6 | Home | Given filter Elektronik / When pilih Semua / Then semua barang | Aksi pengguna | `memilih Semua setelah filter menampilkan kembali seluruh barang` |
| 7 | Home | When barang baru tersimpan / Then langsung muncul paling atas | Reaktif (Flow) | `barang baru yang tersimpan di database langsung muncul di beranda` |
| 8 | Search | Given 3 barang / Then semua tampil, urutan Terbaru | Sukses | `tanpa kata kunci semua barang tersedia tampil dengan kategori` |
| 9 | Search | When ketik "kalku" / Then 299 ms belum dicari, 301 ms terfilter | Debounce | `kata kunci baru diterapkan setelah debounce 300 ms` |
| 10 | Search | When ketik "kalku" lalu "novel" sebelum 300 ms / Then hanya "novel" yang dicari | Debounce | `mengetik cepat hanya mencari kata kunci terakhir` |
| 11 | Search | When kata kunci dikosongkan / Then semua barang tanpa menunggu | Aksi pengguna | `mengosongkan kata kunci langsung menampilkan semua barang tanpa debounce` |
| 12 | Search | When kata kunci tanpa hasil / Then daftar kosong tanpa error | Kosong | `kata kunci tanpa hasil menghasilkan daftar kosong tanpa error` |
| 13 | Search | Given barang TERJUAL / Then tidak muncul | Aturan bisnis | `barang terjual tidak muncul di hasil pencarian` |
| 14 | Search | Given pencarian gagal / Then `LOAD_FAILED`, kategori tetap tampil | Gagal | `pencarian gagal menampilkan pesan error tetapi kategori tetap tampil` |
| 15 | Search | When kategori Buku + "kalku" / Then hanya "Buku Kalkulus" | Aksi pengguna | `filter kategori digabung dengan kata kunci` |
| 16 | Search | When urutan Harga termurah / Then harga naik | Aksi pengguna | `urutan harga termurah mengurutkan hasil dari harga terendah` |

### C2. PostViewModel — Gabriel (belum)

Dibuat dengan `PostViewModel(SavedStateHandle(), productRepo, categoryRepo, sessionRepo)`; mode edit: `SavedStateHandle(mapOf(PostViewModel.PRODUCT_ID_ARG to id))`. `uiState` berupa `MutableStateFlow`, jadi **tidak perlu** `start()`; cukup `runCurrent()` setelah aksi.

| No | Skenario (Given / When / Then) | Jenis | Nama test (usulan) |
|----|--------------------------------|-------|--------------------|
| 1 | Given login (user 1), form valid / When `onPublish()` / Then `showSuccess = true`, `productRepo.current` bertambah 1 dengan `sellerId = 1` dan `deliveryMethod = "COD Kampus & GoSend/GrabExpress"` | Sukses | `pasang iklan dengan data lengkap menyimpan barang dan menampilkan sukses` |
| 2 | Given judul kosong / When `onPublish()` / Then `titleError = "Judul barang wajib diisi"`, tidak ada yang tersimpan | Validasi | `judul kosong menampilkan pesan dan tidak menyimpan` |
| 3 | Given harga "0" / Then `priceError` diawali "Harga harus angka antara 1 dan" | Validasi | `harga nol ditolak` |
| 4 | Given COD & GoSend dimatikan / Then `deliveryError = "Pilih minimal 1 opsi pengiriman"` | Validasi | `tanpa opsi pengiriman tidak dapat dipublikasikan` |
| 5 | Given `writeError = IllegalStateException()` / Then `errorMessage = ErrorMessages.SAVE_FAILED`, `isSaving = false` | Gagal | `penyimpanan gagal menampilkan pesan dan form tetap terisi` |
| 6 | Given `writeError = java.io.IOException()` / Then `ErrorMessages.IMAGE_FAILED` | Gagal | `foto gagal disalin menampilkan pesan foto` |
| 7 | Given belum login (`FakeSessionRepository(null)`) / Then `ErrorMessages.SESSION_EXPIRED` | Gagal | `belum login tidak dapat memasang iklan` |
| 8 | Given `FakeCategoryRepository(emptyList())` / Then `categoryId = null`, publish → `categoryError = "Pilih kategori barang"` | Kosong | `tanpa kategori form menolak publish` |
| 9 | Given mode edit barang milik sendiri / Then judul/harga/kategori terisi dari barang lama | Mode edit | `mode edit memuat data barang lama` |
| 10 | Given mode edit barang milik user lain / Then `loadFailed = true`, pesan "Anda hanya bisa mengubah barang milik sendiri" | Gagal | `mengedit barang orang lain ditolak` |
| 11 | `onPriceChange("Rp 12.500abc")` / Then `price = "12500"` | Aksi pengguna | `input harga hanya menyimpan digit` |
| 12 | Given `writeDelayMillis = 1000` / When publish / Then `isSaving = true`; setelah `advanceTimeBy(1001)` → `false` | Loading | `selama menyimpan state saving aktif` |

### C3. LoginViewModel — Gabriel (belum)

`LoginViewModel(userRepo, sessionRepo)`; user terdaftar: `FakeUserRepository(listOf(user(id = 1, email = "budi@kampus.ac.id") to "rahasia1"))`, sesi: `FakeSessionRepository(null, userRepo)`.

| No | Skenario (Given / When / Then) | Jenis | Nama test (usulan) |
|----|--------------------------------|-------|--------------------|
| 1 | Given email & password benar / When `onLogin()` / Then `isLoggedIn = true`, `session.currentUserId.value == 1` | Sukses | `login dengan akun terdaftar memulai sesi` |
| 2 | Given password salah / Then `generalError = AuthErrors.INVALID_CREDENTIALS`, sesi tetap null | Gagal | `password salah menampilkan pesan dan tidak login` |
| 3 | Given form kosong / Then `emailError = "Email wajib diisi"`, `passwordError = "Password wajib diisi"` | Kosong | `form kosong menampilkan pesan wajib diisi` |
| 4 | Given `userRepo.error = IllegalStateException()` / Then `ErrorMessages.LOGIN_FAILED` | Gagal | `database bermasalah menampilkan pesan gagal masuk` |
| 5 | Given error tampil / When `onEmailChange(...)` / Then `emailError` dan `generalError` hilang | Aksi pengguna | `mengetik ulang menghapus pesan error` |
| 6 | Given `delayMillis = 1000` / Then `isLoading` true lalu false setelah `advanceTimeBy` | Loading | `selama proses login state loading aktif` |

### C4. RegisterViewModel — Gabriel (belum)

| No | Skenario (Given / When / Then) | Jenis | Nama test (usulan) |
|----|--------------------------------|-------|--------------------|
| 1 | Given data valid / When `onRegister()` / Then `isRegistered = true`, user tersimpan, sesi dimulai dengan id user baru | Sukses | `daftar dengan data valid langsung login` |
| 2 | Given email sudah terdaftar / Then `emailError = AuthErrors.EMAIL_TAKEN` (bukan generalError) | Gagal | `email yang sudah dipakai ditandai di kolom email` |
| 3 | Given konfirmasi beda / Then `confirmPasswordError = "Konfirmasi password tidak sama"`, tidak ada user baru | Validasi | `konfirmasi password berbeda tidak mendaftar` |
| 4 | Given form kosong / Then kelima error terisi | Kosong | `form kosong menampilkan semua pesan wajib diisi` |
| 5 | Given `userRepo.error` / Then `generalError = ErrorMessages.REGISTER_FAILED` | Gagal | `database bermasalah menampilkan pesan gagal mendaftar` |
| 6 | `onPhoneChange("0812-3456-7890")` / Then `phone = "081234567890"` | Aksi pengguna | `nomor HP hanya menyimpan digit` |

**Test double yang dipakai:**

| Dependency asli | Pengganti di test | Jenis (fake/stub/mock) | Alasan |
|-----------------|-------------------|------------------------|--------|
| `OfflineProductRepository` (Room + `ImageStorage`) | `FakeProductRepository` | fake | Data di memori; filter/urutan meniru `ProductDao`; saklar `observeError`, `writeError`, `writeDelayMillis` untuk skenario gagal & loading |
| `OfflineCategoryRepository` (Room) | `FakeCategoryRepository` | fake | Daftar kategori bisa diatur/kosong; `observeError` |
| `OfflineUserRepository` (Room + `PasswordHasher`) | `FakeUserRepository` | fake | Register/login di memori (email dinormalisasi seperti asli); `error`, `delayMillis` |
| `DefaultSessionRepository` (SharedPreferences) | `FakeSessionRepository` | fake | Sesi login tanpa Context; `logoutCount` untuk memeriksa logout |
| `android.util.Log` (lewat `ErrorMessages.log`) | `ErrorLogRule` | fake | `Log` tidak ada di JVM; error yang dicatat dapat di-assert |
| `Dispatchers.Main` | `MainDispatcherRule` | stub | `viewModelScope` butuh Main; memberi jam virtual untuk `advanceTimeBy` |

### Petunjuk untuk anggota

**Gabriel (unit test ViewModel)**
1. `git pull` branch `pertemuan-13`; semua fake sudah ada di `app/src/test/java/com/example/unimarket/testutil/`.
2. Contoh pola lengkap: `ui/home/HomeViewModelTest.kt` dan `ui/search/SearchViewModelTest.kt`. Wajib pasang `@get:Rule val mainRule = MainDispatcherRule()` dan `@get:Rule val errorLog = ErrorLogRule()`.
3. Jalankan `./gradlew testDebugUnitTest --tests "*PostViewModelTest"`. Untuk tiap test: ubah sementara nilai expected → pastikan merah → kembalikan.
4. Commit per ViewModel: `test: unit test PostViewModel`, `test: unit test LoginViewModel`, `test: unit test RegisterViewModel`.

**Daniel (UI test + CI)**
1. Refactor (commit terpisah per screen, mis. `refactor: pisah LoginRoute dan LoginScreen stateless`):
   - `LoginRoute(onLoginSuccess, onRegisterClick, viewModel = viewModel(factory = …))` → memanggil `LoginScreen(uiState, onEmailChange, onPasswordChange, onLogin, onRegisterClick)`. `LaunchedEffect(isLoggedIn)` tetap di Route.
   - `MyItemsRoute(…, viewModel)` → `MyItemsScreen(uiState, dialogState, onBack, onProductClick, onEditClick, onPostClick, onDeleteRequest, onDeleteConfirm, onDeleteDismiss, onStatusRequest, onStatusSelected, onStatusDismiss, onMessageShown)`.
   - Ganti pemanggilan di `ui/navigation/AppNavigation.kt` menjadi `LoginRoute` / `MyItemsRoute`. Pola: PANDUAN bagian A5.
2. Template: `../ch13-kit/kit/app/src/androidTest/java/com/example/proyekku/ui/ItemListScreenTest.kt` → `app/src/androidTest/java/com/example/unimarket/ui/`. Bungkus dengan `UniMarketTheme { … }`.
3. Skenario minimal:
   - **LoginScreen**: `emailError` tampil sebagai teks; klik **MASUK** memanggil `onLogin`; klik **Belum punya akun? Daftar** memanggil `onRegisterClick`; saat `isLoading` teks MASUK tidak tampil (tombol berisi indikator loading).
   - **MyItemsScreen**: daftar kosong menampilkan **Belum ada barang** dan klik **JUAL BARANG** memanggil `onPostClick`; judul barang tampil; `errorMessage` menampilkan **Terjadi kesalahan**; klik barang memanggil `onProductClick(id)`.
4. Jalankan emulator → `./gradlew connectedDebugAndroidTest`. Commit: `test: UI test LoginScreen dan MyItemsScreen`.
5. CI: file `.github/workflows/android-test.yml` disiapkan Kenny (Tahap 5). Tugas Daniel: pastikan run **hijau** di tab Actions, tambah badge di `README.md`, lalu uji jaring pengaman (commit yang sengaja merusak validasi → CI merah → `git revert`) dan simpan kedua link di bagian D.

## D. Hasil

| Jenis test | Jumlah | Lulus | Perintah |
|------------|:------:|:-----:|----------|
| Unit test (JVM) | | | `./gradlew testDebugUnitTest` |
| UI test (Compose) | | | `./gradlew connectedDebugAndroidTest` |
| Integration test (DAO) | | | `./gradlew connectedDebugAndroidTest` |

- Screenshot hasil test Android Studio: `docs/test-unit.png`, `docs/test-ui.png`
- Link run GitHub Actions yang **hijau**:
- (Bonus) Coverage baris ViewModel: ……… %

## E. Refleksi (singkat, 3–5 kalimat per butir)

1. **Bug apa yang ditemukan test?** (Jika ada: skenario, penyebab, dan commit perbaikannya.)
2. **Refactor apa yang Anda lakukan agar kode dapat diuji?** Apa yang paling sulit?
3. **Satu test yang menurut Anda paling bernilai**, dan mengapa (perilaku apa yang dilindunginya)?
4. **Apa yang belum diuji** dan mengapa? (Jujur; ini bukan pengurang nilai.)
