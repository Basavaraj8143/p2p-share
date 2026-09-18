package com.basavaraj.dropshare

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.basavaraj.dropshare.model.ErrorReason
import com.basavaraj.dropshare.model.TransferPhase
import com.basavaraj.dropshare.model.TransferUiState
import com.basavaraj.dropshare.ui.screens.HomeScreen
import com.basavaraj.dropshare.ui.screens.ReceiveScreen
import com.basavaraj.dropshare.ui.screens.SendScreen
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

private enum class Destination { Home, Send, Receive }

class MainActivity : ComponentActivity() {

    private var onBackPressedHandler: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handled = onBackPressedHandler?.invoke() ?: false
                if (!handled) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            DropShareTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DropShareColors.Background) {
                    val context = LocalContext.current
                    val clipboardManager = LocalClipboardManager.current

                    var destination by remember { mutableStateOf(Destination.Home) }
                    var sendState by remember { mutableStateOf(TransferUiState()) }
                    var receiveState by remember { mutableStateOf(TransferUiState()) }
                    var server by remember { mutableStateOf<FileServer?>(null) }
                    var localIpAddress by remember { mutableStateOf(getLocalIpAddress()) }

                    // Dynamic Network Observer for auto-syncing network state when Wi-Fi is toggled
                    DisposableEffect(context) {
                        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager

                        val networkCallback = object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network) {
                                localIpAddress = getLocalIpAddress()
                            }

                            override fun onLost(network: Network) {
                                localIpAddress = getLocalIpAddress()
                            }

                            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                                localIpAddress = getLocalIpAddress()
                            }
                        }

                        val networkRequest = NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                            .build()

                        try {
                            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        onDispose {
                            try {
                                connectivityManager?.unregisterNetworkCallback(networkCallback)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    // Intercept back navigation at activity level
                    DisposableEffect(destination) {
                        onBackPressedHandler = {
                            if (destination != Destination.Home) {
                                if (destination == Destination.Send) {
                                    val currentServer = server
                                    Thread { currentServer?.stop() }.start()
                                    server = null
                                    sendState = TransferUiState()
                                } else if (destination == Destination.Receive) {
                                    receiveState = TransferUiState()
                                }
                                destination = Destination.Home
                                true // Handled back press! Transition to Home
                            } else {
                                false // On Home screen: allow system back to exit app
                            }
                        }
                        onDispose {
                            onBackPressedHandler = null
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            val currentServer = server
                            Thread { currentServer?.stop() }.start()
                        }
                    }

                    val filePicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            context.contentResolver.query(
                                uri,
                                arrayOf(
                                    OpenableColumns.DISPLAY_NAME,
                                    OpenableColumns.SIZE
                                ),
                                null,
                                null,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                                    val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                                        cursor.getString(nameIndex)
                                    } else {
                                        "file"
                                    }

                                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                                        cursor.getLong(sizeIndex)
                                    } else {
                                        0L
                                    }

                                    val fileExt = name.substringAfterLast('.', "FILE").uppercase()

                                    sendState = sendState.copy(
                                        phase = TransferPhase.Ready,
                                        fileName = name,
                                        fileType = fileExt,
                                        fileSizeBytes = size,
                                        selectedUri = uri
                                    )
                                }
                            }
                        } else {
                            if (sendState.phase == TransferPhase.Selecting) {
                                sendState = sendState.copy(phase = TransferPhase.Idle)
                            }
                        }
                    }

                    val isOnLocalNetwork = localIpAddress != "Unknown IP"

                    when (destination) {
                        Destination.Home -> HomeScreen(
                            isOnLocalNetwork = isOnLocalNetwork,
                            onRefreshNetwork = {
                                localIpAddress = getLocalIpAddress()
                            },
                            onSendClick = {
                                destination = Destination.Send
                                sendState = TransferUiState()
                            },
                            onReceiveClick = {
                                destination = Destination.Receive
                                receiveState = TransferUiState()
                            },
                        )

                        Destination.Send -> SendScreen(
                            state = sendState,
                            onBackClick = {
                                val currentServer = server
                                Thread { currentServer?.stop() }.start()
                                server = null
                                sendState = TransferUiState()
                                destination = Destination.Home
                            },
                            onPickFile = {
                                sendState = sendState.copy(phase = TransferPhase.Selecting)
                                filePicker.launch("*/*")
                            },
                            onStartSharing = {
                                val uri = sendState.selectedUri
                                if (uri != null && sendState.fileName != null) {
                                    val serverInstance = FileServer(
                                        fileName = sendState.fileName!!,
                                        fileSize = sendState.fileSizeBytes ?: 0L
                                    ) {
                                        context.contentResolver.openInputStream(uri)
                                    }

                                    server = serverInstance
                                    Thread { serverInstance.start() }.start()

                                    val ipAddress = getLocalIpAddress()
                                    val addressString = "http://$ipAddress:8080"
                                    val qrBitmap = generateQrCode(addressString)?.asImageBitmap()

                                    sendState = sendState.copy(
                                        phase = TransferPhase.Waiting,
                                        localIpAddress = ipAddress,
                                        port = 8080,
                                        qrCodeBitmap = qrBitmap
                                    )
                                }
                            },
                            onStopSharing = {
                                val currentServer = server
                                Thread { currentServer?.stop() }.start()
                                server = null
                                sendState = TransferUiState()
                                destination = Destination.Home
                            },
                            onCancelTransfer = {
                                val currentServer = server
                                Thread { currentServer?.stop() }.start()
                                server = null
                                sendState = sendState.copy(phase = TransferPhase.Cancelled)
                            },
                            onRetry = {
                                sendState = TransferUiState(phase = TransferPhase.Ready, selectedUri = sendState.selectedUri, fileName = sendState.fileName, fileType = sendState.fileType, fileSizeBytes = sendState.fileSizeBytes)
                            },
                            onDone = {
                                val currentServer = server
                                Thread { currentServer?.stop() }.start()
                                server = null
                                sendState = TransferUiState()
                                destination = Destination.Home
                            },
                        )

                        Destination.Receive -> ReceiveScreen(
                            state = receiveState,
                            onBackClick = {
                                receiveState = TransferUiState()
                                destination = Destination.Home
                            },
                            onAddressChange = { address ->
                                receiveState = receiveState.copy(enteredAddress = address)
                            },
                            onPasteClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) {
                                    receiveState = receiveState.copy(enteredAddress = clip.text)
                                }
                            },
                            onConnectClick = {
                                val address = receiveState.enteredAddress
                                if (address.isNotBlank()) {
                                    var startTime = System.currentTimeMillis()
                                    downloadFile(
                                        context = context,
                                        urlString = address,
                                        onStatusChange = { status ->
                                            when (status) {
                                                DownloadStatus.Connecting -> {
                                                    startTime = System.currentTimeMillis()
                                                    receiveState = receiveState.copy(phase = TransferPhase.Connecting)
                                                }
                                                is DownloadStatus.Downloading -> {
                                                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                                                    val speed = if (elapsed > 0) (status.bytesDownloaded / elapsed).toLong() else 0L
                                                    val remainingBytes = status.totalBytes - status.bytesDownloaded
                                                    val eta = if (speed > 0) remainingBytes / speed else null

                                                    val ext = status.fileName.substringAfterLast('.', "FILE").uppercase()

                                                    receiveState = receiveState.copy(
                                                        phase = TransferPhase.Transferring,
                                                        fileName = status.fileName,
                                                        fileType = ext,
                                                        fileSizeBytes = status.totalBytes,
                                                        bytesTransferred = status.bytesDownloaded,
                                                        transferSpeedBytesPerSec = speed,
                                                        etaSeconds = eta
                                                    )
                                                }
                                                is DownloadStatus.Success -> {
                                                    receiveState = receiveState.copy(
                                                        phase = TransferPhase.Completed,
                                                        fileName = status.fileName,
                                                        downloadedUri = status.uri
                                                    )
                                                }
                                                is DownloadStatus.Error -> {
                                                    receiveState = receiveState.copy(
                                                        phase = TransferPhase.Failed,
                                                        error = ErrorReason.ConnectFailed
                                                    )
                                                }
                                                DownloadStatus.Idle -> {
                                                    receiveState = receiveState.copy(phase = TransferPhase.Idle)
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            onCancelTransfer = {
                                receiveState = receiveState.copy(phase = TransferPhase.Cancelled)
                            },
                            onOpenFile = {
                                receiveState.downloadedUri?.let { uri ->
                                    openDownloadedFile(context, uri)
                                }
                            },
                            onRetry = {
                                receiveState = TransferUiState(phase = TransferPhase.Idle, enteredAddress = receiveState.enteredAddress)
                            },
                            onDone = {
                                receiveState = TransferUiState()
                                destination = Destination.Home
                            },
                        )
                    }
                }
            }
        }
    }
}

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data object Connecting : DownloadStatus
    data class Downloading(
        val fileName: String,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadStatus
    data class Success(val fileName: String, val uri: Uri) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}

