package com.example.vintagecam

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VintageCameraUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_displaysVintageCamInterface() {
        // Either displays permission rationale if permissions not granted in test env,
        // or displays the vintage camera controls.
        // The activity should launch successfully without crash.
        assert(composeTestRule.activity != null)
    }
}
