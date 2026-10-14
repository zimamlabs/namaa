package dev.zimma.app

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.zimma.money.R

@Composable
actual fun BrandMark(modifier: Modifier) { Image(painterResource(R.drawable.zimma_mark), null, modifier) }
