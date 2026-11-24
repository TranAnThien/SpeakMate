package com.project.speakmate.ui.components.lottie

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.project.speakmate.R

enum class MascotAnimation(@RawRes val animationRes: Int, @DrawableRes val fallbackRes: Int) {
    NETWORK_ERROR(R.raw.lottie_network_error, R.drawable.img_call_error),
    EMPTY_STATE(R.raw.lottie_empty_state, R.drawable.img_placement_mascot),
    ASSESSING(R.raw.lottie_assessing, R.drawable.img_call_processing),
    CALL_MASCOT(R.raw.lottie_call_mascot, R.drawable.img_placement_mascot),
    CALL_CONNECTING(R.raw.lottie_call_connecting, R.drawable.img_placement_mascot),
    RESULT_PASS(R.raw.lottie_result_pass, R.drawable.img_placement_mascot),
    RESULT_ENCOURAGE(R.raw.lottie_result_encourage, R.drawable.img_placement_mascot),
    PATH_COMPLETE(R.raw.lottie_path_complete, R.drawable.img_training_path_complete)
}
