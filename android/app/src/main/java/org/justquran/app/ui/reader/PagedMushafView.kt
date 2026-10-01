package org.justquran.app.ui.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.justquran.app.audio.ChainLogic
import org.justquran.app.data.AppSettings
import org.justquran.app.data.SurahMeta
import org.justquran.app.data.Verse
import org.justquran.app.ui.theme.AppFonts
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.LightAppColors
import org.justquran.app.ui.theme.rememberAppFonts
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val PAGED_LINE_HEIGHT = 2.0f
private const val TEXT_INSET_DP = 8.0f

private val mushafPaginationCache = object : LinkedHashMap<String, SurahPages>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SurahPages>?): Boolean {
        return size > 16
    }
}

private val mushafPageCountCache = HashMap<String, Map<Int, Int>>()

class VSpan(
    val start: Int,
    val end: Int,
    val firstLine: Int,
    val lastLine: Int
)

private fun measureBand(
    measurer: TextMeasurer,
    density: Density,
    fonts: AppFonts,
    fontAr: Int,
    arSize: Float
): BandMetrics {
    val bismillahSize = (fontAr * 22.0f) / 100.0f
    val surahH = measurer.measure(
        text = AnnotatedString("SURAH"),
        style = TextStyle(
            fontSize = 11.sp,
            fontFamily = fonts.serif,
            letterSpacing = 2.4.sp
        )
    ).size.height

    val bisH = measurer.measure(
        text = AnnotatedString(BISMILLAH),
        style = TextStyle(
            fontSize = bismillahSize.sp,
            fontFamily = fonts.arabic,
            lineHeight = (bismillahSize * PAGED_LINE_HEIGHT).sp
        ),
        layoutDirection = LayoutDirection.Rtl
    ).size.height

    val bandH = surahH + bisH + with(density) { 52.dp.roundToPx() }
    val clearance = max(
        ceil(
            (with(density) { PAGED_LINE_HEIGHT.dp.toPx() } +
             with(density) { 11.dp.toPx() } +
             with(density) { 25.sp.toPx() } * 0.55f) -
            ((with(density) { (arSize * PAGED_LINE_HEIGHT).sp.toPx() } - with(density) { 19.dp.toPx() }) / PAGED_LINE_HEIGHT)
        ).toInt(),
        0
    )
    return BandMetrics(bandH, clearance)
}

private fun verseOffsetOf(pages: SurahPages, v: Int): Int? {
    val str = v.toString()
    val annotation = pages.text.getStringAnnotations(VERSE_TAG, 0, pages.text.length)
        .firstOrNull { it.item == str }
    return annotation?.start
}

private fun pageOfPos(pages: SurahPages, pos: Int): Int {
    val ranges = pages.ranges
    if (ranges.isEmpty() || pages.text.isEmpty()) return 0
    val p = pos.coerceIn(0, pages.text.length - 1)
    val it = ranges.listIterator(ranges.size)
    while (it.hasPrevious()) {
        val range = it.previous()
        if (p >= range.first) {
            return it.nextIndex().coerceAtLeast(0)
        }
    }
    return 0
}

private fun pageOfVerse(pages: SurahPages, v: Int): Int {
    val ranges = pages.ranges
    if (ranges.isEmpty() || pages.text.isEmpty()) return 0
    val offset = if (v == Int.MAX_VALUE) {
        pages.text.length - 1
    } else {
        verseOffsetOf(pages, v) ?: 0
    }
    val it = ranges.listIterator(ranges.size)
    while (it.hasPrevious()) {
        val range = it.previous()
        if (offset >= range.first) {
            return it.nextIndex().coerceAtLeast(0)
        }
    }
    return 0
}

private fun firstVerseOf(pages: SurahPages, range: IntRange): Int {
    if (pages.text.isEmpty()) return 1
    val limit = (range.last + 1).coerceAtMost(pages.text.length)
    val annotations = pages.text.getStringAnnotations(VERSE_TAG, 0, pages.text.length)
    val inRange = annotations.filter { it.start >= range.first && it.start < limit }
    val first = inRange.minByOrNull { it.start }
    if (first != null) {
        return first.item.toIntOrNull() ?: 1
    }
    val previous = annotations.findLast { it.start <= range.first && it.end > range.first }
    return previous?.item?.toIntOrNull() ?: 1
}

