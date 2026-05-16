# Aegis AI — Android app

**Package:** `com.aegisai.app`  
**Scope C:** Scan (text + audio), Vishing (transcript + record), Training sim, Profile, Google + email login.

> The old `android/` folder was **not** in git stash (only local WIP was stashed). This is a **new** project matching your live API.

---

## 1. Configure secrets

Edit `android/gradle.properties` (do **not** commit real keys to public repos):

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=your_anon_key
API_BASE_URL=https://aegis-ai-xxg2.onrender.com
```

For **emulator → PC backend**: `API_BASE_URL=http://10.0.2.2:8000`

---

## 2. Open in Android Studio

1. **File → Open** → select the `android` folder (not the repo root).
2. Wait for **Gradle Sync** to finish.
3. If prompted, install **SDK 34** and **JDK 17**.

---

## 3. Google Sign-In setup

### Supabase

**Authentication → URL configuration → Redirect URLs:**

```
com.aegisai.app://auth-callback
```

### Google Cloud (same project as Supabase Google provider)

Create **Android** OAuth client:

- Package: `com.aegisai.app`
- SHA-1: from step 4 below

---

## 4. Get SHA-1 (debug keystore)

In PowerShell:

```powershell
cd $env:USERPROFILE\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the **SHA1** line into Google Cloud Console.

---

## 5. Run the app

- **Emulator:** Run ▶ on `app` — API URL `https://aegis-ai-xxg2.onrender.com` works as-is.
- **Physical phone:** Same Wi‑Fi as PC; use Render URL, or PC LAN IP if you run backend locally (`http://192.168.x.x:8000` — may need backend `ALLOWED_ORIGINS`).

### Install debug APK

```text
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Change app icon later

Replace:

- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Or add PNGs under `mipmap-*` and update `ic_launcher.xml`

Adaptive icon background color: `@color/aegis_green_dark`.

---

## 7. Backend note (vishing transcript)

Deploy latest `backend/main.py` so `/api/vishing/analyze-transcript` exists on Render (added for mobile transcript analysis).

---

## Tabs

| Tab | Feature |
|-----|---------|
| Scan | Text + pick audio file |
| Vishing | Transcript analyze + record & upload |
| Training | Local scam/safe quiz + XP |
| Profile | Username, email prefix, user id, logout |
