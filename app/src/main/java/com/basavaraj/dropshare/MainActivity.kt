package com.basavaraj.dropshare

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DropShareTheme {
                DropShareApp()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropShareApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "DropShare",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Send") },
                        icon = { Icon(Icons.Default.Send, contentDescription = "Send") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Receive") },
                        icon = { Icon(Icons.Default.Download, contentDescription = "Receive") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> SendScreen()
                1 -> ReceiveScreen()
            }
        }
    }
}

@Composable
fun SendScreen() {
    val context = LocalContext.current

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var serverRunning by remember { mutableStateOf(false) }
    var serverAddress by remember { mutableStateOf("") }
    var server by remember { mutableStateOf<FileServer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            val currentServer = server
            Thread {
                currentServer?.stop()
            }.start()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (serverRunning) {
                val currentServer = server
                Thread {
                    currentServer?.stop()
                }.start()
                server = null
                qrBitmap = null
                serverRunning = false
            }

            selectedFileUri = uri

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

                    fileName = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        cursor.getString(nameIndex)
                    } else {
                        "Unknown file"
                    }

                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        0L
                    }

                    fileSize = formatFileSize(size)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Share File Over Local Wi-Fi",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                filePicker.launch("*/*")
            }
        ) {
            Text("Choose File")
        }

        selectedFileUri?.let { uri ->
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "📄 $fileName",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = fileSize)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!serverRunning) {
                        val serverInstance = FileServer(
                            fileName = fileName,
                            fileSize = getFileSize(context, uri)
                        ) {
                            context.contentResolver.openInputStream(uri)
                        }

                        server = serverInstance

                        Thread {
                            serverInstance.start()
                        }.start()

                        serverRunning = true

                        val ipAddress = getLocalIpAddress()
                        serverAddress = "http://$ipAddress:8080"
                        qrBitmap = generateQrCode(serverAddress)
                    } else {
                        val currentServer = server
                        Thread {
                            currentServer?.stop()
                        }.start()
                        server = null
                        qrBitmap = null
                        serverRunning = false
                    }
                }
            ) {
                Text(
                    if (serverRunning) "Stop Sharing" else "Share File"
                )
            }

            if (serverRunning) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Server Running",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = serverAddress,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Open this address on the receiving device",
                            style = MaterialTheme.typography.bodySmall
                        )

                        qrBitmap?.let { bitmap ->
                            Spacer(modifier = Modifier.height(16.dp))

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(200.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Scan QR code on receiving device",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiveScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var serverUrlInput by remember { mutableStateOf("") }
    var downloadStatus by remember { mutableStateOf<DownloadStatus>(DownloadStatus.Idle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Receive File",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the sender's address to download the shared file",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = serverUrlInput,
            onValueChange = { serverUrlInput = it },
            label = { Text("Sender Address") },
            placeholder = { Text("http://192.168.1.x:8080") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        val clip = clipboardManager.getText()
                        if (clip != null) {
                            serverUrlInput = clip.text
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste Address"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (serverUrlInput.isNotBlank()) {
                    downloadFile(
                        context = context,
                        urlString = serverUrlInput,
                        onStatusChange = { status ->
                            downloadStatus = status
                        }
                    )
                }
            },
            enabled = serverUrlInput.isNotBlank() && downloadStatus !is DownloadStatus.Connecting && downloadStatus !is DownloadStatus.Downloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download File")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val status = downloadStatus) {
            DownloadStatus.Idle -> {
                // Idle state
            }

            DownloadStatus.Connecting -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Connecting to server...")
                    }
                }
            }

            is DownloadStatus.Downloading -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Downloading: ${status.fileName}",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (status.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { status.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val formattedDownloaded = formatFileSize(status.bytesDownloaded)
                        val formattedTotal = if (status.totalBytes > 0) formatFileSize(status.totalBytes) else "Unknown"

                        Text(
                            text = "$formattedDownloaded / $formattedTotal",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            is DownloadStatus.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅ File Received Successfully!",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = status.fileName,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Saved to Downloads",
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                openDownloadedFile(context, status.uri)
                            }
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open File")
                        }
                    }
                }
            }

            is DownloadStatus.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "❌ Download Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
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

fun getFileSize(
    context: Context,
    uri: Uri
): Long {
    try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) {
                    return cursor.getLong(index)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return 0L
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

fun formatFileSize(bytes: Long): String {
    if (bytes < 0) return "Unknown"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "%.2f KB".format(bytes / 1024.0)
    if (bytes < 1024 * 1024 * 1024) return "%.2f MB".format(bytes / (1024.0 * 1024.0))
    return "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}