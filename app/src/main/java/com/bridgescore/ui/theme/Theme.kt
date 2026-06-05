package com.bridgescore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenTable = Color(0xFF1B5E20)
val GreenTableLight = Color(0xFF4CAF50)
val CardWhite = Color(0xFFFFFDE7)
val RedSuit = Color(0xFFCC0000)
val BlackSuit = Color(0xFF212121)

private val LightColors = lightColorScheme(
    primary = GreenTable,
    onPrimary = Color.White,
    secondary = GreenTableLight,
    background = CardWhite,
    surface = Color.White,
    onBackground = BlackSuit,
    onSurface = BlackSuit
)

@Composable
fun BridgeScoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
