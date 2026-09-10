const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function request(path, options = {}) {
  const key = sessionStorage.getItem('employeeKey');
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', 'X-Admin-Key': key || '', ...options.headers },
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.message || 'Request failed');
  return payload.data;
}

export const loadCounterCart = token => request(`/api/v1/employee/counter-cart/${encodeURIComponent(token)}`);
export const confirmCash = token => request('/api/v1/employee/confirm-cash', {
  method: 'POST', body: JSON.stringify({ qrToken: token }),
});
export const loadTransactions = () => request('/api/v1/employee/transactions?size=100');
