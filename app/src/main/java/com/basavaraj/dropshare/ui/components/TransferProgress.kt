package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun TransferProgress(
    progress: Float,
    percentLabel: String,
    etaLabel: String?,
    detailLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = percentLabel,
                style = DropShareType.screenTitle,
                color = DropShareColors.PrimaryText,
            )
            if (etaLabel != null) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = etaLabel,
                    style = DropShareType.body,
                    color = DropShareColors.SecondaryText,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = DropShareColors.Primary,
            trackColor = DropShareColors.Border,
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = detailLabel,
            style = DropShareType.supporting,
            color = DropShareColors.MutedText,
        )
    }
}