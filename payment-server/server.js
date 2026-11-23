import 'dotenv/config';
import express from 'express';

const SNAP_URL = 'https://app.sandbox.midtrans.com/snap/v1/transactions';
const CORE_API_BASE = 'https://api.sandbox.midtrans.com/v2';
const FINISH_URL = 'https://unimarket.app/payment-finish';

// Aturan order_id Midtrans: huruf, angka, '-', '_', '~', '.', maksimal 50 karakter.
const ORDER_ID_PATTERN = /^[A-Za-z0-9\-_~.]{1,50}$/;
const MAX_ITEM_NAME = 50;

const SERVER_KEY = process.env.MIDTRANS_SERVER_KEY?.trim();
const PORT = Number(process.env.PORT) || 3000;

if (!SERVER_KEY || SERVER_KEY.includes('GANTI_DENGAN')) {
  console.error('MIDTRANS_SERVER_KEY belum diisi di file .env. Lihat .env.example.');
  process.exit(1);
}
if (!SERVER_KEY.startsWith('SB-')) {
  console.warn('Peringatan: Server Key tidak diawali "SB-". Pastikan memakai key mode Sandbox.');
}

// Header Authorization dihitung sekali; nilainya tidak pernah dicetak ke log.
const AUTH_HEADER = 'Basic ' + Buffer.from(`${SERVER_KEY}:`).toString('base64');

const midtransHeaders = {
  Authorization: AUTH_HEADER,
  Accept: 'application/json',
  'Content-Type': 'application/json',
};

class HttpError extends Error {
  constructor(status, message, details) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

function validateOrderId(orderId) {
  if (typeof orderId !== 'string' || orderId.trim() === '') {
    throw new HttpError(400, 'orderId wajib diisi');
  }
  if (!ORDER_ID_PATTERN.test(orderId)) {
    throw new HttpError(
      400,
      'orderId hanya boleh berisi huruf, angka, -, _, ~, . dan maksimal 50 karakter',
    );
  }
}

function validatePaymentBody(body) {
  if (!body || typeof body !== 'object') {
    throw new HttpError(400, 'Body harus berupa JSON');
  }
  const { orderId, amount, itemName, customerName, customerEmail } = body;

  validateOrderId(orderId);

  if (typeof amount !== 'number' || !Number.isInteger(amount) || amount <= 0) {
    throw new HttpError(400, 'amount wajib berupa bilangan bulat lebih dari 0');
  }
  if (typeof itemName !== 'string' || itemName.trim() === '') {
    throw new HttpError(400, 'itemName wajib diisi');
  }
  if (customerName !== undefined && typeof customerName !== 'string') {
    throw new HttpError(400, 'customerName harus berupa string');
  }
  if (customerEmail !== undefined && typeof customerEmail !== 'string') {
    throw new HttpError(400, 'customerEmail harus berupa string');
  }

  return {
    orderId,
    amount,
    itemName: itemName.trim(),
    customerName: customerName?.trim() || undefined,
    customerEmail: customerEmail?.trim() || undefined,
  };
}

// Status HTTP yang dikembalikan ke klien saat Midtrans gagal.
// 401/403 berarti Server Key di .env salah: itu masalah konfigurasi server, bukan klien.
function upstreamErrorStatus(code) {
  if (code === 401 || code === 403) return 502;
  return code >= 400 && code < 500 ? code : 502;
}

function upstreamErrorMessage(code, fallback) {
  if (code === 401 || code === 403) {
    return 'Midtrans menolak Server Key. Periksa MIDTRANS_SERVER_KEY di .env (harus key Sandbox)';
  }
  return fallback;
}

async function callMidtrans(url, options) {
  let response;
  try {
    response = await fetch(url, { ...options, headers: midtransHeaders });
  } catch (err) {
    throw new HttpError(502, 'Gagal menghubungi Midtrans', err.message);
  }
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = { raw: text };
  }
  return { status: response.status, data };
}

const app = express();
app.use(express.json());

app.use((req, _res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.originalUrl}`);
  next();
});

app.get('/health', (_req, res) => {
  res.json({ ok: true });
});

app.post('/payments', async (req, res, next) => {
  try {
    const p = validatePaymentBody(req.body);

    const customerDetails = {};
    if (p.customerName) customerDetails.first_name = p.customerName;
    if (p.customerEmail) customerDetails.email = p.customerEmail;

    const payload = {
      transaction_details: {
        order_id: p.orderId,
        gross_amount: p.amount,
      },
      item_details: [
        {
          id: p.orderId,
          price: p.amount,
          quantity: 1,
          name: p.itemName.slice(0, MAX_ITEM_NAME),
        },
      ],
      customer_details: customerDetails,
      callbacks: {
        finish: FINISH_URL,
      },
    };

    const { status, data } = await callMidtrans(SNAP_URL, {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    if (status < 200 || status >= 300 || !data?.token) {
      // 4xx dari Midtrans (mis. order_id sudah dipakai) diteruskan,
      // selain itu dianggap kegagalan upstream (502).
      throw new HttpError(
        upstreamErrorStatus(status),
        upstreamErrorMessage(status, 'Midtrans menolak pembuatan transaksi'),
        data?.error_messages ?? data,
      );
    }

    console.log(`Transaksi Snap dibuat untuk order ${p.orderId}`);
    res.status(201).json({ token: data.token, redirectUrl: data.redirect_url });
  } catch (err) {
    next(err);
  }
});

app.get('/payments/:orderId/status', async (req, res, next) => {
  try {
    const { orderId } = req.params;
    validateOrderId(orderId);

    const { status, data } = await callMidtrans(
      `${CORE_API_BASE}/${encodeURIComponent(orderId)}/status`,
      { method: 'GET' },
    );

    // Midtrans bisa memberi 404 lewat HTTP status ATAU lewat field status_code
    // di body (HTTP 200). Keduanya berarti user belum memilih metode bayar.
    const bodyCode = data?.status_code ? String(data.status_code) : null;
    if (status === 404 || bodyCode === '404') {
      return res.json({ orderId, transactionStatus: 'pending', paymentType: null });
    }

    const bodyFailed = bodyCode !== null && !bodyCode.startsWith('2');
    if (status < 200 || status >= 300 || bodyFailed) {
      const code = Number(bodyCode) || status;
      throw new HttpError(
        upstreamErrorStatus(code),
        upstreamErrorMessage(code, 'Gagal mengambil status transaksi dari Midtrans'),
        data?.status_message ?? data,
      );
    }

    res.json({
      orderId: data.order_id ?? orderId,
      transactionStatus: data.transaction_status ?? 'unknown',
      paymentType: data.payment_type ?? null,
    });
  } catch (err) {
    next(err);
  }
});

app.use((_req, res) => {
  res.status(404).json({ error: 'Endpoint tidak ditemukan' });
});

// eslint-disable-next-line no-unused-vars
app.use((err, _req, res, _next) => {
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: 'Body bukan JSON yang valid' });
  }
  const status = err instanceof HttpError ? err.status : 500;
  const body = { error: status === 500 ? 'Terjadi kesalahan di server' : err.message };
  if (err.details !== undefined) body.details = err.details;
  console.error(`Error ${status}: ${err.message}`);
  res.status(status).json(body);
});

app.listen(PORT, () => {
  console.log(`payment-server berjalan di http://localhost:${PORT}`);
  console.log(`Dari emulator Android: http://10.0.2.2:${PORT}`);
});

export default app;
