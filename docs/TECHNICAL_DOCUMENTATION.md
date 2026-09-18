# DropShare — Technical Architecture & Implementation Documentation

## 1. Overview & System Architecture

**DropShare** is a lightweight, zero-cloud, peer-to-peer (P2P) file sharing Android application built with **Jetpack Compose** and **Kotlin**. It enables devices connected to the same local Wi-Fi / Ethernet network to share files directly via HTTP without requiring external servers or internet access.

### Core Architecture Pattern
DropShare follows the **Single Source of Truth** state management pattern:
- **`TransferUiState`** (`com.basavaraj.dropshare.model.TransferState.kt`): Holds all UI and transfer metadata (`phase`, `fileName`, `fileSizeBytes`, `bytesTransferred`, `transferSpeedBytesPerSec`, `etaSeconds`, `localIpAddress`, `qrCodeBitmap`, `downloadedUri`, etc.).
- **`TransferPhase`**: Explicit state machine enum driving UI transitions (`Idle`, `Selecting`, `Ready`, `Connecting`, `Waiting`, `Transferring`, `Completed`, `Failed`, `Cancelled`).
- **`Destination`**: Screen navigation enum (`Home`, `Send`, `Receive`).

---

## 2. Design System & UI Components

The app features a custom Material 3 dark design system that enforces strict design tokens across all screens.

### Design Tokens
- **`DropShareColors`** (`ui/theme/Color.kt`):
  - Background: `#0B0D10` (dark near-black)
  - Primary Mint: `#6EE7B7` (action button fill)
  - OnPrimary: `#0B0D10` (high-contrast text on mint buttons)
  - Surface Containers: `#14171C` and `#1B1F26`
  - Success/Warning/Error: `#4ADE80` / `#FBBF24` / `#F87171`
- **`Spacing`** (`ui/theme/Dimens.kt`): Standardized 4dp scale (`xs: 4dp`, `sm: 8dp`, `md: 12dp`, `lg: 16dp`, `xl: 24dp`, `xxl: 32dp`).
- **`Radius`** (`ui/theme/Dimens.kt`): Button radius (`12dp`), Card radius (`16dp`).
- **`DropShareType`** (`ui/theme/Type.kt`): Responsive typography scale in `sp` units to respect system dynamic font sizing.
- **`MinTouchTarget`**: Enforces `48.dp` minimum touch targets for accessibility.

### Component Layer (`ui/components/`)
1. **`PrimaryButton`**: High-priority filled mint button with embedded loading spinner support.
2. **`SecondaryButton`**: Outlined secondary action button.
3. **`FileInfo`**: Plain metadata row displaying filename, extension badge, and formatted size.
4. **`ConnectionStatus`**: Status indicator with colored dot (`Offline`, `Ready`, `Connected`), status label, and an optional manual refresh icon button.
5. **`QrCodeCard`**: Elevated surface card rendering ZXing QR code bitmap and connection address.
6. **`TransferProgress`**: Live progress bar (`LinearProgressIndicator`), percentage, ETA, and throughput speed (`MB/s`).
7. **`EmptyState`** & **`ErrorMessage`**: Contextual placeholders and error recovery prompt cards.

---

## 3. Local HTTP Server Engine (`FileServer.kt`)

The sender side embeds a raw Java `ServerSocket` to serve files directly over HTTP.

### Implementation Details
- **Port**: Listens on TCP port `8080`.
- **HTTP Response Headers**:
  ```http
  HTTP/1.1 200 OK
  Content-Type: <detected MIME type>
  Content-Length: <file size in bytes>
  Content-Disposition: attachment; filename="<filename>"
  Connection: close
  ```
- **Byte Stream Encoding Fix**: Enclosed multi-line string concatenations in parentheses `("HTTP/1.1 200 OK...\r\n\r\n").toByteArray()` before invocation to prevent Kotlin operator precedence from evaluating string concatenation as `String + ByteArray`.
- **Thread & Exception Safety**: `FileServer.start()` runs on a dedicated background thread. Closing `serverSocket` via `FileServer.stop()` interrupts the blocking `accept()` loop cleanly without throwing unhandled `SocketException` stack traces.

---

## 4. File Downloader Engine & Storage Integration

The receiver side connects to the sender's local HTTP server to download files directly into the Android device storage.

