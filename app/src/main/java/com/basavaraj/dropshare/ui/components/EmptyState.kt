package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DropShareColors.MutedText,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = message,
            style = DropShareType.supporting,
            color = DropShareColors.MutedText,
        )
    }
}