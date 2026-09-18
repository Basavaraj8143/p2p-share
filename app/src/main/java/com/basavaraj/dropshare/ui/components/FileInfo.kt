package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun FileInfo(
    fileName: String,
    fileType: String,
    fileSizeLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = DropShareColors.SecondaryText,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column {
            Text(text = fileName, style = DropShareType.bodyEmphasis, color = DropShareColors.PrimaryText)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "$fileType • $fileSizeLabel",
                style = DropShareType.supporting,
                color = DropShareColors.MutedText,
            )
        }
    }
}