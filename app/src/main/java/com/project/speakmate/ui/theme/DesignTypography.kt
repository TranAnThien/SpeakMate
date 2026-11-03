package com.project.speakmate.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DesignTypography {
    val Heading = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp)
    val Title = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp)
    val Button = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp)
    val AppBar = Button.copy(lineHeight = 20.sp)
    val Tagline = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp)
    val Body = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val Detail = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val Notice = Detail.copy(fontWeight = FontWeight.Medium)
    val Caption = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp)
    val Badge = Caption.copy(fontWeight = FontWeight.Bold, lineHeight = 14.sp)
    val Label = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp)
    val Level = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp)
    val Brand = Heading.copy(fontSize = 22.sp, lineHeight = 26.sp)
    val PageTitle = Heading.copy(fontSize = 24.sp, lineHeight = 28.sp)
    val SectionTitle = Heading.copy(fontSize = 20.sp, lineHeight = 24.sp)
    val SectionLabel = Detail.copy(fontWeight = FontWeight.Bold, lineHeight = 16.sp)
    val Link = Detail.copy(fontWeight = FontWeight.Bold, lineHeight = 16.sp)
    val Small = Caption.copy(lineHeight = 14.sp)
    val Navigation = Small.copy(fontWeight = FontWeight.Medium)
    val EmptyBody = Body.copy(fontSize = 16.sp, lineHeight = 22.sp)
    val LevelBadge = Title.copy(fontSize = 18.sp)
    val CallStatus = Small.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val CallTimer = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 38.sp)
}
