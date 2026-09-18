package com.basavaraj.dropshare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.basavaraj.dropshare.ui.theme.DropShareColors
import com.basavaraj.dropshare.ui.theme.DropShareType
import com.basavaraj.dropshare.ui.theme.MinTouchTarget
import com.basavaraj.dropshare.ui.theme.Radius

@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(MinTouchTarget),
        shape = RoundedCornerShape(Radius.button),
        border = BorderStroke(1.dp, DropShareColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DropShareColors.PrimaryText,
            disabledContentColor = DropShareColors.DisabledText,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text = label, style = DropShareType.buttonLabel)
    }
}