package org.justquran.app.ui

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.justquran.app.R
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import kotlin.math.max
import kotlin.math.roundToInt

const val WQ_MARK_SP: Float = 25.0f

private val WQ_PRIORITY: List<Char> = listOf(
    1752.toChar(), 1753.toChar(), 1556.toChar(), 1557.toChar(),
    2270.toChar(), 1754.toChar(), 2263.toChar(), 1750.toChar(),
    2261.toChar(), 1559.toChar(), 1755.toChar(), 2269.toChar(),
    2271.toChar(), 1769.toChar(), 2262.toChar()
)

private val WQ_INK_OFFSET: Map<String, Float> = mapOf(
    "ۙ" to -2.2f, "ؕ" to -2.0f, "ࣖ" to -1.2f, "ۚ" to -2.2f,
    "ۘ" to -2.5f, "ؗ" to -1.5f, "ؔ" to -4.0f, "ۙۛ" to -2.2f,
    "ؕۛ" to -1.8f, "ۖۙ" to -2.8f, "ۚۖ" to -2.8f, "ۚۙ" to -2.2f,
    "ࣗ" to -1.5f, "ࣖۚۛ" to -2.2f, "ؕؔ" to -4.0f, "ؗۙ" to -2.2f,
    "ࣕ" to -2.5f, "ۙۚۛ" to -2.2f, "ࣞ" to -3.8f, "ۚۖۛ" to -2.8f,
    "ؗۖ" to -2.8f, "۩ࣖ" to 0.2f, "ۖۚ" to -2.8f, "ؕࣖ" to -1.8f,
    "۩" to 0.2f, "ࣞۙ" to -3.8f, "ࣗۙ" to -2.2f, "ؕࣝ" to -5.2f,
    "ࣗۖ" to -2.8f, "ࣞۚ" to -3.8f, "ࣕۖ" to -2.8f, "۩ؕ" to 0.2f,
    "۩ۚ" to 0.2f, "ࣕۙ" to -2.5f, "ۚۛ" to -2.2f, "ࣗۖۛ" to -2.8f,
    "ۙۚ" to -2.2f, "ۖ" to -2.8f, "ࣟ" to -5.0f, "ۘؔ" to -3.8f,
    "ࣗؗ" to -1.5f
)

private var wqTypefaceCache: Typeface? = null

@Composable
fun Modifier.fatihaBackground(): Modifier {
    val isDark = appColors.isDark
    val image = ImageBitmap.imageResource(if (isDark) R.drawable.fatiha_dark else R.drawable.fatiha_light)
    val overlayTop = if (isDark) Color(16, 22, 19, 128) else Color(238, 229, 203, 115)
    val overlayBottom = if (isDark) Color(16, 22, 19, 184) else Color(238, 229, 203, 173)

    return this
        .background(appColors.bg)
        .drawBehind {
            val scale = max(size.width / image.width.toFloat(), size.height / image.height.toFloat())
            val drawW = image.width * scale
            val drawH = image.height * scale
            val left = (size.width - drawW) / 2f
            val top = (size.height - drawH) / 2f

            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                filterQuality = FilterQuality.High
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(overlayTop, overlayBottom),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
}

@Composable
fun Modifier.tilesBackground(): Modifier {
    val isDark = appColors.isDark
    val image = ImageBitmap.imageResource(if (isDark) R.drawable.tiles_dark else R.drawable.tiles_light)
    val shaderBrush = remember(image) {
        ShaderBrush(ImageShader(image, TileMode.Repeated, TileMode.Repeated))
    }

    return this
        .background(appColors.bg)
        .drawBehind {
            drawRect(brush = shaderBrush)
        }
}

@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(appColors.hairline)
    )
}

