package com.basavaraj.dropshare.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.basavaraj.dropshare.model.TransferPhase
import com.basavaraj.dropshare.model.TransferUiState
import com.basavaraj.dropshare.ui.components.ConnectionState
import com.basavaraj.dropshare.ui.components.ConnectionStatus
import com.basavaraj.dropshare.ui.components.EmptyState
import com.basavaraj.dropshare.ui.components.ErrorMessage
import com.basavaraj.dropshare.ui.components.FileInfo
import com.basavaraj.dropshare.ui.components.PrimaryButton
import com.basavaraj.dropshare.ui.components.QrCodeCard
import com.basavaraj.dropshare.ui.components.SecondaryButton
import com.basavaraj.dropshare.ui.components.TransferProgress
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun SendScreen(
    state: TransferUiState,
    onBackClick: () -> Unit,
    onPickFile: () -> Unit,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onCancelTransfer: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    var showStopConfirm by remember { mutableStateOf(false) }

    Scaffold(containerColor = DropShareColors.Background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DropShareColors.PrimaryText
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(text = "Send File", style = DropShareType.screenTitle, color = DropShareColors.PrimaryText)
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            when (state.phase) {
                TransferPhase.Idle, TransferPhase.Selecting -> {
                    EmptyState(
                        icon = Icons.Outlined.UploadFile,
                        message = "No file selected",
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    PrimaryButton(
                        label = "Choose File",
                        onClick = onPickFile,
                        isLoading = state.phase == TransferPhase.Selecting,
                    )
                }

                TransferPhase.Ready -> {
                    FileInfo(
                        fileName = state.fileName.orEmpty(),
                        fileType = state.fileType.orEmpty(),
                        fileSizeLabel = formatBytes(state.fileSizeBytes ?: 0L),
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    PrimaryButton(label = "Start Sharing", onClick = onStartSharing)
                }

                TransferPhase.Waiting -> {
                    Text(
                        text = "Sharing ${state.fileName.orEmpty()}",
                        style = DropShareType.sectionHeading,
                        color = DropShareColors.PrimaryText,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    ConnectionStatus(state = ConnectionState.Waiting, label = "Waiting for a device…")
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    QrCodeCard(
                        qrCodeBitmap = state.qrCodeBitmap,
                        address = "${state.localIpAddress ?: "—"}:${state.port ?: "—"}",
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "Both devices must be connected to the same network.",
                        style = DropShareType.supporting,
                        color = DropShareColors.MutedText,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    SecondaryButton(label = "Stop Sharing", onClick = { showStopConfirm = true })
                }

                TransferPhase.Transferring -> {
                    Text(
                        text = "Sharing ${state.fileName.orEmpty()}",
                        style = DropShareType.sectionHeading,
                        color = DropShareColors.PrimaryText,
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    val pct = if ((state.fileSizeBytes ?: 0L) > 0)
                        state.bytesTransferred.toFloat() / state.fileSizeBytes!!
                    else 0f
                    TransferProgress(
                        progress = pct,
                        percentLabel = "${(pct * 100).toInt()}%",
                        etaLabel = state.etaSeconds?.let { "${it}s left" },
                        detailLabel = "${formatBytes(state.bytesTransferred)} of ${formatBytes(state.fileSizeBytes ?: 0L)} • ${formatBytes(state.transferSpeedBytesPerSec)}/s",
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    SecondaryButton(label = "Cancel Transfer", onClick = onCancelTransfer)
                }

                TransferPhase.Completed -> {
                    Text(text = "Transfer complete", style = DropShareType.sectionHeading, color = DropShareColors.Success)
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    FileInfo(
                        fileName = state.fileName.orEmpty(),
                        fileType = state.fileType.orEmpty(),
                        fileSizeLabel = formatBytes(state.fileSizeBytes ?: 0L),
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    PrimaryButton(label = "Done", onClick = onDone)
                }

                TransferPhase.Failed -> {
                    state.error?.let { reason ->
                        ErrorMessage(reason = reason, onRetry = onRetry, onDone = onDone)
                    }
                }

                TransferPhase.Cancelled -> {
                    Text(
                        text = "Transfer cancelled",
                        style = DropShareType.sectionHeading,
                        color = DropShareColors.SecondaryText,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    PrimaryButton(label = "Done", onClick = onDone)
                }

                TransferPhase.Connecting -> {
                    // Send phase connecting state
                }
            }
        }
    }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text("Stop sharing?") },
            text = {
                Text(
                    if (state.phase == TransferPhase.Transferring)
                        "The current transfer will be cancelled."
                    else
                        "Your device will stop being visible to other devices."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopConfirm = false
                    onStopSharing()
                }) { Text("Stop Sharing") }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}