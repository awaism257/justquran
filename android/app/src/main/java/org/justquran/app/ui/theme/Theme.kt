package org.justquran.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.justquran.app.data.ThemeChoice

object Tokens {
    val DarkBg = Color(0xFF101613)
    val Cream = Color(0xFFF0EBDC)
    val AccentGreen = Color(0xFFC9A658)
    val DarkCard = Color(0xFF2B2C2F)
    val DarkMuted = Cream.copy(alpha = 0.55f)
    val DarkHairline = AccentGreen.copy(alpha = 0.2f)
    val DarkVerseBorder = AccentGreen.copy(alpha = 0.2f)
    val DarkBandBorder = AccentGreen.copy(alpha = 0.45f)
    val DarkUrdu = Color(0xFFE4CC94)
    val DarkEnglish = Cream.copy(alpha = 0.85f)
    val DarkTranslit = Cream.copy(alpha = 0.4f)

    val LightBg = Color(0xFFEEE5CB)
    val LightCard = Color(0xFFFFFFFF)
    val LightInk = Color(0xFF262016)
    val LightMuted = LightInk.copy(alpha = 0.55f)
    val LightHairline = Color(0xFF9E7C3E).copy(alpha = 0.3f)
    val LightBandBorder = Color(0xFF9E7C3E).copy(alpha = 0.55f)
    val LightUrdu = Color(0xFF7A5E2A)
    val LightDivider = Color(0xFF9E7C3E)

    val NightFolioPaper = Color(28, 34, 30)
    val NightFolioInner = Color(22, 28, 24)
    val CreamFolioPaper = Color(243, 236, 216)
    val FolioFrame = Color(158, 124, 62)
    val FolioFrame2 = Color(190, 152, 84)
}

class AppColors(
    val bg: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val hairline: Color,
    val verseBorder: Color,
    val bandBorder: Color,
    val urdu: Color,
    val english: Color,
    val translit: Color,
    val divider: Color,
    val isDark: Boolean,
    val folioPaper: Color,
    val folioInner: Color,
    val folioFrame: Color,
    val folioFrame2: Color,
    val folioText: Color
) {
    val arabic: Color get() = text
}

val DarkAppColors = AppColors(
    bg = Tokens.DarkBg,
    card = Tokens.DarkCard,
    text = Tokens.Cream,
    muted = Tokens.DarkMuted,
    accent = Tokens.AccentGreen,
    hairline = Tokens.DarkHairline,
    verseBorder = Tokens.DarkVerseBorder,
    bandBorder = Tokens.DarkBandBorder,
    urdu = Tokens.DarkUrdu,
    english = Tokens.DarkEnglish,
    translit = Tokens.DarkTranslit,
    divider = Tokens.AccentGreen,
    isDark = true,
    folioPaper = Tokens.NightFolioPaper,
    folioInner = Tokens.NightFolioInner,
    folioFrame = Tokens.FolioFrame,
    folioFrame2 = Tokens.FolioFrame2,
    folioText = Tokens.Cream
)

val LightAppColors = AppColors(
    bg = Tokens.LightBg,
    card = Tokens.LightCard,
    text = Tokens.LightInk,
    muted = Tokens.LightMuted,
    accent = Tokens.LightDivider,
    hairline = Tokens.LightHairline,
    verseBorder = Tokens.LightHairline,
    bandBorder = Tokens.LightBandBorder,
    urdu = Tokens.LightUrdu,
    english = Tokens.LightInk.copy(alpha = 0.85f),
    translit = Tokens.LightInk.copy(alpha = 0.4f),
    divider = Tokens.LightDivider,
    isDark = false,
    folioPaper = Tokens.CreamFolioPaper,
    folioInner = Tokens.CreamFolioPaper,
    folioFrame = Tokens.FolioFrame,
    folioFrame2 = Tokens.FolioFrame2,
    folioText = Tokens.LightInk
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

private val DarkM3: ColorScheme = darkColorScheme(
    primary = Tokens.AccentGreen,
    onPrimary = Tokens.DarkBg,
    background = Tokens.DarkBg,
    onBackground = Tokens.Cream,
    surface = Tokens.DarkBg,
    onSurface = Tokens.Cream,
    surfaceVariant = Tokens.DarkCard,
    onSurfaceVariant = Tokens.Cream,
    outline = Tokens.DarkHairline
)

private val LightM3: ColorScheme = lightColorScheme(
    primary = Tokens.LightDivider,
    onPrimary = Tokens.LightCard,
    background = Tokens.LightBg,
    onBackground = Tokens.LightInk,
    surface = Tokens.LightBg,
    onSurface = Tokens.LightInk,
    surfaceVariant = Tokens.LightCard,
    onSurfaceVariant = Tokens.LightInk,
    outline = Tokens.LightHairline
)

val appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

@Composable
fun JustQuranTheme(
    themeChoice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeChoice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.DARK -> true
        ThemeChoice.LIGHT -> false
    }

    val colors = if (darkTheme) DarkAppColors else LightAppColors
    val m3Colors = if (darkTheme) DarkM3 else LightM3

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )
    }
}
