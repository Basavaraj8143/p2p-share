package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.model.ErrorReason
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun ErrorMessage(
    reason: ErrorReason,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (reason == ErrorReason.NoNetwork) Icons.Outlined.WifiOff else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = DropShareColors.Error,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = reason.message,
            style = DropShareType.bodyEmphasis,
            color = DropShareColors.PrimaryText,
        )
        Spacer(modifier = Modifier.height(Spacing.xl))

        if (reason.recoverable) {
            PrimaryButton(label = "Retry", onClick = onRetry)
            Spacer(modifier = Modifier.height(Spacing.md))
            SecondaryButton(label = "Done", onClick = onDone)
        } else {
            PrimaryButton(label = "Done", onClick = onDone)
        }
    }
}