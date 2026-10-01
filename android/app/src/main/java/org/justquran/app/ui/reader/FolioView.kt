package org.justquran.app.ui.reader

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.justquran.app.data.AppSettings
import org.justquran.app.data.SurahMeta
import org.justquran.app.data.Verse
import org.justquran.app.data.displayArabic
import org.justquran.app.data.juzStartAt
import org.justquran.app.ui.drawWqGlyph
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.AppColors
import org.justquran.app.ui.theme.LightAppColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.wqDisplay
import org.justquran.app.ui.wqMarkTypeface
import org.justquran.app.ui.wqSortDisplay

const val VERSE_TAG = "verse"
const val FOLIO_BASE_AR = 23.0f
const val FOLIO_LINE_HEIGHT = 2.5f

fun chipWidthDp(v: Int): Float {
    return ((v.toString().length - 2).coerceAtLeast(0) * 7.5f) + 19.0f
}

@Composable
fun VerseNumberChip(
    number: Int,
    wq: String?,
    fc: AppColors
) {
    val fonts = rememberAppFonts()
    val context = LocalContext.current
    val paint = remember(fc.folioFrame) {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (fc.folioFrame.alpha * 255).toInt(),
                (fc.folioFrame.red * 255).toInt(),
                (fc.folioFrame.green * 255).toInt(),
                (fc.folioFrame.blue * 255).toInt()
            )
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(19.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    drawVerseMark(this, wq, paint, context)
                }
                .height(19.dp)
                .defaultMinSize(minWidth = 19.dp)
                .border(BorderStroke(1.dp, fc.folioFrame), CircleShape)
                .background(Color.Transparent)
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            val fontSize = if (number > 99) 9.sp else 10.5.sp
            Text(
                text = number.toString(),
                color = fc.folioFrame,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontFamily = fonts.serif,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun drawVerseMark(drawScope: DrawScope, wq: String?, paint: Paint, context: Context) {
    if (wq != null) {
        val display = wqSortDisplay(wqDisplay(wq))
        paint.textSize = with(drawScope) { 22.sp.toPx() }
        paint.typeface = wqMarkTypeface(context)
        val markSpacing = with(drawScope) { 10.dp.toPx() }
        val markOffset = with(drawScope) { 5.dp.toPx() }
        val centerX = drawScope.size.width / 2f
        val yOffset = with(drawScope) { -(2.dp.toPx()) }

        when (display.length) {
            1 -> drawWqGlyph(drawScope.drawContext.canvas.nativeCanvas, paint, display[0], centerX, yOffset)
            2 -> {
                drawWqGlyph(drawScope.drawContext.canvas.nativeCanvas, paint, display[0], centerX + markOffset, yOffset)
                drawWqGlyph(drawScope.drawContext.canvas.nativeCanvas, paint, display[1], centerX - markOffset, yOffset)
            }
            else -> {
                drawWqGlyph(drawScope.drawContext.canvas.nativeCanvas, paint, display[0], centerX, yOffset - markSpacing)
                drawWqGlyph(drawScope.drawContext.canvas.nativeCanvas, paint, display[1], centerX + markOffset, yOffset)
                drawWqGlyph(drawScope.drawContext.canvas.nativeCanvas, paint, display[2], centerX - markOffset, yOffset)
            }
        }
    }
}

fun buildFolioText(
    verses: List<Verse>,
    fc: AppColors,
    playingVerse: Int?,
    arSize: Float
): FolioBuild {
    val builder = AnnotatedString.Builder()
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val placeholders = mutableListOf<AnnotatedString.Range<Placeholder>>()

    for (verse in verses) {
        if (verse.s != 1 || verse.v != 1) {
            val juzNum = juzStartAt(verse.s, verse.v)
            if (juzNum != null) {
                val start = builder.length
                builder.append("۞ ")
                builder.addStyle(
                    SpanStyle(color = fc.folioFrame2),
                    start,
                    builder.length
                )
            }

            val vStart = builder.length
            builder.append(verse.displayArabic().trim())
            val chipId = "chip_${verse.v}"
            builder.append("\u2060")
            val chipStart = builder.length
            builder.appendInlineContent(chipId, " ")
            val vEnd = builder.length
            builder.addStringAnnotation(VERSE_TAG, verse.v.toString(), vStart, vEnd)

            if (juzNum != null) {
                builder.addStyle(
                    SpanStyle(background = fc.accent.copy(alpha = 0.13f)),
                    vStart,
                    vEnd
                )
            }

            if (playingVerse != null && verse.v == playingVerse) {
                builder.addStyle(
                    SpanStyle(background = fc.accent.copy(alpha = 0.22f)),
                    vStart,
                    vEnd
                )
            }

            val chipW = (chipWidthDp(verse.v) + 8.0f) / arSize
            val chipH = 19.0f / arSize
            val placeholder = Placeholder(
                width = chipW.em,
                height = chipH.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )

            inlineContent[chipId] = InlineTextContent(placeholder) {
                VerseNumberChip(number = verse.v, wq = verse.wq, fc = fc)
            }
            placeholders.add(AnnotatedString.Range(placeholder, chipStart, chipStart + 1))
            builder.append(" ")
        }
    }
    return FolioBuild(builder.toAnnotatedString(), inlineContent, placeholders)
}

@Composable
fun FolioBismillahBand(
    surahMeta: SurahMeta?,
    fc: AppColors,
    settings: AppSettings,
    playing: Boolean = false,
    fatiha: Boolean = false
) {
    val fonts = rememberAppFonts()
    val borderWidth = if (playing) 2.5.dp else if (fatiha) 2.dp else 1.dp
    val borderColor = if (playing) fc.accent else if (fatiha) fc.accent else fc.bandBorder
    val backgroundColor = if (playing) fc.accent.copy(alpha = 0.12f) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (surahMeta != null) {
            Text(
                text = surahMeta.nameEn.uppercase(Locale.ROOT),
                color = fc.folioFrame2,
                fontSize = 11.sp,
                fontFamily = fonts.serif,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center
            )
        }
        val fontArScale = if (settings.fontAr > 0) settings.fontAr else 100
        val fontAr = (FOLIO_BASE_AR * fontArScale) / 100f
        Text(
            text = BISMILLAH,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = fc.text,
            fontSize = fontAr.sp,
            fontFamily = fonts.arabic,
            textAlign = TextAlign.Center,
            lineHeight = (fontAr * 2.0f).sp
        )
    }
}

fun openVerseAt(
    pos: Offset,
    annotated: AnnotatedString,
    layoutResult: TextLayoutResult?,
    verses: List<Verse>,
    onVerseOpen: (Verse) -> Unit
) {
    if (layoutResult == null || annotated.isEmpty()) return
    val offset = layoutResult.getOffsetForPosition(pos).coerceIn(0, (annotated.length - 1).coerceAtLeast(0))
    val range = annotated.getStringAnnotations(VERSE_TAG, offset, offset + 1).firstOrNull() ?: return
    val verseNum = range.item.toIntOrNull() ?: return
    val verse = verses.firstOrNull { it.v == verseNum } ?: return
    onVerseOpen(verse)
}

fun verseLineTop(annotated: AnnotatedString, lr: TextLayoutResult, v: Int): Float? {
    val str = v.toString()
    val range = annotated.getStringAnnotations(VERSE_TAG, 0, annotated.length)
        .firstOrNull { it.item == str } ?: return null
    return lr.getLineTop(lr.getLineForOffset(range.start))
}

fun verseAtY(annotated: AnnotatedString, lr: TextLayoutResult, yInText: Float): Int? {
    if (annotated.isEmpty()) return null
    val line = lr.getLineForVerticalPosition(yInText.coerceAtLeast(0f))
    val offset = lr.getLineStart(line).coerceIn(0, (annotated.length - 1).coerceAtLeast(0))
    val range = annotated.getStringAnnotations(VERSE_TAG, offset, offset + 1).firstOrNull() ?: return null
    return range.item.toIntOrNull()
}

@Composable
fun FolioView(
    verses: List<Verse>,
    surah: Int,
    meta: SurahMeta?,
    settings: AppSettings,
    anchorVerse: Int?,
    anchorEnd: Boolean = false,
    onAnchorConsumed: () -> Unit,
    onVerseOpen: (Verse) -> Unit,
    onVisibleVerse: (Int) -> Unit,
    playingVerse: Int? = null,
    bismillahPlaying: Boolean = false,
    followVerse: Int? = null,
    onFollowConsumed: () -> Unit = {},
    pageTurn: Int? = null,
    onPageTurnConsumed: () -> Unit = {}
) {
    val fonts = rememberAppFonts()
    val colors = appColors
    val fc = if (settings.alwaysCreamPage) LightAppColors else colors

    val fontArScale = if (settings.fontAr > 0) settings.fontAr else 100
    val arSize = (FOLIO_BASE_AR * fontArScale) / 100f
    val folioBuild = remember(verses, fc, playingVerse, arSize) {
        buildFolioText(verses, fc, playingVerse, arSize)
    }

    val scrollState = rememberScrollState()
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textContentTop by remember { mutableFloatStateOf(0f) }
    var topReady by remember { mutableStateOf(false) }

    LaunchedEffect(layoutResult, topReady, anchorVerse) {
        val lr = layoutResult ?: return@LaunchedEffect
        if (!topReady) return@LaunchedEffect
        if (anchorEnd) {
            scrollState.scrollTo(scrollState.maxValue)
            onAnchorConsumed()
        } else if (anchorVerse != null) {
            val y = verseLineTop(folioBuild.text, lr, anchorVerse)
            if (y != null) {
                val target = ((textContentTop + y) - 16f).toInt().coerceIn(0, scrollState.maxValue)
                scrollState.scrollTo(target)
            }
            onAnchorConsumed()
        }
    }

    LaunchedEffect(pageTurn) {
        val dir = pageTurn ?: return@LaunchedEffect
        val delta = (scrollState.viewportSize * 0.85f).toInt().coerceAtLeast(1)
        val target = (scrollState.value + (dir * delta)).coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(target)
        onPageTurnConsumed()
    }

    LaunchedEffect(layoutResult, topReady, followVerse) {
        val lr = layoutResult ?: return@LaunchedEffect
        if (!topReady) return@LaunchedEffect
        val follow = followVerse ?: return@LaunchedEffect
        if (follow == 0) {
            scrollState.animateScrollTo(0)
            onFollowConsumed()
            return@LaunchedEffect
        }
        val y = verseLineTop(folioBuild.text, lr, follow)
        if (y != null) {
            val target = ((textContentTop + y) - 16f).toInt().coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(target)
        }
        onFollowConsumed()
    }

    LaunchedEffect(layoutResult, topReady) {
        val lr = layoutResult ?: return@LaunchedEffect
        if (!topReady) return@LaunchedEffect
        snapshotFlow { scrollState.value }.collect { scrollVal ->
            val v = verseAtY(folioBuild.text, lr, scrollVal - textContentTop)
            if (v != null) {
                onVisibleVerse(v)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        if (surah != 9) {
            FolioBismillahBand(
                surahMeta = meta,
                fc = fc,
                settings = settings,
                playing = bismillahPlaying,
                fatiha = (surah == 1)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .background(fc.folioPaper, RoundedCornerShape(8.dp))
                .padding(horizontal = 20.dp, vertical = 26.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, fc.folioFrame2), RoundedCornerShape(4.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 300.dp)
                        .background(fc.folioInner)
                        .border(
                            BorderStroke(if (surah == 1) 3.dp else 2.dp, if (surah == 1) fc.accent else fc.folioFrame)
                        )
                        .padding(horizontal = 18.dp, vertical = 26.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = folioBuild.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    textContentTop = coords.positionInRoot().y
                                }
                                .pointerInput(folioBuild.text) {
                                    detectTapGestures { offset ->
                                        openVerseAt(offset, folioBuild.text, layoutResult, verses, onVerseOpen)
                                    }
                                },
                            color = fc.arabic,
                            fontSize = arSize.sp,
                            fontFamily = fonts.arabic,
                            textAlign = TextAlign.Center,
                            lineHeight = (arSize * FOLIO_LINE_HEIGHT).sp,
                            inlineContent = folioBuild.inlineContent,
                            onTextLayout = { lr ->
                                layoutResult = lr
                                topReady = true
                            }
                        )
                    }
                }
            }
        }
    }
}
