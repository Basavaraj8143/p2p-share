package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.MinTouchTarget
import com.basavaraj.dropshare.ui.theme.Radius

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(MinTouchTarget),
        shape = RoundedCornerShape(Radius.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = DropShareColors.Primary,
            contentColor = DropShareColors.OnPrimary,
            disabledContainerColor = DropShareColors.DisabledSurface,
            disabledContentColor = DropShareColors.DisabledText,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = DropShareColors.OnPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = label, style = DropShareType.buttonLabel)
            }
        }
    }
}