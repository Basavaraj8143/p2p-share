package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.Radius
import com.basavaraj.dropshare.ui.theme.Spacing

@Composable
fun QrCodeCard(
    qrCodeBitmap: ImageBitmap?,
    address: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DropShareColors.ElevatedSurface, RoundedCornerShape(Radius.card))
            .border(BorderStroke(1.dp, DropShareColors.Border), RoundedCornerShape(Radius.card))
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Scan to connect",
            style = DropShareType.supporting,
            color = DropShareColors.SecondaryText,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        if (qrCodeBitmap != null) {
            Image(
                bitmap = qrCodeBitmap,
                contentDescription = "QR code for $address",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f),
            )
        } else {
            CircularProgressIndicator(
                color = DropShareColors.Primary,
                modifier = Modifier.padding(Spacing.xxl),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = address,
            style = DropShareType.monospaceAddress,
            color = DropShareColors.PrimaryText,
        )
    }
}