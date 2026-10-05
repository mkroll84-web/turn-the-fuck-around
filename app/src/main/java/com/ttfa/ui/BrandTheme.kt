package com.ttfa.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ttfa.domain.ThemeMode

val BrandAqua = Color(0xFF00E0DF)
val BrandPink = Color(0xFFFF399C)
val BrandInk = Color(0xFF111218)
private val DarkBrand = darkColorScheme(
    primary = BrandAqua, onPrimary = BrandInk, primaryContainer = Color(0xFF004E50), onPrimaryContainer = Color(0xFFB4FFFF),
    secondary = BrandPink, onSecondary = BrandInk, secondaryContainer = Color(0xFF5B163E), onSecondaryContainer = Color(0xFFFFD9EC),
    background = BrandInk, onBackground = Color(0xFFF6F2F7), surface = Color(0xFF1B1D25), onSurface = Color(0xFFF6F2F7),
    surfaceVariant = Color(0xFF30343F), onSurfaceVariant = Color(0xFFD2D5DE), outline = Color(0xFF969EAD),
)
private val LightBrand = lightColorScheme(
    primary = Color(0xFF00686B), onPrimary = Color.White, primaryContainer = BrandAqua, onPrimaryContainer = BrandInk,
    secondary = Color(0xFFAA005C), onSecondary = Color.White, secondaryContainer = Color(0xFFFFD9EC), onSecondaryContainer = Color(0xFF47102E),
    background = Color(0xFFF8F7FA), onBackground = BrandInk, surface = Color(0xFFF8F7FA), onSurface = BrandInk,
    surfaceVariant = Color(0xFFE4E8ED), onSurfaceVariant = Color(0xFF414954), outline = Color(0xFF6D7681),
)
@Composable
fun BrandTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    MaterialTheme(colorScheme = if (dark) DarkBrand else LightBrand, content = content)
}

@Composable
fun BrandButton(onClick: () -> Unit, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
                enabled: Boolean = true, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = BrandAqua, contentColor = BrandInk), content = content)
}

@Composable
fun BrandSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    Switch(checked, onCheckedChange, modifier = modifier,
        colors = SwitchDefaults.colors(checkedTrackColor = BrandAqua, checkedThumbColor = BrandInk))
}
