# Rencana & Laporan Testing — Pertemuan 13

> Salin berkas ini ke **root repository proyek kelompok** dengan nama `TESTING.md`,
> isi bagian A–C **sebelum** menulis test, lalu bagian D–E setelah selesai.
> Berkas ini ikut dinilai.

**Kelompok:**
**Nama aplikasi:** UniMarket

| Anggota (Nama / NIM) | Username GitHub | Tugas testing (rencana pembagian awal) | Dikerjakan oleh | Berkas test |
|----------------------|-----------------|----------------------------------------|-----------------|-------------|
| Gabriel Emil (PM) / NIM: 01082240002 | gabriele-cpp | Unit test `PostViewModel`, `LoginViewModel`, `RegisterViewModel` (C2–C4) | Kenny Lay (pembagian diubah) | `ui/post/PostViewModelTest.kt` (13), `ui/login/LoginViewModelTest.kt` (8), `ui/register/RegisterViewModelTest.kt` (8) |
| Kenny Lay (Backend) / NIM: 01082240010 | michael-zen (commit sebagai `Michael-Zen`) | Audit + refactor testability, dependensi, `MainDispatcherRule`, semua Fake + `TestData`, unit test `HomeViewModel` & `SearchViewModel`, workflow CI | Kenny Lay | `testutil/*`, `ui/home/HomeViewModelTest.kt` (7), `ui/search/SearchViewModelTest.kt` (9), `ui/util/ValidatorsTest.kt` (+2 email) |
| Daniel Gilberth Octavianus Latupeirissa (Frontend) / NIM: 01082240027 | oktodd | Pisah Route + Screen stateless untuk `LoginScreen` & `MyItemsScreen`, 2 UI test Compose, verifikasi GitHub Actions | Kenny Lay (pembagian diubah) | `androidTest/…/ui/LoginScreenTest.kt` (5), `androidTest/…/ui/MyItemsScreenTest.kt` (8) |

> Catatan kontribusi: pembagian di atas adalah rencana awal. Pada pelaksanaannya seluruh refactor, test, dan CI dikerjakan oleh Kenny Lay, sehingga riwayat commit test di branch ini berasal dari akun `Michael-Zen`.

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
| 3 | ViewModel **tidak** memakai `Context`, `android.net.Uri`, `Log`, `Toast`, `FirebaseFirestore` langsung | ✔ (setelah refactor) | ✘ (awal) | `385cda5 refactor: logger ErrorMessages dapat diganti agar ViewModel teruji di JVM` — sebelumnya semua ViewModel memanggil `ErrorMessages.log()` yang memakai `android.util.Log.e` sehingga skenario error gagal di test JVM (`Method e … not mocked`). `imageUri` sudah `String`. |
| 4 | Logika (validasi, filter, hitung, format) ada di **fungsi murni**, bukan di Composable | ✔ | | — (`Validators`, `Formatters`, `PhoneUtils`; filter/sort dilakukan query Room) |
| 5 | Screen yang akan diuji **stateless** (menerima state + lambda, bukan ViewModel) | ✔ (2 screen yang diuji) | ⚠ (awal) | Awalnya semua 12 `…Screen` menerima ViewModel (signature multi-baris; tidak terdeteksi skrip). `098df4e refactor: pisah LoginRoute dan LoginScreen stateless`, `7e9290d refactor: pisah MyItemsRoute dan MyItemsScreen stateless`. |
| 6 | Semua `Icon`/`IconButton` yang dapat diketuk punya **`contentDescription`** | ✔ | | — 8 `contentDescription = null` sudah diperiksa: semuanya dekoratif (ikon di samping teks tombol/baris, `leadingIcon` pencarian) |

**Hasil `cek_proyek.sh` pertama (Tahap 1):** ✔ 8 · ⚠ 2 · ✘ 7 — ✘: `kotlinx-coroutines-test`, `MainDispatcherRule`, kelas Fake, test ViewModel (0/5), UI test Compose (0/2), workflow CI, `TESTING.md`.
**Setelah Tahap 2 (dependensi, MainDispatcherRule, refactor logger):** ✔ 12 · ⚠ 1 · ✘ 4 — sisa ✘: kelas Fake, test ViewModel, UI test Compose, workflow CI.
**Setelah Tahap 3 (Fake + test Home/Search):** ✔ 14 · ⚠ 1 · ✘ 2 — sisa ✘: UI test Compose, workflow CI.
**Akhir (semua test + CI):** ✔ 16 · ⚠ 1 · ✘ 0 — ⚠ tersisa: 8 `contentDescription = null` dekoratif.

**Temuan tambahan (baris 3):** `Validators.email()` memakai `android.util.Patterns`, padahal dipanggil `LoginViewModel` dan `RegisterViewModel` → test Login/Register dengan email valid akan crash di JVM. Diperbaiki di `23a9e39 refactor: validasi email tanpa android.util.Patterns agar teruji di JVM` (regex disalin persis, perilaku sama, + 2 test email).

