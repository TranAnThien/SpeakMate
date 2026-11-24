package com.project.speakmate.ui.call.state

sealed interface CallDialog {
    data object Title : CallDialog

    data object EndConfirmation : CallDialog
}
