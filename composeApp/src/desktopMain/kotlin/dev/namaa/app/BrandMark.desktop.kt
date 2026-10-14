package dev.namaa.app

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource

@Composable
actual fun BrandMark(modifier: Modifier) {
    val image = remember { useResource("mark.png") { loadImageBitmap(it) } }
    Image(image, null, modifier)
}
