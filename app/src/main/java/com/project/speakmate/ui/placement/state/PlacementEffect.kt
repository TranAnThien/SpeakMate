package com.project.speakmate.ui.placement.state

sealed interface PlacementEffect {
    data object NavigateHome : PlacementEffect
}
