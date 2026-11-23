# UniMarket

Aplikasi Android marketplace barang bekas antarmahasiswa. Dibangun dengan Kotlin, Jetpack Compose, Room (data lokal), dan arsitektur MVVM (Screen → ViewModel → Repository → DAO/API).

- Panduan dan laporan testing: [TESTING.md](TESTING.md)
- Unit test: `./gradlew testDebugUnitTest` (juga dijalankan GitHub Actions)

## Pembayaran (Midtrans Sandbox)

Pembeli dapat membayar barang lewat **Midtrans Snap** dalam mode **Sandbox** (uji coba, tanpa uang sungguhan).

### Arsitektur

```
Aplikasi Android                     payment-server (laptop)            Midtrans Sandbox
─────────────────                    ───────────────────────            ────────────────
CheckoutScreen ─► CheckoutViewModel
                       │
                       ▼
               PaymentRepository ──► POST /payments ─────────────────► Snap API
               (OnlinePaymentRepository)   (menyimpan Server Key)       (token + redirect_url)
                  │        │
                  │        └───────► GET /payments/{orderId}/status ─► Status API
                  ▼
           Room: tabel orders
           (+ produk → TERJUAL saat lunas)

PaymentScreen: WebView membuka redirect_url Snap. Saat Snap mengarah ke
https://unimarket.app/payment-finish, WebView ditutup lalu status dicek ke server.
```

- **Server Key Midtrans hanya ada di `payment-server/.env`**, tidak pernah di aplikasi Android.
- Midtrans Mobile SDK tidak dipakai (sedang dihentikan). Halaman pembayaran Snap dibuka di WebView.
- Alur di aplikasi: Detail → **Beli Sekarang** → Checkout → **Bayar** → halaman Midtrans → hasil pembayaran. Status semua pesanan dapat dilihat di Menu → **Riwayat pembelian**.
- Pemetaan status Midtrans ke status pesanan (`mapMidtransStatus` di `data/model/Order.kt`):

  | Status Midtrans | Status pesanan |
  |---|---|
  | `settlement`, `capture` | `PAID` (produk otomatis menjadi `TERJUAL`) |
  | `pending` | `PENDING` |
  | `deny`, `cancel`, `failure` | `GAGAL` |
  | `expire` | `KEDALUWARSA` |

### 1. Menjalankan payment-server

Butuh Node.js 18+. Langkah lengkap ada di [payment-server/README.md](payment-server/README.md). Ringkasnya di PowerShell:

```powershell
cd payment-server
npm install
Copy-Item .env.example .env
notepad .env        # isi MIDTRANS_SERVER_KEY dengan Server Key Sandbox (SB-Mid-server-...)
npm start           # server berjalan di http://localhost:3000
```

Cek: `Invoke-RestMethod http://localhost:3000/health` harus mengembalikan `ok: True`. Biarkan jendela ini terbuka selama mengetes aplikasi.

### Menjalankan online dengan Render Free

Repo ini sudah memiliki `render.yaml` di root. Render akan membuat Web Service dari folder `payment-server`, memakai `npm ci` dan `npm start`, lalu memeriksa `/health`. Server otomatis bind ke port Render dan `0.0.0.0`.

1. Push perubahan project ke branch GitHub yang ingin dipakai untuk deploy.
2. Di Render Dashboard pilih **New → Blueprint**, lalu hubungkan repo UniMarket dan branch tersebut. Pilih `render.yaml` di root repo.
3. Saat diminta, isi `MIDTRANS_SERVER_KEY` dengan **Server Key Sandbox** (`SB-Mid-server-...`). Jangan memasukkan file `.env` atau commit key ke GitHub.
4. Tunggu deploy selesai, lalu cek `https://<nama-service>.onrender.com/health`; respons harus `{ "ok": true }`.
5. Di `local.properties` pada root Android project, tambahkan URL HTTPS dari service Render:

   ```properties
   PAYMENT_BASE_URL=https://<nama-service>.onrender.com/
   ```

6. Sync Gradle dan build ulang aplikasi. Emulator maupun HP bisa memakai URL publik ini dari jaringan mana saja.

