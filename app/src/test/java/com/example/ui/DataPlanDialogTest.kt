package com.example.ui

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.room.DataPlanEntity
import com.example.ui.components.DataPlanDialog
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class DataPlanDialogTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun `saving applies limit, cycle day and re-arms alerts`() {
        var saved: DataPlanEntity? = null
        val initial = DataPlanEntity(cycleStartDay = 31, warningAlertedCycle = 42L, limitAlertedCycle = 42L)
        compose.setContent {
            MyApplicationTheme { DataPlanDialog(currentPlan = initial, onSave = { saved = it }, onDismiss = {}) }
        }
        compose.onNodeWithText("5GB").performClick()
        // Wraps from 31 to 1.
        compose.onNodeWithTag("cycle_day_plus").performScrollTo().performClick()
        compose.onNodeWithTag("cycle_day_value").assertTextEquals("1")
        compose.onNodeWithTag("save_plan_button").performClick()

        val plan = requireNotNull(saved)
        assertEquals(5L * 1024 * 1024 * 1024, plan.monthlyLimitBytes)
        assertEquals(1, plan.cycleStartDay)
        assertEquals(0L, plan.warningAlertedCycle)
        assertEquals(0L, plan.limitAlertedCycle)
    }

    @Test
    fun `invalid limit disables save`() {
        compose.setContent {
            MyApplicationTheme { DataPlanDialog(currentPlan = DataPlanEntity(), onSave = {}, onDismiss = {}) }
        }
        compose.onNodeWithText("10.0").performTextReplacement("0")
        compose.onNodeWithTag("save_plan_button").assertIsNotEnabled()
    }
}
