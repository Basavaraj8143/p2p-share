package com.basavaraj.dropshare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DropShareType {
    val screenTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val sectionHeading = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val bodyEmphasis = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    val supporting = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val buttonLabel = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
    val monospaceAddress = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
}

val AppTypography = Typography(
    headlineSmall = DropShareType.screenTitle,
    titleMedium = DropShareType.sectionHeading,
    bodyLarge = DropShareType.body,
    bodyMedium = DropShareType.body,
    labelLarge = DropShareType.buttonLabel,
    bodySmall = DropShareType.supporting,
)