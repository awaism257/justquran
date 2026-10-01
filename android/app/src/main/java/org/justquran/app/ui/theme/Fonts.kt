package org.justquran.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em
import java.io.File
import java.io.FileOutputStream

class AppFonts(
    val arabic: FontFamily,
    val arabicAmiri: FontFamily,
    val urdu: FontFamily,
    val serif: FontFamily,
    val naskh: FontFamily
)

private val HONORIFIC_CHARS = charArrayOf('\uFD40', '\uFD41', '\uFDFA')
private var cached: AppFonts? = null

private fun assetFile(context: Context, name: String): File {
    val file = File(context.cacheDir, "jqfonts/$name")
    if (file.exists() && file.length() != 0L) {
        return file
    }
    file.parentFile?.mkdirs()
    context.assets.open("fonts/$name").use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}

fun loadAppFonts(context: Context): AppFonts {
    cached?.let { return it }
    val appContext = context.applicationContext
    val indoPak = Font(assetFile(appContext, "digitalkhatt-indopak-v2.otf"))
    val amiri = Font(assetFile(appContext, "amiri-quran.ttf"))
    val nastaliq = Font(assetFile(appContext, "nastaliq.ttf"))
    val notoNaskh = Font(assetFile(appContext, "noto-naskh.ttf"))

    val fonts = AppFonts(
        arabic = FontFamily(indoPak, amiri),
        arabicAmiri = FontFamily(amiri),
        urdu = FontFamily(nastaliq),
        serif = FontFamily.Serif,
        naskh = FontFamily(notoNaskh)
    )
    cached = fonts
    return fonts
}

@Composable
fun rememberAppFonts(): AppFonts {
    val context = LocalContext.current
    return remember(context) {
        loadAppFonts(context)
    }
}

fun withHonorifics(text: String, honorificFont: FontFamily): AnnotatedString {
    var hasHonorific = false
    for (i in 0 until text.length) {
        if (text[i] in HONORIFIC_CHARS) {
            hasHonorific = true
            break
        }
    }
    if (!hasHonorific) {
        return AnnotatedString(text)
    }

    val spanStyle = SpanStyle(
        fontSize = 0.9.em,
        fontFamily = honorificFont
    )
    val builder = AnnotatedString.Builder()
    for (i in 0 until text.length) {
        val c = text[i]
        if (c in HONORIFIC_CHARS) {
            builder.pushStyle(spanStyle)
            builder.append(c)
            builder.pop()
        } else {
            builder.append(c)
        }
    }
    return builder.toAnnotatedString()
}
