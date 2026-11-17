package app.orchard.tv.ui.wallpaper

import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import app.orchard.tv.R
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.settings.HomeFocusPosition
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "w960dp-h540dp-land-xhdpi")
class HomeFocusPositionSettingsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var host: ActivityController<ComponentActivity>? = null
    private lateinit var view: View
    private val context get() = RuntimeEnvironment.getApplication()
    @After fun close() { compose.runOnUiThread { host?.close() } }
    private fun settle() { repeat(25) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() } }
    private fun click(id: Int, vararg args: Any) {
        compose.onNodeWithText(context.getString(id, *args)).performTouchInput { click() }
        settle()
    }

    @Test fun existingSettingsControlsSelectAnyRowColumnAndRestoreMiddle() {
        AerialSettings.init(context)
        AerialSettings.chooseHomeColumns(5)
        AerialSettings.chooseHomeFocusPosition(null)
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            host = controller
            controller.get().setContent {
                view = LocalView.current
                Column(Modifier.size(620.dp, 400.dp)) { HomeFocusPositionSettings(10) }
            }
            controller.windowFocusChanged(true)
        }
        settle()
        click(R.string.st_focus_position_custom)
        assertEquals(HomeFocusPosition(0, 2), AerialSettings.homeFocusPosition)
        click(R.string.st_focus_position_row_value, 1)
        assertEquals(HomeFocusPosition(1, 2), AerialSettings.homeFocusPosition)
        click(R.string.st_focus_position_column_value, 3)
        assertEquals(HomeFocusPosition(1, 3), AerialSettings.homeFocusPosition)
        // The same existing Pressable control accepts the remote OK key.
        compose.runOnUiThread {
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        }
        settle()
        assertEquals(HomeFocusPosition(1, 4), AerialSettings.homeFocusPosition)
        click(R.string.st_focus_position_column_value, 5)
        assertEquals(HomeFocusPosition(1, 0), AerialSettings.homeFocusPosition)
        click(R.string.st_focus_position_middle)
        assertNull(AerialSettings.homeFocusPosition)
    }
}
