package com.basavaraj.dropshare.model

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap

enum class TransferPhase {
    Idle,        // No file selected (Send) / no address entered (Receive)
    Selecting,   // System file picker or address entry is open
    Ready,       // File chosen, not yet sharing (Send only)
    Connecting,  // Receiver is attempting to reach the sender
    Waiting,     // Sender is up, no receiver connected yet
    Transferring,
    Completed,
    Failed,
    Cancelled,
}

enum class ErrorReason(val message: String, val recoverable: Boolean) {
    NoNetwork("No local network connection", recoverable = false),
    ConnectFailed("Could not connect to the sender", recoverable = true),
    SenderStopped("The sender stopped sharing this file", recoverable = false),
    Interrupted("Transfer interrupted", recoverable = true),
    FileGone("File is no longer available", recoverable = false),
}

data class TransferUiState(
    val phase: TransferPhase = TransferPhase.Idle,

    // File
    val fileName: String? = null,
    val fileType: String? = null,   // e.g. "PDF" — derived from MIME or extension
    val fileSizeBytes: Long? = null,
    val selectedUri: Uri? = null,
    val downloadedUri: Uri? = null,

    // Network (Send)
    val localIpAddress: String? = null,
    val port: Int? = null,
    val qrCodeBitmap: ImageBitmap? = null,

    // Network (Receive)
    val enteredAddress: String = "",

    // Progress — only meaningful while Transferring
    val bytesTransferred: Long = 0L,
    val transferSpeedBytesPerSec: Long = 0L,
    val etaSeconds: Long? = null,

    // Failure
    val error: ErrorReason? = null,
)
