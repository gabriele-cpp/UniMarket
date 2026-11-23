const SNAP_URL = 'https://app.sandbox.midtrans.com/snap/v1/transactions';
const CORE_API_BASE = 'https://api.sandbox.midtrans.com/v2';
const FINISH_URL = 'https://unimarket.app/payment-finish';
const ORDER_ID_PATTERN = /^[A-Za-z0-9\-_~.]{1,50}$/;

class HttpError extends Error {
  constructor(status, message, details) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
  });
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
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
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
    itemName: itemName.trim().slice(0, 50),
    customerName: customerName?.trim() || undefined,
    customerEmail: customerEmail?.trim() || undefined,
  };
}

function midtransHeaders(serverKey) {
  return {
    Authorization: `Basic ${btoa(`${serverKey}:`)}`,
    Accept: 'application/json',
    'Content-Type': 'application/json',
  };
}

async function callMidtrans(url, options, headers) {
  let response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch {
    throw new HttpError(502, 'Gagal menghubungi Midtrans');
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

function upstreamErrorStatus(code) {
  if (code === 401 || code === 403) return 502;
  return code >= 400 && code < 500 ? code : 502;
}

function upstreamErrorMessage(code, fallback) {
  if (code === 401 || code === 403) {
    return 'Midtrans menolak Server Key. Periksa MIDTRANS_SERVER_KEY (harus key Sandbox)';
  }
  return fallback;
}

async function createPayment(request, headers) {
  let body;
  try {
    body = await request.json();
  } catch {
    throw new HttpError(400, 'Body bukan JSON yang valid');
  }
  const payment = validatePaymentBody(body);
  const customerDetails = {};
  if (payment.customerName) customerDetails.first_name = payment.customerName;
  if (payment.customerEmail) customerDetails.email = payment.customerEmail;

  const payload = {
    transaction_details: {
      order_id: payment.orderId,
      gross_amount: payment.amount,
    },
    item_details: [{
      id: payment.orderId,
      price: payment.amount,
      quantity: 1,
      name: payment.itemName,
    }],
    customer_details: customerDetails,
    callbacks: { finish: FINISH_URL },
  };

  const { status, data } = await callMidtrans(SNAP_URL, {
    method: 'POST',
    body: JSON.stringify(payload),
  }, headers);

  if (status < 200 || status >= 300 || !data?.token) {
    throw new HttpError(
      upstreamErrorStatus(status),
      upstreamErrorMessage(status, 'Midtrans menolak pembuatan transaksi'),
      data?.error_messages ?? data,
    );
  }
  return json({ token: data.token, redirectUrl: data.redirect_url }, 201);
}

async function getPaymentStatus(orderId, headers) {
  validateOrderId(orderId);
  const { status, data } = await callMidtrans(
    `${CORE_API_BASE}/${encodeURIComponent(orderId)}/status`,
    { method: 'GET' },
    headers,
  );

  const bodyCode = data?.status_code ? String(data.status_code) : null;
  if (status === 404 || bodyCode === '404') {
    return json({ orderId, transactionStatus: 'pending', paymentType: null });
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
  return json({
    orderId: data.order_id ?? orderId,
    transactionStatus: data.transaction_status ?? 'unknown',
    paymentType: data.payment_type ?? null,
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const method = request.method.toUpperCase();

    if (method === 'GET' && url.pathname === '/health') {
      return json({ ok: true });
    }

    if (!env.MIDTRANS_SERVER_KEY || !env.MIDTRANS_SERVER_KEY.startsWith('SB-')) {
      return json({ error: 'MIDTRANS_SERVER_KEY Sandbox belum dikonfigurasi' }, 503);
    }

    const headers = midtransHeaders(env.MIDTRANS_SERVER_KEY);
    try {
      if (method === 'POST' && url.pathname === '/payments') {
        return await createPayment(request, headers);
      }

      const match = url.pathname.match(/^\/payments\/([^/]+)\/status$/);
      if (method === 'GET' && match) {
        return await getPaymentStatus(decodeURIComponent(match[1]), headers);
      }
      return json({ error: 'Endpoint tidak ditemukan' }, 404);
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500;
      const response = { error: status === 500 ? 'Terjadi kesalahan di server' : error.message };
      if (error.details !== undefined) response.details = error.details;
      return json(response, status);
    }
  },
};
