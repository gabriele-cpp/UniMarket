# UniMarket Payment Server (Midtrans Sandbox)

Backend kecil untuk fitur pembayaran UniMarket. Server ini menyimpan **Server Key Midtrans** dan meneruskan request dari aplikasi Android ke Midtrans Snap (mode **sandbox**). Server Key tidak boleh ada di aplikasi Android.

- Node.js 18+ dan Express (ES module)
- Tanpa database dan tanpa SDK Midtrans (memakai `fetch` bawaan Node)

## 1. Instalasi

Jalankan di PowerShell dari root repo:

```powershell
cd payment-server
npm install
```

## 2. Isi file `.env`

```powershell
Copy-Item .env.example .env
notepad .env
```

Isi `MIDTRANS_SERVER_KEY` dengan Server Key sandbox dari **Midtrans Dashboard (mode Sandbox) > Settings > Access Keys**. Server Key sandbox diawali `SB-Mid-server-`.

```env
MIDTRANS_SERVER_KEY=SB-Mid-server-xxxxxxxxxxxx
PORT=3000
```

File `.env` sudah masuk `.gitignore`, jadi tidak ikut ter-commit. Jangan bagikan Server Key ke siapa pun.

## 3. Menjalankan server

```powershell
npm start
```

Output yang diharapkan:

```
payment-server berjalan di http://localhost:3000
Dari emulator Android: http://10.0.2.2:3000
```

Server berhenti dengan pesan error jika `MIDTRANS_SERVER_KEY` belum diisi.

## 4. Endpoint

| Method | Path | Keterangan |
|---|---|---|
| GET | `/health` | Cek server hidup, mengembalikan `{ "ok": true }` |
| POST | `/payments` | Membuat transaksi Snap, mengembalikan `{ token, redirectUrl }` |
| GET | `/payments/:orderId/status` | Status ringkas: `{ orderId, transactionStatus, paymentType }` |

Body `POST /payments`:

```json
{
  "orderId": "UM-1731900000",
  "amount": 150000,
  "itemName": "Kalkulator Casio FX-991",
  "customerName": "Budi",
  "customerEmail": "budi@example.com"
}
```

Aturan validasi:

- `orderId` wajib, hanya huruf, angka, `-`, `_`, `~`, `.`, maksimal 50 karakter, dan **harus unik** untuk setiap transaksi
- `amount` wajib, bilangan bulat > 0 (Rupiah, tanpa desimal)
- `itemName` wajib (dipotong otomatis menjadi 50 karakter)
- `customerName` dan `customerEmail` opsional

Contoh error (HTTP 400):

```json
{ "error": "amount wajib berupa bilangan bulat lebih dari 0" }
```

Jika `GET /payments/:orderId/status` dipanggil sebelum user memilih metode bayar, Midtrans membalas 404 dan server mengembalikan `transactionStatus: "pending"`.

Nilai `transactionStatus` yang umum: `pending`, `settlement` (berhasil), `capture` (kartu kredit berhasil), `deny`, `cancel`, `expire`, `failure`.

## 5. Tes manual dengan PowerShell

Buka jendela PowerShell **kedua** (server tetap berjalan di jendela pertama).

**Health check**

```powershell
Invoke-RestMethod http://localhost:3000/health
```

**Membuat transaksi**

```powershell
$orderId = "UM-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$body = @{
    orderId       = $orderId
    amount        = 150000
    itemName      = "Kalkulator Casio FX-991"
    customerName  = "Budi"
    customerEmail = "budi@example.com"
} | ConvertTo-Json

$res = Invoke-RestMethod -Method Post -Uri http://localhost:3000/payments `
    -ContentType "application/json" -Body $body
$res
Start-Process $res.redirectUrl   # buka halaman pembayaran Snap di browser
```

**Cek status**

```powershell
Invoke-RestMethod "http://localhost:3000/payments/$orderId/status"
```

**Tes validasi (harus error 400)**

```powershell
try {
    Invoke-RestMethod -Method Post -Uri http://localhost:3000/payments `
        -ContentType "application/json" -Body '{"orderId":"X1","amount":0,"itemName":"Tes"}'
} catch {
    $_.Exception.Response.StatusCode.value__
    $_.ErrorDetails.Message
}
```

## 6. Membayar dengan Midtrans Payment Simulator

1. Buka `redirectUrl` di browser (perintah `Start-Process` di atas).
2. Pilih metode, misalnya **Virtual Account > BCA**, lalu salin nomor VA yang muncul.
3. Buka https://simulator.sandbox.midtrans.com, pilih **Virtual Account > BCA**, tempel nomor VA, klik **Inquire**, lalu **Pay**.
4. Cek status lagi. `transactionStatus` seharusnya berubah menjadi `settlement`.

Alternatif: pilih **QRIS/GoPay**, salin URL gambar QR, lalu tempel di menu **QRIS** pada simulator. Untuk kartu kredit, pakai kartu tes `4811 1111 1111 1114`, CVV `123`, tanggal kedaluwarsa bulan apa pun di masa depan, dan OTP `112233`.

Setelah pembayaran selesai, Snap mengarahkan browser ke `https://unimarket.app/payment-finish?...`. Halaman itu memang tidak ada. Nantinya aplikasi Android menangkap URL ini di WebView untuk menutup halaman pembayaran.

## 7. Akses dari emulator Android

Emulator mengakses laptop lewat `http://10.0.2.2:3000`. Karena memakai HTTP (bukan HTTPS), aplikasi Android perlu mengizinkan cleartext untuk `10.0.2.2` (dikerjakan di tahap Android).