## B. Daftar Unit yang Akan Diuji

| Jenis | Nama kelas / fungsi | Berkas | Prioritas (1–3) |
|-------|---------------------|--------|:---------------:|
| ViewModel | `HomeViewModel` — **✔ 7 test** | `app/src/test/java/com/example/unimarket/ui/home/HomeViewModelTest.kt` | 1 |
| ViewModel | `SearchViewModel` — **✔ 9 test** | `app/src/test/java/com/example/unimarket/ui/search/SearchViewModelTest.kt` | 1 |
| ViewModel | `PostViewModel` — **✔ 13 test** | `app/src/test/java/com/example/unimarket/ui/post/PostViewModelTest.kt` | 1 |
| ViewModel | `LoginViewModel` — **✔ 8 test** | `app/src/test/java/com/example/unimarket/ui/login/LoginViewModelTest.kt` | 1 |
| ViewModel | `RegisterViewModel` — **✔ 8 test** | `app/src/test/java/com/example/unimarket/ui/register/RegisterViewModelTest.kt` | 2 |
| Fungsi murni | `Validators`, `Formatters`, `PhoneUtils` (sudah ada; + test email oleh Kenny) | `app/src/test/java/com/example/unimarket/ui/util/` | 2 |
| Mapper | — (proyek Room tanpa DTO API; tidak ada mapper terpisah yang perlu diuji) | — | 3 |
| Screen (UI) | `LoginScreen` (stateless) — **✔ 5 UI test** | `app/src/androidTest/java/com/example/unimarket/ui/LoginScreenTest.kt` | 1 |
| Screen (UI) | `MyItemsScreen` (stateless) — **✔ 8 UI test** | `app/src/androidTest/java/com/example/unimarket/ui/MyItemsScreenTest.kt` | 1 |
| DAO (tantangan) | `ProductDao.searchProducts` / `getMarketplaceProducts` (bonus, belum dibuat) | `app/src/androidTest/java/com/example/unimarket/data/ProductDaoTest.kt` | 3 |

## C. Matriks Skenario ViewModel

Minimal **5 skenario** dari ViewModel proyek Anda. Wajib ada sukses, gagal, dan kosong.

### C1. HomeViewModel & SearchViewModel — ✔ 16 test lulus

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

### C2. PostViewModel — ✔ 13 test lulus

Semua skenario di bawah diimplementasikan, ditambah `kategori pertama otomatis terpilih saat form dibuka`; skenario 12 diperluas menjadi `selama menyimpan state saving aktif dan publish ganda diabaikan`.

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

### C3. LoginViewModel — ✔ 8 test lulus

Semua skenario di bawah diimplementasikan, ditambah `email ditulis kapital dan berspasi tetap dikenali` dan `format email salah ditolak sebelum menghubungi database`.

`LoginViewModel(userRepo, sessionRepo)`; user terdaftar: `FakeUserRepository(listOf(user(id = 1, email = "budi@kampus.ac.id") to "rahasia1"))`, sesi: `FakeSessionRepository(null, userRepo)`.

| No | Skenario (Given / When / Then) | Jenis | Nama test (usulan) |
|----|--------------------------------|-------|--------------------|
| 1 | Given email & password benar / When `onLogin()` / Then `isLoggedIn = true`, `session.currentUserId.value == 1` | Sukses | `login dengan akun terdaftar memulai sesi` |
| 2 | Given password salah / Then `generalError = AuthErrors.INVALID_CREDENTIALS`, sesi tetap null | Gagal | `password salah menampilkan pesan dan tidak login` |
| 3 | Given form kosong / Then `emailError = "Email wajib diisi"`, `passwordError = "Password wajib diisi"` | Kosong | `form kosong menampilkan pesan wajib diisi` |
| 4 | Given `userRepo.error = IllegalStateException()` / Then `ErrorMessages.LOGIN_FAILED` | Gagal | `database bermasalah menampilkan pesan gagal masuk` |
| 5 | Given error tampil / When `onEmailChange(...)` / Then `emailError` dan `generalError` hilang | Aksi pengguna | `mengetik ulang menghapus pesan error` |
| 6 | Given `delayMillis = 1000` / Then `isLoading` true lalu false setelah `advanceTimeBy` | Loading | `selama proses login state loading aktif` |

### C4. RegisterViewModel — ✔ 8 test lulus

Semua skenario di bawah diimplementasikan, ditambah `password kurang dari 6 karakter ditolak` dan `mengubah email menghapus pesan email sudah terdaftar`.

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

### C5. UI test Compose (androidTest) — ✔ 13 test lulus di emulator Pixel 8