fun paginateSurah(
    measurer: TextMeasurer,
    fonts: AppFonts,
    verses: List<Verse>,
    surah: Int,
    arSize: Float,
    maxW: Int,
    maxH: Int,
    band: BandMetrics,
    safetyPx: Int
): SurahPages {
    if (verses.isEmpty() || maxW <= 0 || maxH <= 0) {
        return SurahPages(emptyList(), AnnotatedString(""))
    }
    val build = buildFolioText(verses, LightAppColors, null, arSize)
    val text = build.text
    if (text.isEmpty()) {
        return SurahPages(emptyList(), text)
    }
    val layout = measurer.measure(
        text = text,
        style = TextStyle(
            fontSize = arSize.sp,
            fontFamily = fonts.arabic,
            textAlign = TextAlign.Center,
            lineHeight = (arSize * PAGED_LINE_HEIGHT).sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        ),
        placeholders = build.placeholders,
        constraints = Constraints(maxWidth = maxW),
        layoutDirection = LayoutDirection.Rtl
    )
    if (layout.lineCount == 0) {
        return SurahPages(listOf(0 until text.length), text)
    }
    val annotations = text.getStringAnnotations(VERSE_TAG, 0, text.length)
    if (annotations.isEmpty()) {
        return SurahPages(listOf(0 until text.length), text)
    }
    val spans = ArrayList<VSpan>()
    for (ann in annotations) {
        var start = ann.start
        if (start >= 2 && text[start - 2].code == 1758) {
            start -= 2
        }
        val end = ann.end
        if (end > start) {
            val firstL = layout.getLineForOffset(start)
            val lastL = layout.getLineForOffset(end - 1)
            spans.add(VSpan(start, end, firstL, lastL))
        }
    }
    val sortedSpans = spans.sortedBy { it.start }
    if (sortedSpans.isEmpty()) {
        return SurahPages(listOf(0 until text.length), text)
    }
    val starts = IntArray(sortedSpans.size + 1)
    for (idx in 0 until sortedSpans.size + 1) {
        starts[idx] = if (idx < sortedSpans.size) sortedSpans[idx].start else text.length
    }
    val resultRanges = ArrayList<IntRange>()
    var i = 0
    var pageIdx = 0
    while (i < sortedSpans.size) {
        val usableH = (if (pageIdx != 0 || surah == 9) maxH else maxH - band.bandH) - band.clearance - safetyPx
        val targetH = max(usableH, 1)
        val vSpan = sortedSpans[i]
        val top = layout.getLineTop(vSpan.firstLine)
        val spanH = layout.getLineBottom(vSpan.lastLine) - top
        if (spanH > targetH) {
            val totalLines = (vSpan.lastLine - vSpan.firstLine) + 1
            var linesFit = 0
            while (linesFit < totalLines && layout.getLineBottom(vSpan.firstLine + linesFit) - top <= targetH) {
                linesFit++
            }
            val step = max(linesFit, 1)
            val pageCount = (totalLines + step - 1) / step
            val linesPerPage = (totalLines + pageCount - 1) / pageCount
            var line = vSpan.firstLine
            while (line <= vSpan.lastLine) {
                var endLine = min(line + linesPerPage - 1, vSpan.lastLine)
                while (endLine > line && layout.getLineBottom(endLine) - layout.getLineTop(line) > targetH) {
                    endLine--
                }
                val startOffset = if (line == vSpan.firstLine) starts[i] else layout.getLineStart(line)
                val endOffset = if (endLine == vSpan.lastLine) starts[i + 1] else layout.getLineStart(endLine + 1)
                if (endOffset > startOffset) {
                    resultRanges.add(startOffset until endOffset)
                }
                line = endLine + 1
                pageIdx++
            }
            i++
        } else {
            var j = i
            do {
                j++
                if (j >= sortedSpans.size) break
            } while (layout.getLineBottom(sortedSpans[j].lastLine) - top <= targetH)
            resultRanges.add(starts[i] until starts[j])
            pageIdx++
            i = j
        }
    }
    return SurahPages(if (resultRanges.isEmpty()) listOf(0 until text.length) else resultRanges, text)
}

