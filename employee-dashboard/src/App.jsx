import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import { Camera, CheckCircle2, History, LogOut, Package, Receipt, ScanBarcode, Search, ShoppingCart, UserRound } from 'lucide-react';
import { confirmCash, loadCounterCart, loadTransactions } from './api';

const money = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(value || 0));
const when = value => value ? new Date(value).toLocaleString('en-IN') : '—';

function Brand() {
  return <div className="brand"><div className="brand-mark">I</div><div><strong>ITS3LF</strong><span>SCAN · PAY <b>&amp; GO</b></span></div></div>;
}

function Login({ onLogin }) {
  const [key, setKey] = useState('');
  return <main className="login-page"><section className="login-card"><Brand /><div className="login-icon"><ScanBarcode /></div><h1>Employee counter</h1><p>Sign in with the employee access key provided by your manager.</p><form onSubmit={e => { e.preventDefault(); if (key.trim()) onLogin(key.trim()); }}><label>Employee access key</label><input type="password" value={key} onChange={e => setKey(e.target.value)} placeholder="Enter access key" autoFocus /><button>Open dashboard</button></form></section></main>;
}

function Scanner({ onScan, busy }) {
  const [token, setToken] = useState('');
  const [cameraOpen, setCameraOpen] = useState(false);
  const [cameraError, setCameraError] = useState('');
  const cameraSession = useRef(null);

  const closeCamera = async () => {
    await cameraSession.current?.close();
    setCameraOpen(false);
  };

  useEffect(() => {
    if (!cameraOpen) return;
    let cancelled = false;
    setCameraError('');
    let scanned = false;
    const activeScanner = new Html5Qrcode('barcode-reader', {
      formatsToSupport: [
        Html5QrcodeSupportedFormats.CODE_128,
        Html5QrcodeSupportedFormats.CODE_39,
        Html5QrcodeSupportedFormats.CODE_93,
        Html5QrcodeSupportedFormats.EAN_13,
        Html5QrcodeSupportedFormats.EAN_8,
        Html5QrcodeSupportedFormats.UPC_A,
        Html5QrcodeSupportedFormats.UPC_E,
        Html5QrcodeSupportedFormats.ITF,
        Html5QrcodeSupportedFormats.CODABAR,
      ],
      verbose: false,
    });
    let closing;
    let startup;
    const session = {
      close: () => {
        cancelled = true;
        if (!closing) closing = (async () => {
          try {
            await startup;
            if (activeScanner.isScanning) await activeScanner.stop();
            activeScanner.clear();
          } catch {
            // Camera startup errors are displayed separately.
          }
        })();
        return closing;
      },
    };
    cameraSession.current = session;
    startup = Html5Qrcode.getCameras().then(cameras => {
      if (cancelled) return;
      if (!cameras.length) throw new Error('No camera was found. Connect a webcam or use the barcode input.');
      const preferred = cameras.find(camera => /back|rear|environment/i.test(camera.label)) || cameras[0];
      return activeScanner.start(preferred.id, {
        fps: 10,
        qrbox: (width, height) => ({
          width: Math.floor(width * 0.9),
          height: Math.floor(Math.min(150, height * 0.6)),
        }),
        aspectRatio: 1.333,
      }, value => {
        if (cancelled || scanned || !value.trim()) return;
        scanned = true;
        closeCamera().then(() => onScan(value.trim()));
      }, () => {});
    }).catch(error => {
      if (!cancelled) setCameraError(error?.message || 'Camera permission was denied or the camera is unavailable.');
    });
    return () => { void session.close(); };
  }, [cameraOpen]);

  return <section className="scan-card">
    <div className="scan-visual"><ScanBarcode size={54} /><span>Ready to scan</span></div>
    <div><p className="eyebrow">Billing counter</p><h2>Scan customer barcode</h2><p>Use this device's camera, a USB scanner, or paste the barcode value below.</p>
      <div className="scan-actions"><button onClick={() => setCameraOpen(true)} disabled={busy}><Camera size={18} /> Open camera</button><span>or</span><form onSubmit={e => { e.preventDefault(); if (!busy && token.trim()) onScan(token.trim()); }}><input aria-label="Customer barcode" disabled={busy} value={token} onChange={e => setToken(e.target.value)} placeholder="Scan or enter barcode" autoFocus /><button disabled={busy}>Load cart</button></form></div>
    </div>{cameraOpen && <div className="camera-modal"><div onClick={e => e.stopPropagation()}><h3>Scan customer barcode</h3><p className="camera-help">Allow camera access, then hold the customer's entire barcode horizontally inside the rectangle.</p><div id="barcode-reader" />{cameraError && <div className="camera-error">{cameraError}</div>}<button className="secondary" onClick={closeCamera}>Close camera</button></div></div>}
  </section>;
}

function Cart({ cart, onConfirm, busy }) {
  return <section className="cart-panel">
    <div className="customer-strip"><div><UserRound /><span><small>Customer</small><strong>{cart.userName || 'Customer'}</strong></span></div><div><small>User ID</small><code>{cart.userId}</code></div><div><small>Phone</small><strong>{cart.userPhone || '—'}</strong></div><div><small>Store</small><strong>{cart.storeName}</strong></div></div>
    <div className="panel-title"><div><p className="eyebrow">Order {String(cart.orderId).slice(0, 8)}</p><h2>Customer cart</h2></div><div className="item-pill"><ShoppingCart size={16} /> {cart.items?.reduce((n, item) => n + item.quantity, 0)} items</div></div>
    <div className="table-wrap"><table><thead><tr><th>Product</th><th>Barcode</th><th>MRP</th><th>Price</th><th>Qty</th><th>Total</th></tr></thead><tbody>{cart.items?.map(item => <tr key={item.barcode}><td><strong>{item.productName}</strong></td><td><code>{item.barcode}</code></td><td className="muted strike">{money(item.mrp)}</td><td>{money(item.discountPrice)}</td><td><span className="qty">{item.quantity}</span></td><td><strong>{money(item.lineTotal)}</strong></td></tr>)}</tbody></table></div>
    <div className="checkout"><div><span>Payment method</span><strong>Cash / counter payment</strong></div><div className="grand-total"><span>Amount to collect</span><strong>{money(cart.totalAmount)}</strong></div><button className="confirm" onClick={onConfirm} disabled={busy}><CheckCircle2 /> {busy ? 'Processing…' : 'Confirm payment & generate bill'}</button></div>
  </section>;
}

function Transactions({ rows, search, setSearch }) {
  const filtered = useMemo(() => rows.filter(row => JSON.stringify(row).toLowerCase().includes(search.toLowerCase())), [rows, search]);
  return <section className="history-panel"><div className="panel-title"><div><p className="eyebrow">Records</p><h2>Order history</h2></div><label className="search"><Search size={17} /><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search user, phone, bill…" /></label></div>
    <div className="table-wrap"><table><thead><tr><th>Bill</th><th>Customer</th><th>User ID</th><th>Payment</th><th>Items</th><th>Amount</th><th>Date</th></tr></thead><tbody>{filtered.map(row => <tr key={row.id}><td><strong>{row.billRef}</strong></td><td>{row.userName}<small className="block">{row.userPhone}</small></td><td><code>{String(row.userId).slice(0, 8)}…</code></td><td><span className="status">{row.paymentMethod}</span></td><td>{row.itemCount}</td><td><strong>{money(row.totalAmount)}</strong></td><td>{when(row.paidAt)}</td></tr>)}</tbody></table>{!filtered.length && <div className="empty"><Receipt /><p>No transactions found</p></div>}</div>
  </section>;
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(Boolean(sessionStorage.getItem('employeeKey')));
  const [tab, setTab] = useState('counter'); const [cart, setCart] = useState(null); const [bill, setBill] = useState(null);
  const [rows, setRows] = useState([]); const [search, setSearch] = useState(''); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const refresh = async () => { try { setRows(await loadTransactions()); } catch (e) { setError(e.message); } };
  useEffect(() => { if (loggedIn) refresh(); }, [loggedIn]);
  const scan = async token => { setBusy(true); setError(''); setBill(null); try { const data = await loadCounterCart(token); setCart({ ...data, qrToken: token }); } catch (e) { setError(e.message); setCart(null); } finally { setBusy(false); } };
  const pay = async () => { setBusy(true); setError(''); try { setBill(await confirmCash(cart.qrToken)); setCart(null); refresh(); } catch (e) { setError(e.message); } finally { setBusy(false); } };
  if (!loggedIn) return <Login onLogin={key => { sessionStorage.setItem('employeeKey', key); setLoggedIn(true); }} />;
  return <div className="app"><aside><Brand /><nav><button className={tab === 'counter' ? 'active' : ''} onClick={() => setTab('counter')}><ScanBarcode /> Counter scanner</button><button className={tab === 'history' ? 'active' : ''} onClick={() => { setTab('history'); refresh(); }}><History /> Order history</button></nav><div className="sidebar-note"><Package /><p><strong>Counter online</strong><span>Ready for customers</span></p></div><button className="logout" onClick={() => { sessionStorage.clear(); setLoggedIn(false); }}><LogOut /> Sign out</button></aside>
    <main className="content"><header><div><p className="eyebrow">Employee workspace</p><h1>{tab === 'counter' ? 'Checkout counter' : 'Transaction history'}</h1></div><div className="online"><i /> System online</div></header>{error && <div className="alert">{error}<button onClick={() => setError('')}>×</button></div>}
      {bill && <div className="success-banner"><CheckCircle2 /><div><strong>Payment confirmed</strong><span>Bill {bill.billRef} generated · {money(bill.totalAmount)}</span></div><button onClick={() => window.print()}><Receipt /> Print bill</button></div>}
      {tab === 'counter' ? <>{!cart && <Scanner onScan={scan} busy={busy} />}{cart && <Cart cart={cart} onConfirm={pay} busy={busy} />}{cart && <button className="text-button" onClick={() => setCart(null)}>Cancel and scan another barcode</button>}</> : <Transactions rows={rows} search={search} setSearch={setSearch} />}
    </main></div>;
}
