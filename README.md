# DropShare

**Zero‑cloud, peer‑to‑peer file sharing over local Wi‑Fi / Ethernet.**  
Send files from your Android phone to any device on the same network — laptops, tablets, other phones — without cables, cloud storage, or internet access.

---

## 📦 What works today

| Direction | How to receive |
|-----------|----------------|
| **Phone → Laptop / any browser** | Open `http://<phone-IP>:8080` in any desktop browser (Chrome, Edge, Safari, Firefox). The file downloads to your OS default download folder. |
| **Phone → Phone** | Use the Send screen → pick a file → QR code or IP address → receiver opens the same URL. |

**Typical flow:**
1. Open the app → **Send** → select a file → tap **Start Sharing**.
2. The app shows a QR code + the address `http://192.168.x.x:8080`.
3. On the laptop, open that URL in a browser → the file begins downloading.

The app also lets you **copy the URL** to the clipboard from the Send screen.

---

## ⚙️ Building / Running

| Step | Command |
|------|---------|
| **Open in Android Studio** | `android studio .` |
| **Connect a device** | Enable USB debugging, or use an emulator with `minSdk` ≥ 26. |
| **Run** | `Run > app` (or `./gradlew installDebug` then `adb install ...`) |

**Environment prerequisites:**
- Gradle 9.6.0 (wrapper is included)
- JDK 17 (compatible with AGP 9.4.0 + Kotlin 2.2.10)
- Android SDK 37 (target) / 26 (min)

**Project structure:**
```
app/
├── src/main/java/com/basavaraj/dropshare/
│   ├── MainActivity.kt        ← all wiring, HTTP downloader, QR generation, network monitor
│   ├── FileServer.kt          ← raw ServerSocket HTTP file server (port 8080)
│   └── model/TransferState.kt ← TransferPhase, ErrorReason, TransferUiState
├── src/main/res/              ← Compose UI, themes, strings, icons
├── src/main/AndroidManifest.xml
└── build.gradle.kts
```

**Dependencies (from `build.gradle.kts`):**
- `com.google.zxing:core:3.5.3` — QR code encoding
- `androidx.compose.material3` — UI toolkit
- `androidx.activity:activity-compose` — activity-lifecycle + `rememberLauncherForActivityResult`
- `androidx.lifecycle:lifecycle-runtime-ktx` — `DisposableEffect`

---

## 🧩 Architecture (one‑sentence summary)

State lives entirely in **`TransferUiState`** (`model/TransferState.kt`) as a data class + `TransferPhase` enum; the UI reads it via Compose `remember { mutableStateOf(...) }` in `MainActivity`. Network discovery uses `ConnectivityManager.NetworkCallback`; the HTTP file server is a hand‑rolled `ServerSocket` on port 8080; received files land via `MediaStore.Downloads` (Android 10+) or `Environment.DIRECTORY_DOWNLOADS` fallback.

---

## 🚧 Known limitations & roadmap (what's missing)

| Feature | Status | Notes |
|---------|--------|-------|
| **Phone → Laptop download** | ✅ Working | Browser opens `http://<IP>:8080`, file downloads to OS downloads folder. |
| **Phone → Laptop upload** | ❌ Not yet | `FileServer` only serves files outbound; no `PUT` / HTML dashboard built yet. |
| **Laptop → Phone upload** | ❌ Not yet | Would need `PUT /upload` endpoint + HTML upload UI; browser‑based zero‑install. |
| **Session code / auth** | ❌ Not yet | Anyone on the LAN can reach `:8080`. |
| **mDNS (`dropshare.local`)** | ❌ Not yet | Zero‑IP typing would require `NsdManager` / `.local` resolution. |
| **AP isolation handling** | ⚠️ Manual hotspot | If guest Wi‑Fi blocks client‑to‑client, turn the phone into a hotspot and connect the laptop to it. |
| **Large‑file resume / Range requests** | ❌ Not yet | Server serves whole file in one `GET`. |
| **Progress for sender** | ❌ Dead code | `SendScreen` has `TransferPhase.Transferring` UI but the server never reports back, so that branch never fires. |

---

## 🛠️ Development notes for contributors

- **Don't edit `FileServer.kt`'s header parsing** if you only need outbound downloads — it works for `GET /` today.
- **If adding upload**, restructure `handleClient()` to read headers as raw bytes, extract `Content-Length`, then read exactly that many bytes from the raw `InputStream` (avoid `bufferedReader()` which swallows body bytes).
- **`saveStreamToDownloads()`** in `MainActivity.kt:470` already bridges to `MediaStore.Downloads` on API 29+ and falls back to `DIRECTORY_DOWNLOADS`. Reuse it for uploads.
- **Network callback** (`DisposableEffect` on line 86 of `MainActivity.kt`) auto‑updates `localIpAddress` when Wi‑Fi toggles. It's guarded by a `try/catch`; if you add auth or mDNS, keep the callback alive.
- **Back‑press handling** (`OnBackPressedCallback` at line 63) resets state to Home. Works as designed.

---

## 📄 License

No license file is present in the root. The code was written as a personal project; treat it as private unless you add a LICENSE.

---

*Generated from the codebase on 2025‑08‑24.*