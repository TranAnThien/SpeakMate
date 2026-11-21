package com.project.speakmate.test

import android.os.Bundle
import androidx.core.os.bundleOf
import com.project.speakmate.utils.extension.default
import com.project.speakmate.utils.extension.defaultFalse
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class SavedStateHandleRule : TestWatcher() {
    companion object {
        private const val BUNDLE_FUNCTIONS_CLASS = "androidx.core.os.BundleKt"
    }

    override fun starting(description: Description) {
        mockkStatic(BUNDLE_FUNCTIONS_CLASS)
        every { bundleOf(*anyVararg()) } answers {
            val values = firstArg<Array<Pair<String, Any?>>>().toMap()
            val bundle = mockk<Bundle>()
            every { bundle.getString(any()) } answers { values[firstArg<String>()] as? String }
            every { bundle.getBoolean(any()) } answers {
                (values[firstArg<String>()] as? Boolean).defaultFalse()
            }
            every { bundle.getBoolean(any(), any()) } answers {
                (values[firstArg<String>()] as? Boolean).default(secondArg<Boolean>())
            }
            every { bundle.containsKey(any()) } answers { values.containsKey(firstArg<String>()) }
            every { bundle.get(any()) } answers { values[firstArg<String>()] }
            bundle
        }
    }

    override fun finished(description: Description) {
        unmockkStatic(BUNDLE_FUNCTIONS_CLASS)
    }
}
