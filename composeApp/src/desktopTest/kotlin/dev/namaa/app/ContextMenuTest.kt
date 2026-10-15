package dev.namaa.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The shared long-press menu: a tap does the main action, a long press ticks the haptic and opens the menu without doing the main action. */
@OptIn(ExperimentalComposeUiApi::class)
class ContextMenuTest {
    private class CountingHaptics : HapticFeedback { val types = ArrayList<HapticFeedbackType>(); override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { types += hapticFeedbackType } }

    private fun run(holdMillis: Long, picked: MutableList<String>, haptics: CountingHaptics, snap: String? = null) {
        val scene = ImageComposeScene(400, 400, Density(1f)) {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                NamaaTheme("dark") {
                    ContextMenuBox(listOf(MenuAction("Edit") { picked += "edit" }, MenuAction("Delete", destructive = true, confirm = "Delete?" to "Sure?") { picked += "delete" }), { picked += "tap" }, Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize()) { Text("Row") } }
                }
            }
        }
        try {
            var t = 0L
            fun frame(ms: Long) { t += ms; scene.render(t * 1_000_000) }
            frame(16)
            val p = Offset(100f, 100f)
            scene.sendPointerEvent(PointerEventType.Press, p, type = PointerType.Touch, timeMillis = t); frame(16)
            var held = 0L
            while (held < holdMillis) { Thread.sleep(50); frame(50); held += 50 }
            scene.sendPointerEvent(PointerEventType.Release, p, type = PointerType.Touch, timeMillis = t); frame(50); frame(50)
            if (snap != null) File("build/screenshots").apply { mkdirs() }.resolve("$snap.png").writeBytes(scene.render(t * 1_000_000 + 1).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        } finally { scene.close() }
    }

    @Test fun aTapDoesTheMainActionAndNoHaptic() {
        val picked = ArrayList<String>(); val h = CountingHaptics()
        run(50, picked, h)
        assertEquals(listOf("tap"), picked); assertTrue(h.types.isEmpty())
    }

    @Test fun aLongPressTicksOnceAndDoesNotRunTheMainAction() {
        val picked = ArrayList<String>(); val h = CountingHaptics()
        run(900, picked, h, snap = "context-menu-open")
        assertEquals(emptyList(), picked, "nothing ran yet, the menu is open")
        assertEquals(listOf(HapticFeedbackType.LongPress), h.types)
    }
}