fun downloadFile(
    context: Context,
    urlString: String,
    onStatusChange: (DownloadStatus) -> Unit
) {
    Thread {
        try {
            onStatusChange(DownloadStatus.Connecting)

            var formattedUrl = urlString.trim()
            if (!formattedUrl.startsWith("http://", ignoreCase = true) &&
                !formattedUrl.startsWith("https://", ignoreCase = true)
            ) {
                formattedUrl = "http://$formattedUrl"
            }

            val url = URL(formattedUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                onStatusChange(
                    DownloadStatus.Error("Server returned code ${connection.responseCode}")
                )
                connection.disconnect()
                return@Thread
            }

            val contentDisposition = connection.getHeaderField("Content-Disposition")
            var fileName = "downloaded_file"
            if (!contentDisposition.isNullOrEmpty()) {
                val match = Regex("""filename="?([^";]+)"?""").find(contentDisposition)
                if (match != null) {
                    fileName = match.groupValues[1]
                }
            } else {
                val path = url.path
                if (path.isNotEmpty() && path != "/") {
                    fileName = path.substringAfterLast('/')
                }
            }

            val contentLength = connection.contentLengthLong
            val mimeType = connection.contentType ?: "application/octet-stream"

            val uri = connection.inputStream.use { inputStream ->
                saveStreamToDownloads(
                    context = context,
                    fileName = fileName,
                    mimeType = mimeType,
                    inputStream = inputStream,
                    onProgress = { totalRead ->
                        val progress = if (contentLength > 0) {
                            totalRead.toFloat() / contentLength
                        } else {
                            -1f
                        }

                        onStatusChange(
                            DownloadStatus.Downloading(
                                fileName = fileName,
                                progress = progress,
                                bytesDownloaded = totalRead,
                                totalBytes = contentLength
                            )
                        )
                    }
                )
            }

            connection.disconnect()
            onStatusChange(DownloadStatus.Success(fileName, uri))
        } catch (e: Exception) {
            e.printStackTrace()
            onStatusChange(
                DownloadStatus.Error(e.localizedMessage ?: "Download error")
            )
        }
    }.start()
}

