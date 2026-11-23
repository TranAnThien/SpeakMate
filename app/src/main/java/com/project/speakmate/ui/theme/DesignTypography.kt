package com.project.speakmate.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DesignTypography {
    val Heading = Typography.headlineMedium
    val Title = Typography.titleMedium
    val Button = Typography.labelLarge
    val AppBar = Typography.titleLarge
    val Tagline = Typography.bodyLarge
    val Body = Typography.bodyLarge
    val Detail = Typography.bodyMedium
    val Notice = Detail.copy(fontWeight = FontWeight.Medium)
    val Caption = Typography.bodySmall
    val Badge = Typography.labelMedium
    val Label = Typography.labelSmall
    val Level = Typography.displayMedium
    val Brand = Typography.titleLarge
    val PageTitle = Typography.titleLarge
    val SectionTitle = Typography.headlineSmall
    val SectionLabel = Typography.labelLarge
    val Link = Typography.labelLarge
    val Small = Typography.bodySmall
    val Navigation = Typography.labelMedium
    val EmptyBody = Typography.bodyLarge
    val LevelBadge = Typography.titleMedium
    val CallStatus = Typography.labelMedium
    val CallTimer = Typography.bodyLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp)
}
