package com.basavaraj.dropshare.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.basavaraj.dropshare.model.TransferPhase
import com.basavaraj.dropshare.model.TransferUiState
import com.basavaraj.dropshare.ui.components.ConnectionState
import com.basavaraj.dropshare.ui.components.ConnectionStatus
import com.basavaraj.dropshare.ui.components.ErrorMessage
import com.basavaraj.dropshare.ui.components.FileInfo
import com.basavaraj.dropshare.ui.components.PrimaryButton
import com.basavaraj.dropshare.ui.components.TransferProgress
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun ReceiveScreen(
    state: TransferUiState,
    onBackClick: () -> Unit,
    onAddressChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onConnectClick: () -> Unit,
    onCancelTransfer: () -> Unit,
    onOpenFile: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
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
                Text(text = "Receive File", style = DropShareType.screenTitle, color = DropShareColors.PrimaryText)
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Connect to a device sharing on your local network.",
                style = DropShareType.supporting,
                color = DropShareColors.SecondaryText,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))

            when (state.phase) {
                TransferPhase.Idle -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.enteredAddress,
                            onValueChange = onAddressChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("192.168.1.8:8080") },
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        IconButton(onClick = onPasteClick) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = "Paste address",
                                tint = DropShareColors.SecondaryText,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    PrimaryButton(
                        label = "Connect",
                        onClick = onConnectClick,
                        enabled = state.enteredAddress.isNotBlank(),
                    )
                }

                TransferPhase.Connecting -> {
                    ConnectionStatus(state = ConnectionState.Connecting, label = "Connecting to device…")
                }

                TransferPhase.Transferring -> {
                    val pct = if ((state.fileSizeBytes ?: 0L) > 0)
                        state.bytesTransferred.toFloat() / state.fileSizeBytes!!
                    else 0f
                    FileInfo(
                        fileName = state.fileName.orEmpty(),
                        fileType = state.fileType.orEmpty(),
                        fileSizeLabel = formatBytes(state.fileSizeBytes ?: 0L),
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    TransferProgress(
                        progress = pct,
                        percentLabel = "${(pct * 100).toInt()}%",
                        etaLabel = state.etaSeconds?.let { "${it}s left" },
                        detailLabel = "${formatBytes(state.bytesTransferred)} of ${formatBytes(state.fileSizeBytes ?: 0L)} • ${formatBytes(state.transferSpeedBytesPerSec)}/s",
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    OutlinedButton(
                        onClick = onCancelTransfer,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancel") }
                }

                TransferPhase.Completed -> {
                    Text(text = "Transfer complete", style = DropShareType.sectionHeading, color = DropShareColors.Success)
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    FileInfo(
                        fileName = state.fileName.orEmpty(),
                        fileType = state.fileType.orEmpty(),
                        fileSizeLabel = formatBytes(state.fileSizeBytes ?: 0L),
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    OutlinedButton(
                        onClick = onOpenFile,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open File") }
                    Spacer(modifier = Modifier.height(Spacing.md))
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

                TransferPhase.Selecting, TransferPhase.Ready, TransferPhase.Waiting -> {
                    // Send-only phases
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}