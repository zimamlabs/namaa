package dev.namaa.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

/** The dot of the Arabic letter ز in Zimam's mark (a soft diamond with one corner cut), centred on 0 and one unit across. */
private val ZimamDot: Path by lazy {
    PathParser().parsePathString("M-0.02 0.50L-0.24 0.29Q-0.33 0.21 -0.39 0.16Q-0.45 0.12 -0.49 0.09V0.04Q-0.45 -0.02 -0.39 -0.10Q-0.32 -0.18 -0.24 -0.26Q-0.16 -0.35 -0.10 -0.41Q-0.03 -0.48 0.00 -0.50H0.05Q0.26 -0.37 0.37 -0.26Q0.49 -0.15 0.49 -0.03Q0.49 0.09 0.34 0.24Q0.20 0.38 0.02 0.50Z").toPath()
}

/** Draws the Zimam dot [size] across, centred on [center]. */
fun DrawScope.zimamDot(center: Offset, size: Float, color: Color) {
    if (size <= 0f) return
    withTransform({ translate(center.x, center.y); scale(size, size, Offset.Zero) }) { drawPath(ZimamDot, color) }
}
