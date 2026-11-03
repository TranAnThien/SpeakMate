package com.project.speakmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun ContentSkeletonCards(count: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count.coerceAtLeast(0)) {
            Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.widthIn(max = 180.dp).fillMaxWidth().height(14.dp).background(DesignColor.Border, RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth().height(14.dp).background(DesignColor.Border, RoundedCornerShape(4.dp)))
                Box(Modifier.widthIn(max = 280.dp).fillMaxWidth().height(14.dp).background(DesignColor.Border, RoundedCornerShape(4.dp)))
                Box(Modifier.widthIn(max = 140.dp).fillMaxWidth().height(14.dp).background(DesignColor.Border, RoundedCornerShape(4.dp)))
            }
        }
    }
}
