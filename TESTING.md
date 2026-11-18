# Rencana & Laporan Testing — Pertemuan 13

> Salin berkas ini ke **root repository proyek kelompok** dengan nama `TESTING.md`,
> isi bagian A–C **sebelum** menulis test, lalu bagian D–E setelah selesai.
> Berkas ini ikut dinilai.

**Kelompok:**
**Nama aplikasi:** UniMarket

| Anggota (Nama / NIM) | Username GitHub | Tugas testing | Berkas test yang ditulis |
|----------------------|-----------------|---------------|--------------------------|
| Gabriel Emil (PM) / NIM: … | gabriele-cpp | | |
| Kenny Lay (Backend) / NIM: … | michael-zen (commit sebagai `Michael-Zen`) | | |
| Daniel Gilberth Octavianus Latupeirissa (Frontend) / NIM: … | | | |

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

## B. Daftar Unit yang Akan Diuji

| Jenis | Nama kelas / fungsi | Berkas | Prioritas (1–3) |
|-------|---------------------|--------|:---------------:|
| ViewModel | | | |
| ViewModel | | | |
| Fungsi murni | | | |
| Mapper | | | |
| Screen (UI) | | | |
| Screen (UI) | | | |
| DAO (tantangan) | | | |

## C. Matriks Skenario ViewModel

Minimal **5 skenario** dari ViewModel proyek Anda. Wajib ada sukses, gagal, dan kosong.

| No | ViewModel | Skenario (Given / When / Then) | Jenis | Nama test |
|----|-----------|--------------------------------|-------|-----------|
| 1 | | Given … / When … / Then … | Sukses | |
| 2 | | | Gagal | |
| 3 | | | Kosong | |
| 4 | | | Aksi pengguna | |
| 5 | | | Loading / validasi / … | |

**Test double yang dipakai:**

| Dependency asli | Pengganti di test | Jenis (fake/stub/mock) | Alasan |
|-----------------|-------------------|------------------------|--------|
| | `Fake…Repository` | fake | |

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