> **Catatan paket gratis:** service Render Free akan tidur setelah 15 menit tanpa trafik. Request pertama setelah tidur dapat memerlukan sekitar satu menit untuk membangunkan server. Ini cocok untuk demo; jangan gunakan konfigurasi demo ini untuk pembayaran produksi.

### 2. Mengisi PAYMENT_BASE_URL

Alamat payment-server dibaca dari `local.properties` (tidak di-commit) ke `BuildConfig.PAYMENT_BASE_URL`.

- **Emulator:** tidak perlu mengisi apa pun. Default-nya `http://10.0.2.2:3000/` (alamat laptop dari emulator). Default ini juga membuat build di CI tetap berhasil.
- **Alamat lain:** tambahkan ke `local.properties` di root proyek, lalu Sync Gradle dan build ulang:

  ```properties
  PAYMENT_BASE_URL=http://10.0.2.2:3000/
  ```

  Akhiran `/` ditambahkan otomatis jika lupa.

- **HP fisik:** isi dengan IP laptop di jaringan Wi-Fi yang sama, misalnya `http://192.168.1.10:3000/`. Karena memakai HTTP biasa, IP tersebut juga harus ditambahkan ke `app/src/main/res/xml/network_security_config.xml`. Secara default cleartext **hanya** diizinkan untuk `10.0.2.2`.

### 3. Mengetes dengan Payment Simulator

1. Jalankan payment-server, lalu jalankan aplikasi di emulator.
2. Login, lalu buka barang milik **user lain** yang berstatus Tersedia. Tombol **Beli Sekarang** hanya muncul untuk barang seperti ini.
3. Tekan **Beli Sekarang**, lalu **Bayar**. Halaman Midtrans terbuka di dalam aplikasi.
4. Pilih **Virtual Account → BCA** dan salin nomor VA.
5. Di browser laptop, buka https://simulator.sandbox.midtrans.com, pilih **Virtual Account → BCA**, tempel nomor VA, lalu klik **Inquire** dan **Pay**.
6. Kembali ke aplikasi dan tekan **Cek Status**. Status menjadi **Lunas**, dan barang hilang dari Beranda karena sekarang berstatus Terjual.

Skenario lain yang bisa dicoba:

- **Server mati:** hentikan payment-server (Ctrl+C), lalu tekan **Bayar**. Muncul pesan "Gagal terhubung ke server pembayaran…".
- **Kartu kredit:** pilih Credit Card dan pakai kartu tes `4811 1111 1111 1114`, CVV `123`, tanggal kedaluwarsa bulan apa pun di masa depan, OTP `112233`.
- **Lanjutkan nanti:** tekan kembali di halaman Midtrans lalu pilih **Tinggalkan**. Pesanan tetap ada di Riwayat pembelian dan bisa dilanjutkan dengan **Lanjutkan Pembayaran**.

### Batasan

Fitur ini dibuat untuk demo, bukan untuk produksi:

- **Backend tidak memverifikasi harga.** Harga barang tersimpan di perangkat (Room) dan dikirim aplikasi ke payment-server apa adanya. Di aplikasi sungguhan, harga harus diambil dari database server agar tidak bisa dimanipulasi.
- **Webhook notifikasi Midtrans tidak dipakai.** Status dicek dengan polling ke `GET /payments/{orderId}/status`: otomatis saat halaman Midtrans ditutup atau pesanan dibuka dari Riwayat, dan manual lewat tombol **Cek Status**. Jika aplikasi ditutup setelah membayar, status baru diperbarui saat pesanan dibuka lagi.
- **payment-server tidak punya autentikasi** dan hanya dijalankan lokal di laptop.
- **Riwayat ikut terhapus bersama barang.** Data pesanan hanya ada di perangkat pembeli. Jika barang dihapus penjual, pesanan terkait ikut terhapus (foreign key `CASCADE`).
- **Hanya mode Sandbox.** Mengganti ke Production butuh Server Key Production, URL API produksi di payment-server, dan semua perbaikan di atas.
