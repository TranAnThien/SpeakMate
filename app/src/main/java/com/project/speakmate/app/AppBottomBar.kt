package com.project.speakmate.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun AppBottomBar(
    onSelectDestination: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    selectedDestination: AppDestination = AppDestination.TRAINING,
    bottomPadding: Dp = 12.dp
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup()
            .background(DesignColor.Surface)
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = bottomPadding)
    ) {
        AppDestination.entries.forEach { destination ->
            val iconRes =
                when (destination) {
                    AppDestination.TRAINING -> R.drawable.ic_navigation_path
                    AppDestination.PRACTICE -> R.drawable.ic_navigation_practice
                    AppDestination.HISTORY -> R.drawable.ic_navigation_history
                    AppDestination.PROFILE -> R.drawable.ic_navigation_profile
                }
            val labelRes =
                when (destination) {
                    AppDestination.TRAINING -> R.string.navigation_training
                    AppDestination.PRACTICE -> R.string.navigation_practice
                    AppDestination.HISTORY -> R.string.navigation_history
                    AppDestination.PROFILE -> R.string.navigation_profile
                }
            AppNavigationItem(
                iconRes = iconRes,
                label = stringResource(labelRes),
                isSelected = selectedDestination == destination,
                onClick = { onSelectDestination(destination) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppNavigationItem(
    @DrawableRes iconRes: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) DesignColor.Primary else DesignColor.Muted
    Column(
        modifier =
            modifier
                .heightIn(min = 50.dp)
                .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
                .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(color)
        )
        Text(text = label, style = DesignTypography.Navigation, color = color)
    }
}