### Download Logic (`downloadFile` & `saveStreamToDownloads`)
1. **HTTP Connection**: Initiates an `HttpURLConnection` GET request with a 10s connect timeout and 30s read timeout.
2. **Auto-Formatting**: Automatically prefixes `http://` if the user enters an IP address without a scheme (e.g. `192.168.1.5:8080`).
3. **Filename Parsing**: Parses `Content-Disposition` header regex (`filename="?([^";]+)"?`) to restore original filenames.
4. **Streaming & Speed Tracking**: Reads the input stream in `8192` byte chunks, calculating live progress percentage, download speed (`bytes / elapsed_seconds`), and remaining ETA (`seconds`).
5. **Storage API Bridge**:
   - **Android 10+ (API 29+)**: Uses `ContentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)` to insert natively into the system Downloads collection.
   - **Legacy (API < 29)**: Falls back to `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)`.
6. **File Viewer (`openDownloadedFile`)**: Uses `androidx.core.content.FileProvider` (`FileProvider.getUriForFile`) with `Intent.ACTION_VIEW` and `FLAG_GRANT_READ_URI_PERMISSION` so received files can be opened instantly in external viewers.

---

## 5. Dynamic Network Monitoring & Auto-Sync

To ensure the user always knows whether the device is ready to share, network connectivity is monitored in real-time.

### Network Monitoring
- **`ConnectivityManager.NetworkCallback`**: Listens for network availability (`onAvailable`, `onLost`, `onCapabilitiesChanged`) using transport constraints (`TRANSPORT_WIFI`, `TRANSPORT_ETHERNET`).
- **Real-Time Auto-Sync**: Automatically updates `localIpAddress` state when Wi-Fi is toggled ON or OFF in Android system settings.
- **Manual Sync Icon**: A refresh icon button (`Icons.Default.Refresh`) on the main screen allows manual network re-checking.

---

## 6. Navigation & Back-Press State Machine

To prevent accidental app exits during file selection or active transfers, back-press handling is explicitly managed at the Activity level.

### Navigation Rules
- **`BackHandler` at Activity Level**: Registered directly in `MainActivity.onCreate()` using `onBackPressedDispatcher` and updated via `DisposableEffect(destination)`.
- **Behavior**:
  - When on `Destination.Send` or `Destination.Receive`: Pressing the system back button or back gesture resets transfer state, safely stops any running server, and returns to `Destination.Home`.
  - When on `Destination.Home`: Allows standard system back action to exit the app.
- **Top Bar Back Arrow**: Top-left back icon button (`Icons.AutoMirrored.Filled.ArrowBack`) added to `SendScreen` and `ReceiveScreen`.

---

## 7. Android Manifest & Security Configurations

`AndroidManifest.xml` includes all required permissions and provider declarations:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<application
    android:usesCleartextTraffic="true" ...>
    
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.provider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />
    </provider>
</application>
```

- **`android:usesCleartextTraffic="true"`**: Mandatory for Android 9+ (API 28+) to allow local unencrypted `http://192.168.x.x:8080` connections.
- **`FileProvider`**: Points to `res/xml/file_paths.xml` allowing secure URI sharing with external file viewing apps.

---

## 8. Summary of Major Bug Fixes & Refactorings

1. **Fixed HTTP Header Precedence Compilation Error**: Enclosed header string concatenation in `(...)` before invoking `.toByteArray()`.
2. **Fixed Server Lifecycle Memory Leak**: Moved `FileServer` instance into Compose state (`var server by remember { mutableStateOf<FileServer?>(null) }`) so `server.stop()` can be called when stopping or selecting new files.
3. **Fixed Composable Invocation in Listener Error**: Removed `@Composable` UI code (`Spacer`, `Image`, `Text`) from button `onClick` handlers and placed them in the composable tree.
4. **Added Complete Receive Flow**: Created `ReceiveScreen` with address input, paste shortcut, HTTP downloader, progress calculation, and MediaStore saving.
5. **Fixed Back-Press App Exit Bug**: Implemented activity-level `OnBackPressedCallback` in `MainActivity` to intercept back gestures and navigate smoothly to `HomeScreen`.
6. **Added Real-Time Network Auto-Sync**: Implemented `ConnectivityManager.NetworkCallback` and manual refresh button to handle Wi-Fi toggling seamlessly.