| Screen | Skenario | Nama test |
|--------|----------|-----------|
| Login | Pesan error validasi & login tampil | `pesanErrorValidasiDanLoginTampilDiLayar` |
| Login | Ketuk MASUK memanggil `onLogin` | `ketukMasuk_memanggilOnLogin` |
| Login | Saat loading teks MASUK diganti indikator | `saatLoading_teksMasukDigantiIndikatorLoading` |
| Login | Mengetik email mengirim teks ke callback | `mengetikEmail_mengirimTeksKeCallback` |
| Login | Ketuk "Belum punya akun? Daftar" | `ketukDaftar_membukaHalamanRegister` |
| My Items | Daftar kosong + tombol JUAL BARANG | `daftarKosong_menampilkanPesanDanTombolJualBarang` |
| My Items | Judul barang & ringkasan "1 aktif · 1 terjual" | `daftarBarang_menampilkanJudulDanRingkasanStatus` |
| My Items | Pesan error tampil | `pesanErrorTampil` |
| My Items | Loading tidak menampilkan pesan kosong | `saatLoading_tidakMenampilkanPesanKosong` |
| My Items | Ketuk barang mengirim id yang benar | `ketukBarang_mengirimIdYangBenar` |
| My Items | Ketuk HAPUS mengirim barang yang dipilih | `ketukHapus_mengirimBarangYangDipilih` |
| My Items | Dialog konfirmasi → Hapus memanggil konfirmasi | `dialogKonfirmasiHapus_tombolHapusMemanggilKonfirmasi` |
| My Items | Tombol kembali memanggil `onBack` | `tombolKembali_memanggilOnBack` |

**Bukti test dapat gagal:** pada setiap test baru (29 unit test Post/Login/Register, 16 unit test Home/Search, 13 UI test), assertion pertama dibalik sementara (mis. `assertEquals` → `assertNotEquals`, `assertIsDisplayed` → `assertDoesNotExist`). Semua test tersebut menjadi merah, lalu dikembalikan dan hijau lagi.

## D. Hasil

| Jenis test | Jumlah | Lulus | Perintah |
|------------|:------:|:-----:|----------|
| Unit test (JVM) | 63 | 63 | `./gradlew testDebugUnitTest` |
| UI test (Compose) | 13 (+1 `ExampleInstrumentedTest`) | 14 | `./gradlew connectedDebugAndroidTest` (emulator Pixel 8) |
| Integration test (DAO) | 0 | — | belum dibuat (bonus) |

Rincian unit test: `PostViewModelTest` 13, `SearchViewModelTest` 9, `LoginViewModelTest` 8, `RegisterViewModelTest` 8, `HomeViewModelTest` 7, `ValidatorsTest` 9, `PhoneUtilsTest` 5, `FormattersTest` 3, `ExampleUnitTest` 1.

- Link run GitHub Actions: https://github.com/gabriele-cpp/UniMarket/actions/workflows/android-test.yml
- (Bonus) Coverage baris ViewModel yang diuji (`./gradlew createDebugUnitTestCoverageReport`): Home 100%, Search 100%, Login 100%, Register 100%, Post 77% → gabungan **143/155 = 92%**. Seluruh 12 ViewModel: 143/314 = 46% (Chat, ChatList, Detail, MyItems, Profile, Menu, Session belum diuji).

## E. Refleksi

1. **Bug apa yang ditemukan test?** Tidak ditemukan bug logika di ViewModel yang diuji; semua Flow sudah memakai `.catch` dan operasi tulis sudah dibungkus `try/catch`. Test justru menemukan dua ketergantungan tersembunyi pada kelas Android (`android.util.Log` di `ErrorMessages.log` dan `android.util.Patterns` di `Validators.email`) yang membuat skenario error serta validasi email tidak dapat diuji di JVM.
2. **Refactor apa yang dilakukan?** Pencatat error dibuat dapat diganti (`385cda5`), regex email dipindah ke Kotlin (`23a9e39`), dan `LoginScreen`/`MyItemsScreen` dipisah menjadi Route + Screen stateless (`098df4e`, `7e9290d`). Bagian tersulit adalah menemukan ketergantungan tidak langsung: ViewModel tidak mengimpor kelas Android, tetapi fungsi utilitas yang dipanggilnya mengimpor, sehingga tidak terdeteksi `cek_proyek.sh`.
3. **Test paling bernilai:** `selama menyimpan state saving aktif dan publish ganda diabaikan` (PostViewModel) melindungi dari iklan tersimpan dua kali saat tombol diketuk berulang, dan `kata kunci baru diterapkan setelah debounce 300 ms` (SearchViewModel) melindungi perilaku waktu tunda yang sulit dicek manual.
4. **Apa yang belum diuji:** ViewModel Chat, ChatList, Detail, MyItems, Profile, Menu, dan Session; serta query SQL `ProductDao` (filter di `FakeProductRepository` hanya meniru query tersebut, kebenaran SQL aslinya baru terbukti dengan DAO test Room in-memory). UI test belum dijalankan di CI karena butuh emulator.
