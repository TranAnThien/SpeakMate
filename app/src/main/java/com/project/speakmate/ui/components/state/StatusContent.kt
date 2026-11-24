package com.project.speakmate.ui.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun StatusContent(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    illustration: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    titleStyle: TextStyle = DesignTypography.Heading,
    descriptionStyle: TextStyle = DesignTypography.Body,
    titleColor: Color = DesignColor.Text,
    descriptionColor: Color = DesignColor.Muted,
    titleTextAlign: TextAlign = TextAlign.Unspecified,
    descriptionTextAlign: TextAlign = TextAlign.Unspecified,
    titleModifier: Modifier = Modifier,
    descriptionModifier: Modifier = Modifier,
    titleContent: @Composable ColumnScope.() -> Unit = {}
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment, verticalArrangement = verticalArrangement) {
        illustration()
        if (title.isNotEmpty()) {
            HeadingText(
                text = title,
                modifier = titleModifier,
                style = titleStyle,
                color = titleColor,
                textAlign = titleTextAlign
            )
        }
        titleContent()
        if (description.isNotEmpty()) {
            BodyText(
                text = description,
                modifier = descriptionModifier,
                style = descriptionStyle,
                color = descriptionColor,
                textAlign = descriptionTextAlign
            )
        }
        actions()
    }
}
