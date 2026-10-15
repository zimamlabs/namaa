package dev.namaa.app

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import dev.namaa.core.DataLocation
import dev.namaa.core.MoneyBook
import dev.namaa.core.SettingsRepository
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Drives the real shell with pointer events: the pager swipes between sections in both reading directions, and the drawer opens from the start edge. */
@OptIn(ExperimentalComposeUiApi::class)
class ShellTest {
    private val w = 412; private val h = 860

    private class Rig(val scene: ImageComposeScene, val pages: MutableList<Tab>, val drawer: DrawerState) {
        var now = 0L
        fun frames(n: Int = 30) { repeat(n) { now += 16_000_000; scene.render(now) } }
        fun drag(from: Offset, to: Offset, steps: Int = 12) {
            scene.sendPointerEvent(PointerEventType.Press, from, type = PointerType.Touch); frames(1)
            for (i in 1..steps) { scene.sendPointerEvent(PointerEventType.Move, Offset(from.x + (to.x - from.x) * i / steps, from.y + (to.y - from.y) * i / steps), type = PointerType.Touch); frames(1) }
            scene.sendPointerEvent(PointerEventType.Release, to, type = PointerType.Touch); frames(40)
        }
        fun png(name: String) = File("build/screenshots").apply { mkdirs() }.resolve("$name.png").writeBytes(scene.render(now + 1).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }

    private fun store(lang: String, settingsTweak: (dev.namaa.core.Settings) -> dev.namaa.core.Settings = { it }): NamaaStore {
        val fs = FakeFileSystem()
        val repo = SettingsRepository(fs, "/s/prefs.json".toPath()).also { r -> r.update { settingsTweak(it.copy(lang = lang, tourDone = true, lastSeenVersion = APP_VERSION)) } }
        val location = DataLocation(fs, "/data".toPath(), "/shared".toPath(), emptyList()) { false }
        return NamaaStore(MoneyBook(fs, "/data".toPath()), repo, location)
    }

    private fun rig(lang: String, tweak: (dev.namaa.core.Settings) -> dev.namaa.core.Settings = { it }, test: (Rig) -> Unit) {
        val pages = mutableListOf<Tab>()
        val drawer = DrawerState(DrawerValue.Closed)
        val st = store(lang, tweak)
        val scene = ImageComposeScene(w * 2, h * 2, Density(2f)) { NamaaApp(st, drawer, { pages += it }, showSplash = false) }
        try { Rig(scene, pages, drawer).also { it.frames(); test(it) } } finally { scene.close() }
    }

    private val mid = h.toFloat() * 2 / 2
    private val y get() = mid + 300

    @Test fun swipingTowardTheStartMovesToTheNextSectionInEnglish() = rig("en") { r ->
        assertEquals(Tab.Home, r.pages.last())
        r.drag(Offset(w * 2 * 0.8f, y), Offset(w * 2 * 0.2f, y))
        assertEquals(Tab.Money, r.pages.last(), "left swipe goes to the next section (pages ${r.pages})")
        r.drag(Offset(w * 2 * 0.2f, y), Offset(w * 2 * 0.8f, y))
        assertEquals(Tab.Home, r.pages.last(), "right swipe goes back")
        r.png("shell-en-home")
    }

    @Test fun theSwipeMirrorsInArabic() = rig("ar") { r ->
        assertEquals(Tab.Home, r.pages.last())
        r.drag(Offset(w * 2 * 0.2f, y), Offset(w * 2 * 0.8f, y))
        assertEquals(Tab.Money, r.pages.last(), "right swipe goes to the next section in RTL")
        r.png("shell-ar-money")
    }

    @Test fun theDrawerOpensByAnEdgeSwipeOnTheFirstSection() = rig("en") { r ->
        r.drag(Offset(4f, y), Offset(w * 2 * 0.7f, y))
        assertTrue(r.drawer.currentValue == DrawerValue.Open || r.drawer.targetValue == DrawerValue.Open, "drawer opened (state ${r.drawer.currentValue}, pages ${r.pages})")
        r.png("shell-en-drawer")
    }

    @Test fun theDrawerOpensFromTheRightEdgeInArabic() = rig("ar") { r ->
        r.drag(Offset(w * 2 - 4f, y), Offset(w * 2 * 0.3f, y))
        assertTrue(r.drawer.currentValue == DrawerValue.Open || r.drawer.targetValue == DrawerValue.Open, "drawer opened (state ${r.drawer.currentValue})")
        r.png("shell-ar-drawer")
    }

    @Test fun aSwipeInTheMiddleOfTheFirstSectionDoesNotOpenTheDrawer() = rig("en") { r ->
        r.drag(Offset(w * 2 * 0.3f, y), Offset(w * 2 * 0.8f, y))
        assertEquals(DrawerValue.Closed, r.drawer.currentValue)
        assertEquals(Tab.Home, r.pages.last())
    }

    @Test fun hiddenSectionsAreSkippedByTheSwipe() = rig("en", { it.copy(hiddenSections = setOf("money")) }) { r ->
        r.drag(Offset(w * 2 * 0.8f, y), Offset(w * 2 * 0.2f, y))
        assertEquals(Tab.Accounts, r.pages.last())
    }

    @Test fun theStartSectionIsHonoured() = rig("en", { it.copy(startSection = "plan") }) { r ->
        assertEquals(Tab.Plan, r.pages.last())
    }
}
