package com.basavaraj.dropshare.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basavaraj.dropshare.ui.components.ConnectionState
import com.basavaraj.dropshare.ui.components.ConnectionStatus
import com.basavaraj.dropshare.ui.components.PrimaryButton
import com.basavaraj.dropshare.ui.components.SecondaryButton
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun HomeScreen(
    isOnLocalNetwork: Boolean,
    onRefreshNetwork: () -> Unit,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
) {
    Scaffold(containerColor = DropShareColors.Background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))

            Text(
                text = "DropShare",
                style = DropShareType.screenTitle,
                color = DropShareColors.PrimaryText,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Direct file sharing over local network",
                style = DropShareType.supporting,
                color = DropShareColors.SecondaryText,
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(label = "Receive File", onClick = onReceiveClick, enabled = isOnLocalNetwork)
            Spacer(modifier = Modifier.height(Spacing.md))
            SecondaryButton(label = "Send File", onClick = onSendClick, enabled = isOnLocalNetwork)

            Spacer(modifier = Modifier.height(Spacing.xl))

            ConnectionStatus(
                state = if (isOnLocalNetwork) ConnectionState.Ready else ConnectionState.Offline,
                label = if (isOnLocalNetwork) "Ready to share" else "Not connected to a local network",
                onRefreshClick = onRefreshNetwork,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}