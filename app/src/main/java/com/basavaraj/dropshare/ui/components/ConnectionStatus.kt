package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

enum class ConnectionState { Offline, Ready, Connecting, Waiting, Connected }

@Composable
fun ConnectionStatus(
    state: ConnectionState,
    label: String,
    modifier: Modifier = Modifier,
    onRefreshClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            ConnectionState.Connecting, ConnectionState.Waiting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = DropShareColors.Primary,
                    strokeWidth = 2.dp,
                )
            }
            ConnectionState.Offline -> Dot(color = DropShareColors.MutedText)
            ConnectionState.Ready -> Dot(color = DropShareColors.Primary)
            ConnectionState.Connected -> Dot(color = DropShareColors.Success)
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = DropShareType.supporting,
            color = when (state) {
                ConnectionState.Offline -> DropShareColors.MutedText
                else -> DropShareColors.SecondaryText
            },
            modifier = Modifier.weight(1f, fill = false)
        )
        if (onRefreshClick != null) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh network status",
                    tint = DropShareColors.SecondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color = color, shape = CircleShape),
    )
}