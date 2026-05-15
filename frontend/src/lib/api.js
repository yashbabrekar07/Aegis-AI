/** Production: VITE_API_URL (Render). Local dev: Vite proxies /api → localhost:8000 */
export function apiUrl(path) {
  const p = path.startsWith('/') ? path : `/${path}`;
  const base = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
  return base ? `${base}${p}` : p;
}
