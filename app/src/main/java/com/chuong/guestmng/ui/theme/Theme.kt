package com.chuong.guestmng.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SophisticatedColorScheme = darkColorScheme(
    primary = Soph_Primary,
    onPrimary = Soph_OnPrimary,
    primaryContainer = Soph_ActiveBlueBg,
    onPrimaryContainer = Soph_ActiveBlueText,
    secondary = Soph_SurfaceVariant,
    onSecondary = Soph_OnSurfaceVariant,
    secondaryContainer = Soph_SurfaceVariant,
    onSecondaryContainer = Soph_OnSurfaceVariant,
    background = Soph_Bg,
    onBackground = Soph_Text,
    surface = Soph_Surface,
    onSurface = Soph_Text,
    surfaceVariant = Soph_SurfaceVariant,
    onSurfaceVariant = Soph_OnSurfaceVariant,
    outline = Soph_Outline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for Sophisticated Dark aesthetic
    dynamicColor: Boolean = false, // Disable dynamic colors by default to preserve theme colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> SophisticatedColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
