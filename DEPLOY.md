# Deploy Aegis AI (Vercel + Render + Cloudflare)

**Budget:** $0/month using free tiers (Vercel + Render free).  
**Custom domain (optional):** ~$10–15/year via Cloudflare — recommended name: **`aegisai.app`** or **`getaegis.ai`**.

Until you buy a domain, use:

| Role | URL |
|------|-----|
| Website | `https://<your-project>.vercel.app` |
| API | `https://aegis-ai-api.onrender.com` (or your Render service name) |

---

## Do NOT commit secrets

| File | Commit? |
|------|---------|
| `.env` | **Never** |
| `secret.key` | **Never** |
| `dataset.csv.enc` | **Never** (only needed to *retrain* the model) |
| `phishing_model.pkl` | **Never** — upload to Render **Secret Files** instead |

You already have a trained `phishing_model.pkl` locally. That is enough for production scans. The encrypted dataset is only required if you retrain on the server.

---

## Step 1 — Push this repo to GitHub

Commit the deployment files (`Dockerfile`, `render.yaml`, `frontend/src/lib/api.js`, etc.) and push to `main`.

---

## Step 2 — Deploy API on Render

1. Go to [render.com](https://render.com) → **New** → **Blueprint** (or **Web Service**).
2. Connect repo `ayushmore007/Aegis-AI`.
3. Use **Docker** (root `Dockerfile`).
4. Service name: `aegis-ai-api` (or any name; note the URL).
5. **Environment variables:**

   | Key | Value |
   |-----|--------|
   | `VIRUSTOTAL_API_KEY` | from your `.env` |
   | `WHISPER_MODEL` | `tiny` (required on free 512MB RAM) |
   | `ALLOWED_ORIGINS` | `https://YOUR-APP.vercel.app` (add custom domain later, comma-separated) |

6. **Secret Files** (Render dashboard → your service → **Secret Files**):

   | Filename | Local path |
   |----------|------------|
   | `phishing_model.pkl` | `backend/phishing_model.pkl` |
   | `secret.key` | `backend/secret.key` (optional if only using `.pkl`) |
   | `dataset.csv.enc` | only if you have it and plan to retrain |

7. Deploy. First build may take **15–25 min** (Whisper + dependencies).  
8. Copy your live API URL, e.g. `https://aegis-ai-api.onrender.com`.

**Note:** Free Render services **sleep after ~15 min idle**. First request after sleep can take 30–60s. Audio scan uses Whisper `tiny` + FFmpeg; if it fails, the app falls back to Google Speech.

---

## Step 3 — Deploy frontend on Vercel

1. [vercel.com](https://vercel.com) → **Add New Project** → import GitHub repo.
2. **Root Directory:** `frontend`
3. Framework: **Vite**
4. **Environment variables** (Production + Preview):

   | Key | Value |
   |-----|--------|
   | `VITE_SUPABASE_URL` | from `.env` |
   | `VITE_SUPABASE_ANON_KEY` | from `.env` |
   | `VITE_API_URL` | `https://aegis-ai-api.onrender.com` (your Render URL, no trailing slash) |

5. Deploy. Open `https://<project>.vercel.app`.

`frontend/vercel.json` already enables SPA routing for React Router.

---

## Step 4 — Supabase (auth)

In [Supabase](https://supabase.com) → **Authentication** → **URL configuration**:

| Setting | Value |
|---------|--------|
| **Site URL** | `https://<your-project>.vercel.app` |
| **Redirect URLs** | `https://<your-project>.vercel.app/**` |

If you use a custom domain later, add `https://aegisai.app/**` as well.

**Google OAuth:** In Google Cloud Console, ensure authorized origins include your Vercel URL. Supabase callback URL stays the Supabase project URL.

---

## Step 5 — Cloudflare (custom domain — optional, professional)

1. Register domain at [Cloudflare Registrar](https://www.cloudflare.com/products/registrar/) (e.g. `aegisai.app`).
2. **Website:** DNS → `CNAME` `www` → `cname.vercel-dns.com` (Vercel will show exact target in **Domains**).
3. **API (optional):** `CNAME` `api` → your Render host `aegis-ai-api.onrender.com`.
4. Update Vercel env: `VITE_API_URL=https://api.aegisai.app`
5. Update Render `ALLOWED_ORIGINS` to include `https://aegisai.app,https://www.aegisai.app`
6. Update Supabase Site URL + Redirect URLs to the new domain.

---

## Step 6 — Smoke test

1. Open Vercel URL → Login with Google.
2. **Home** → paste text → Scan.
3. Upload a short `.wav` / `.mp3` → audio scan (may be slow on first request after sleep).
4. **Dashboard** → profile loads from API.
5. **Simulate** → works offline (localStorage).

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| CORS error | Set `ALLOWED_ORIGINS` on Render to exact Vercel URL (https, no trailing slash). |
| API timeout | Render free tier waking up; retry after 60s. |
| Audio fails / OOM | Keep `WHISPER_MODEL=tiny`; upgrade Render plan later. |
| Login redirect fails | Add Vercel URL to Supabase redirect URLs. |
| Black screen | Check `VITE_SUPABASE_*` in Vercel env and redeploy. |

---

## Security reminder

Do not commit `.env`. If keys were shared in chat or screenshots, **rotate** VirusTotal and Supabase keys in their dashboards.