fun saveStreamToDownloads(
    context: Context,
    fileName: String,
    mimeType: String,
    inputStream: InputStream,
    onProgress: (Long) -> Unit
): Uri {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { outputStream ->
            copyStream(inputStream, outputStream, onProgress)
        } ?: throw Exception("Failed to open output stream")

        return uri
    } else {
        @Suppress("DEPRECATION")
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            copyStream(inputStream, outputStream, onProgress)
        }
        return Uri.fromFile(file)
    }
}

private fun copyStream(
    inputStream: InputStream,
    outputStream: OutputStream,
    onProgress: (Long) -> Unit
) {
    val buffer = ByteArray(8192)
    var totalRead = 0L
    var bytesRead: Int
    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
        totalRead += bytesRead
        onProgress(totalRead)
    }
    outputStream.flush()
}

fun openDownloadedFile(context: Context, uri: Uri) {
    try {
        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                setDataAndType(contentUri, mimeType)
            } else {
                setDataAndType(uri, mimeType)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun generateQrCode(text: String): Bitmap? {
    return try {
        val size = 600

        val bitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.RGB_565
        )

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "Unknown IP"

        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()

            if (!networkInterface.isUp || networkInterface.isLoopback) {
                continue
            }

            val addresses = networkInterface.inetAddresses

            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()

                if (!address.isLoopbackAddress && address is Inet4Address) {
                    val hostAddress = address.hostAddress
                    if (!hostAddress.isNullOrEmpty() && hostAddress != "127.0.0.1") {
                        return hostAddress
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return "Unknown IP"
}