@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    borderColor: Color? = null,
    size: Dp = 38.dp,
    content: @Composable () -> Unit
) {
    val bg = color ?: Color.Transparent
    val borderCol = borderColor ?: color ?: appColors.hairline
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderCol, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BackBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    home: Boolean = false,
    titleFont: FontFamily? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val fonts = rememberAppFonts()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors.bg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(onClick = onBack) {
                if (home) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_home_outline),
                        contentDescription = "Home",
                        modifier = Modifier.size(18.dp),
                        tint = appColors.text
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp),
                        tint = appColors.text
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = appColors.text,
                    fontSize = if (subtitle != null) 18.sp else 21.sp,
                    fontFamily = titleFont ?: fonts.serif,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = appColors.muted,
                        fontSize = 12.sp,
                        fontFamily = fonts.serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            actions()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(appColors.divider)
        )
    }
}

@Composable
fun TilesScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tilesBackground(),
        contentAlignment = Alignment.TopStart
    ) {
        content()
    }
}

@Composable
fun Rtl(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier, contentAlignment = Alignment.TopStart) {
            content()
        }
    }
}

@Throws(PackageManager.NameNotFoundException::class)
fun appVersionLabel(context: Context): String {
    val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return "${packageInfo.versionName} (${packageInfo.longVersionCode}) · Build: native-${packageInfo.versionName}"
}

@Throws(PackageManager.NameNotFoundException::class)
fun appBuildLabel(context: Context): String {
    val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return "Build: native-${packageInfo.versionName} (${packageInfo.longVersionCode})"
}

fun wqDisplay(wq: String): String {
    return wq.replace("ࣛ", "۩")
}

fun wqSortDisplay(displayWq: String): String {
    return displayWq.toList().sortedWith(Comparator { a, b ->
        val idxA = WQ_PRIORITY.indexOf(a).let { if (it < 0) WQ_PRIORITY.size else it }
        val idxB = WQ_PRIORITY.indexOf(b).let { if (it < 0) WQ_PRIORITY.size else it }
        idxA.compareTo(idxB)
    }).joinToString("")
}

fun wqInkOffset(displayWq: String): Float {
    val offset = WQ_INK_OFFSET[displayWq] ?: 0f
    return offset * 1.4705882f
}

@Composable
fun WaqafMark(
    wq: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val sorted = remember(wq) { wqSortDisplay(wqDisplay(wq)) }
    val context = LocalContext.current
    val paint = remember(color) { wqMarkPaint(color) }
    val heightDp = (if (sorted.length > 1) 14 else 0) + 19

    Canvas(modifier = modifier.size(44.dp, heightDp.dp)) {
        paint.textSize = WQ_MARK_SP.sp.toPx()
        paint.typeface = wqMarkTypeface(context)
        val baseH = size.height - 3.dp.toPx()
        val stepY = 14.dp.toPx()
        val stepX = 8.dp.toPx()
        val centerX = size.width / 2f

        when (sorted.length) {
            1 -> {
                drawWqGlyph(drawContext.canvas.nativeCanvas, paint, sorted[0], centerX, baseH)
            }
            2 -> {
                drawWqGlyph(drawContext.canvas.nativeCanvas, paint, sorted[0], centerX, baseH - stepY)
                drawWqGlyph(drawContext.canvas.nativeCanvas, paint, sorted[1], centerX, baseH)
            }
            else -> {
                drawWqGlyph(drawContext.canvas.nativeCanvas, paint, sorted[0], centerX, baseH - stepY)
                drawWqGlyph(drawContext.canvas.nativeCanvas, paint, sorted[1], centerX + stepX, baseH)
                drawWqGlyph(drawContext.canvas.nativeCanvas, paint, sorted[2], centerX - stepX, baseH)
            }
        }
    }
}

fun wqMarkPaint(color: Color): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setColor(color.toArgb())
    }
}

fun wqMarkTypeface(context: Context): Typeface {
    wqTypefaceCache?.let { return it }
    val tf = runCatching {
        Typeface.createFromAsset(context.assets, "fonts/digitalkhatt-indopak-v2.otf")
    }.getOrNull() ?: Typeface.DEFAULT
    wqTypefaceCache = tf
    return tf
}

fun drawWqGlyph(canvas: Canvas, paint: Paint, c: Char, x: Float, y: Float) {
    val str = c.toString()
    val bounds = Rect()
    paint.getTextBounds(str, 0, 1, bounds)
    canvas.drawText(str, x - bounds.exactCenterX(), y - bounds.bottom, paint)
}
