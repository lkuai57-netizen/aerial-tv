package app.orchard.tv.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import app.orchard.tv.apps.AppEntry
import app.orchard.tv.apps.CustomIcons
import app.orchard.tv.apps.TileArt
import app.orchard.tv.settings.AerialSettings
import app.orchard.tv.settings.HomeFocusPosition
import app.orchard.tv.settings.homeEntryFocusIndex
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
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
class PreferredHomeFocusTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var host: ActivityController<ComponentActivity>? = null
    private lateinit var view: View
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences("test_home_focus", Context.MODE_PRIVATE)
    private var position by mutableStateOf<HomeFocusPosition?>(null)
    private var apps by mutableStateOf(emptyList<AppEntry>())
    private var ready by mutableStateOf(true)
    private var entry by mutableStateOf(0L)
    private var reset by mutableStateOf(false)
    private var generation by mutableStateOf(0)
    private var focused: String? = null
    private var overlayReturn: String? = null
    private val launched = mutableListOf<String>()
    private val requests = HashMap<String, FocusRequester>()
    private val tvbox = "owner.actual.tvbox.package"

    private fun app(pkg: String, name: String): AppEntry {
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
        return AppEntry(pkg, name, ComponentName(pkg, "$pkg.Main"), true, true,
            TileArt(bmp, null, Rect.Zero, Color.Black, bmp))
    }

    @Before fun prepare() {
        prefs.edit().clear().commit()
        position = null
        AerialSettings.init(context)
        CustomIcons.init(context)
        apps = listOf(app("owner.film", "影视"), app("owner.bili", "云视听小电视"),
            app(tvbox, "TVBox"), app("owner.drive", "115生活"), app("owner.send", "LocalSend"))
    }

    @After fun close() { compose.runOnUiThread { host?.close() } }

    @Test fun coordinatesPersistAndDefaultsChooseTheMiddleWithoutAnAppIdentity() {
        assertEquals(2, homeEntryFocusIndex(5, 5, null))
        assertEquals(2, homeEntryFocusIndex(11, 5, null))
        assertEquals(5, homeEntryFocusIndex(11, 5, HomeFocusPosition(1, 0)))
        assertEquals(9, homeEntryFocusIndex(11, 5, HomeFocusPosition(1, 4)))
        HomeFocusPosition.write(prefs, HomeFocusPosition(1, 0))
        assertEquals(HomeFocusPosition(1, 0), HomeFocusPosition.read(prefs))
        assertEquals(setOf("home_focus_row", "home_focus_column"), prefs.all.keys)
        HomeFocusPosition.write(prefs, null)
        assertNull(HomeFocusPosition.read(prefs))
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun missingSlotsAndEmptyListsUseOriginalFallback() {
        assertNull(homeEntryFocusIndex(5, 5, null, enabled = false))
        assertNull(homeEntryFocusIndex(0, 5, null))
        assertNull(homeEntryFocusIndex(5, 5, HomeFocusPosition(1, 0)))
        assertNull(homeEntryFocusIndex(8, 5, HomeFocusPosition(1, 4)))
        assertNull(homeEntryFocusIndex(20, 4, HomeFocusPosition(0, 4)))
        assertNull(homeEntryFocusIndex(5, 5, HomeFocusPosition(Int.MAX_VALUE, 1)))
        assertEquals(0, homeEntryFocusIndex(1, 5, null))
    }

    private fun mount() {
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            host = controller
            controller.get().setContent {
                view = LocalView.current
                key(generation) {
                    HomeFocusEffect(ready && apps.isNotEmpty(), entry, reset, apps.map { it.key },
                        preferredKey = { homeEntryFocusIndex(apps.size, 5, position)?.let { apps[it].key } },
                        focus = { requests.getValue(it).requestFocus() },
                        restore = { first ->
                            val wanted = if (first) apps.first().key else overlayReturn ?: focused ?: apps.first().key
                            requests[if (apps.any { it.key == wanted }) wanted else apps.first().key]?.requestFocus()
                            overlayReturn = null
                        },
                        onEntryFocused = { overlayReturn = null },
                        onResetHandled = { reset = false })
                    Column(Modifier.size(960.dp, 500.dp)) {
                      apps.chunked(5).forEach { row -> Row {
                        row.forEachIndexed { index, app ->
                            key(app.key) {
                                AppTile(app, 150.dp, 100.dp, requests.getOrPut(app.key) { FocusRequester() },
                                    enabled = ready, moving = false,
                                    onFocused = { focused = app.key }, onClick = { launched += app.key },
                                    onLongPress = {}, rowEnd = index == row.lastIndex)
                            }
                        }
                      } }
                    }
                }
            }
            controller.windowFocusChanged(true)
        }
        settle()
    }

    private fun settle() { repeat(35) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() } }
    private fun change(block: () -> Unit) { compose.runOnUiThread(block); settle() }
    private fun tap(code: Int) {
        change {
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
    }

    @Test fun coldStartAndRecreationFocusTheMiddleWithoutLaunchingOrReordering() {
        val original = apps.map { it.key }
        mount()
        assertEquals(tvbox, focused)
        assertTrue(launched.isEmpty())
        tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals("owner.drive", focused)
        change { generation++; focused = null }
        assertEquals(tvbox, focused)
        assertEquals(original, apps.map { it.key })
        assertTrue(launched.isEmpty())
    }

    @Test fun remoteMovesFreelyAndOnlyConfirmLaunchesThenReturnUsesSelectedPosition() {
        mount()
        tap(KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals("owner.bili", focused)
        tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(tvbox, focused)
        tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        tap(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(listOf("owner.drive"), launched)
        change { ready = false }
        change { entry++; ready = true }
        assertEquals(tvbox, focused)
        assertEquals(listOf("owner.drive"), launched)
        tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals("owner.drive", focused)
    }

    @Test fun homeResetWinsOverStaleOverlayRestorationButNormalOverlayKeepsItsTile() {
        mount()
        tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        change { overlayReturn = "owner.drive"; ready = false }
        change { ready = true }
        assertEquals("owner.drive", focused)
        change { overlayReturn = "owner.drive"; ready = false; entry++; reset = true }
        change { ready = true }
        assertEquals(tvbox, focused)
        assertFalse(reset)
        settle()
        assertEquals(tvbox, focused)
        assertTrue(launched.isEmpty())
    }

    @Test fun waitsForLoadWindowAndForegroundThenUsesPositionNotPackage() {
        val loaded = apps
        apps = emptyList()
        ready = false
        mount()
        change { apps = loaded.reversed() }
        assertNull(focused)
        change { ready = true }
        assertEquals(tvbox, focused)
        change { apps = listOf(loaded[2]) + loaded.filter { it.key != tvbox } }
        tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals("owner.film", focused)
        change { entry++ }
        assertEquals("owner.bili", focused)
    }

    @Test fun customSecondRowFirstAndRightmostPositionsWorkAcrossReturns() {
        apps = apps + List(5) { app("owner.extra$it", "Extra $it") }
        position = HomeFocusPosition(1, 0)
        mount()
        assertEquals("owner.extra0", focused)
        change { ready = false; position = HomeFocusPosition(1, 4); entry++ }
        change { ready = true }
        assertEquals("owner.extra4", focused)
        change { ready = false; position = HomeFocusPosition(0, 0); entry++ }
        change { ready = true }
        assertEquals("owner.film", focused)
        assertTrue(launched.isEmpty())
    }

    @Test fun missingPositionRetainsOriginalFirstAndReturnFocusPolicy() {
        position = HomeFocusPosition(1, 0)
        apps = apps.filter { it.key != tvbox }
        mount()
        assertEquals("owner.film", focused)
        tap(KeyEvent.KEYCODE_DPAD_RIGHT)
        change { ready = false }
        change { ready = true; entry++ }
        assertEquals("owner.bili", focused)
        change { entry++; reset = true }
        assertEquals("owner.film", focused)
        assertTrue(launched.isEmpty())
    }
}