@Composable
fun EdgeStrip(
    enabled: Boolean,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val colors = appColors
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(22.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) colors.muted.copy(alpha = 0.55f) else Color.Transparent
        )
    }
}

private fun turn(
    page: Int,
    pageCount: Int,
    pages: SurahPages,
    turnDir: MutableIntState,
    anchorPos: MutableIntState,
    curSurah: MutableIntState,
    dir: Int
) {
    if (page < 0) return
    val newPage = page + dir
    if (newPage < 0 || newPage >= pageCount) {
        if (dir > 0 && curSurah.intValue < 114) {
            turnDir.intValue = 1
            curSurah.intValue += 1
            anchorPos.intValue = 0
        } else if (dir < 0 && curSurah.intValue > 1) {
            turnDir.intValue = -1
            curSurah.intValue -= 1
            anchorPos.intValue = Int.MAX_VALUE
        }
    } else {
        turnDir.intValue = dir
        anchorPos.intValue = pages.ranges[newPage].first
    }
}

@Composable
fun PagedMushafView(
    surah: Int,
    settings: AppSettings,
    anchorVerse: Int?,
    anchorEnd: Boolean = false,
    onAnchorConsumed: () -> Unit,
    onVerseOpen: (Verse) -> Unit,
    onVisibleVerse: (Int, Int) -> Unit,
    onPageChange: (label: String, canPrev: Boolean, canNext: Boolean) -> Unit,
    audioSurah: Int? = null,
    playingVerse: Int? = null,
    followTarget: Pair<Int, Int>? = null,
    onFollowConsumed: () -> Unit = {},
    pageTurn: Int? = null,
    onPageTurnConsumed: () -> Unit = {},
    versesOf: (Int) -> List<Verse>,
    metaOf: (Int) -> SurahMeta?
) {
    val appFonts = rememberAppFonts()
    val colors = if (settings.alwaysCreamPage) LightAppColors else appColors
    val fontArScale = if (settings.fontAr > 0) settings.fontAr else 100
    val fontAr = (fontArScale * 23.0f) / 100.0f
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val curSurah = remember { mutableIntStateOf(surah) }
    val entered = remember { mutableStateOf(false) }
    val anchorPos = remember { mutableIntStateOf(0) }
    val turnDir = remember { mutableIntStateOf(0) }
    val turnMotion = remember { Animatable(1.0f) }
    val localTurn = remember { mutableStateOf<Integer?>(null) }
    val edgeCanPrev = remember { mutableStateOf(false) }
    val edgeCanNext = remember { mutableStateOf(false) }

    val bandMetrics = remember(fontArScale, density, appFonts) {
        measureBand(textMeasurer, density, appFonts, fontArScale, fontAr)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EdgeStrip(
            enabled = edgeCanNext.value,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            description = "Next page",
            onClick = { localTurn.value = 1 as Integer }
        )

        Box(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight()
                .padding(bottom = 10.dp)
                .shadow(TEXT_INSET_DP.dp, RoundedCornerShape(TEXT_INSET_DP.dp))
                .background(colors.folioPaper, RoundedCornerShape(TEXT_INSET_DP.dp))
                .padding(12.dp, 12.dp)
        ) {
            val isFirstSurah = curSurah.intValue == 1
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        if (isFirstSurah) PAGED_LINE_HEIGHT.dp else 1.0.dp,
                        colors.folioFrame2
                    )
                    .padding(if (isFirstSurah) 4.dp else 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.folioInner)
                        .border(
                            (if (isFirstSurah) 3.0f else PAGED_LINE_HEIGHT).dp,
                            if (isFirstSurah) colors.accent else colors.folioFrame
                        )
                        .padding(14.dp, 14.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        val maxW = constraints.maxWidth
                        val maxH = constraints.maxHeight
                        val verses = versesOf(curSurah.intValue)
                        val textW = max(maxW - (density.run { TEXT_INSET_DP.dp.roundToPx() } * 2), 1)
                        val safetyPx = density.run { 4.dp.roundToPx() }

                        var contentReady by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(350L)
                            contentReady = true
                        }

                        val key = "${curSurah.intValue}|$textW|$maxH|$fontAr"
                        val pages = remember(verses, curSurah.intValue, textW, maxH, fontAr, bandMetrics, contentReady) {
                            val cached = mushafPaginationCache[key]
                            if (cached != null) {
                                cached
                            } else if (!contentReady) {
                                SurahPages(emptyList(), AnnotatedString(""))
                            } else {
                                val computed = paginateSurah(textMeasurer, appFonts, verses, curSurah.intValue, fontAr, textW, maxH, bandMetrics, safetyPx)
                                mushafPaginationCache[key] = computed
                                computed
                            }
                        }

                        val pageCount = pages.ranges.size
                        val page = if (!entered.value || pageCount <= 0) -1 else pageOfPos(pages, anchorPos.intValue)

                        LaunchedEffect(pages, anchorEnd, anchorVerse, onAnchorConsumed) {
                            if (entered.value || pages.ranges.isEmpty()) return@LaunchedEffect
                            val initial = if (anchorEnd) {
                                Int.MAX_VALUE
                            } else {
                                anchorVerse?.let { verseOffsetOf(pages, it) } ?: 0
                            }
                            anchorPos.intValue = initial
                            entered.value = true
                            onAnchorConsumed()
                        }

                        LaunchedEffect(pageTurn) {
                            val dir = pageTurn ?: return@LaunchedEffect
                            onPageTurnConsumed()
                            turn(page, pageCount, pages, turnDir, anchorPos, curSurah, dir)
                        }

                        LaunchedEffect(localTurn.value) {
                            val dir = localTurn.value ?: return@LaunchedEffect
                            localTurn.value = null
                            turn(page, pageCount, pages, turnDir, anchorPos, curSurah, dir.toInt())
                        }

                        LaunchedEffect(followTarget) {
                            val target = followTarget ?: return@LaunchedEffect
                            onFollowConsumed()
                            if (page >= 0 && target.second >= 1) {
                                if (target.first == curSurah.intValue) {
                                    val targetPage = pageOfVerse(pages, target.second)
                                    if (targetPage != page) {
                                        turnDir.intValue = if (targetPage <= page) -1 else 1
                                        anchorPos.intValue = pages.ranges[targetPage].first
                                    }
                                } else {
                                    turnDir.intValue = if (target.first <= curSurah.intValue) -1 else 1
                                    curSurah.intValue = target.first
                                    anchorPos.intValue = verseOffsetOf(pages, target.second) ?: 0
                                }
                            }
                        }

                        val countKey = "$textW|$maxH|$fontAr"
                        val pageCounts = remember(textW, maxH, fontAr, bandMetrics) {
                            val map = mutableStateMapOf<Int, Int>()
                            mushafPageCountCache[countKey]?.let { map.putAll(it) }
                            map
                        }

                        DisposableEffect(countKey) {
                            onDispose {
                                if (pageCounts.isNotEmpty()) {
                                    mushafPageCountCache[countKey] = HashMap(pageCounts)
                                }
                            }
                        }

                        LaunchedEffect(pageCounts, pages, contentReady) {
                            if (!contentReady) return@LaunchedEffect
                            if (pageCount > 0) {
                                pageCounts[curSurah.intValue] = pageCount
                            }
                            for (s in 1..114) {
                                if (pageCounts[s] == null) {
                                    val count = paginateSurah(textMeasurer, appFonts, versesOf(s), s, fontAr, textW, maxH, bandMetrics, safetyPx).ranges.size
                                    pageCounts[s] = count
                                    withFrameNanos { }
                                }
                            }
                            mushafPageCountCache[countKey] = HashMap(pageCounts)
                        }

                        LaunchedEffect(entered.value, page, pages, onVisibleVerse) {
                            if (page < 0) return@LaunchedEffect
                            if (turnDir.intValue != 0) {
                                turnMotion.snapTo(0.0f)
                                turnMotion.animateTo(1.0f, tween(160))
                                turnDir.intValue = 0
                            }
                            val pageFirst = pages.ranges[page].first
                            if (pageFirst != anchorPos.intValue) {
                                anchorPos.intValue = pageFirst
                            }
                            val verse = if (curSurah.intValue == 1 && page == 0) 1 else firstVerseOf(pages, pages.ranges[page])
                            onVisibleVerse(curSurah.intValue, verse)
                        }

                        if (page < 0) return@BoxWithConstraints

                        val prefixSum = (1 until curSurah.intValue).mapNotNull { pageCounts[it] }
                        val globalPage = if (prefixSum.size == curSurah.intValue - 1) prefixSum.sum() + page + 1 else null
                        val totalPages = if (pageCounts.size == 114) (1..ChainLogic.LAST_SURAH).sumOf { pageCounts[it] ?: 0 } else null

                        val label = if (globalPage != null && totalPages != null) {
                            "Page $globalPage of $totalPages"
                        } else if (globalPage != null) {
                            "Page $globalPage"
                        } else {
                            ""
                        }

                        val canPrev = curSurah.intValue > 1 || page > 0
                        val canNext = curSurah.intValue < 114 || page < pageCount - 1

                        SideEffect {
                            onPageChange(label, canPrev, canNext)
                            if (edgeCanPrev.value != canPrev) edgeCanPrev.value = canPrev
                            if (edgeCanNext.value != canNext) edgeCanNext.value = canNext
                        }

                        val activeVerse = if (playingVerse != null && playingVerse >= 1 && audioSurah == curSurah.intValue) playingVerse else null
                        val folioBuild = remember(verses, colors, activeVerse, fontAr) {
                            buildFolioText(verses, colors, activeVerse, fontAr)
                        }

                        val pageRange = pages.ranges[page]
                        val pageAnnotated = remember(folioBuild, pageRange) {
                            folioBuild.text.subSequence(pageRange.first, min(pageRange.last + 1, folioBuild.text.length))
                        }

                        val pageLayout = remember { mutableStateOf<TextLayoutResult?>(null) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val progress = turnMotion.value
                                    alpha = (progress * 0.65f) + 0.35f
                                    translationX = (1.0f - progress) * (-turnDir.intValue) * 26.dp.toPx()
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (page == 0 && curSurah.intValue != 9) {
                                FolioBismillahBand(
                                    surahMeta = metaOf(curSurah.intValue),
                                    fc = colors,
                                    settings = settings,
                                    playing = audioSurah == curSurah.intValue && ((playingVerse != null && playingVerse == 0) || (curSurah.intValue == 1 && playingVerse == 1)),
                                    fatiha = curSurah.intValue == 1
                                )
                            }

                            Spacer(modifier = Modifier.height(with(density) { bandMetrics.clearance.toDp() }))

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = pageAnnotated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = TEXT_INSET_DP.dp)
                                        .pointerInput(pageAnnotated) {
                                            detectTapGestures(
                                                onTap = { offset ->
                                                    openVerseAt(offset, pageAnnotated, pageLayout.value, verses, onVerseOpen)
                                                }
                                            )
                                        },
                                    color = colors.folioText,
                                    fontSize = fontAr.sp,
                                    fontFamily = appFonts.arabic,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (fontAr * PAGED_LINE_HEIGHT).sp,
                                    inlineContent = folioBuild.inlineContent,
                                    onTextLayout = { pageLayout.value = it },
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        EdgeStrip(
            enabled = edgeCanPrev.value,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = "Previous page",
            onClick = { localTurn.value = -1 as Integer }
        )
    }
}
}
