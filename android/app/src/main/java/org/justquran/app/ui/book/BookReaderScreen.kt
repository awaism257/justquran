package org.justquran.app.ui.book

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import org.justquran.app.R
import org.justquran.app.data.SurahTitles
import org.justquran.app.ui.AudioBottomBar
import org.justquran.app.ui.CircleIconButton
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.justquran.app.AppContainer
import org.justquran.app.data.SettingsRepository
import org.justquran.app.data.Verse
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.reader.VersePopupSheet
import org.justquran.app.ui.reader.surahSwipe
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.fatihaBackground
import org.justquran.app.ui.tilesBackground
import org.justquran.app.ui.nav.Routes

@Composable
fun BookReaderScreen(
    container: AppContainer,
    nav: NavHostController,
    lang: String,
    surahNumber: Int
) {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val settings by appViewModel.settings.collectAsState()
    val bundleReady by appViewModel.bundleLoaded.collectAsState()
    val bookmarks by appViewModel.bookmarks.collectAsState()
    val fonts = rememberAppFonts()
    val colors = appColors
    val isUrdu = (lang == "ur")

    val surahMeta = if (bundleReady) appViewModel.surah(surahNumber) else null
    val verses = if (bundleReady) appViewModel.verses(surahNumber) else emptyList()

    val audio = container.audioController
    val currentVerse by audio.currentVerse.collectAsState()
    val isPlaying by audio.isPlaying.collectAsState()
    val autoAdvance by audio.autoAdvance.collectAsState()
    val chainActive by audio.chainActive.collectAsState()
    val chainSurah by audio.chainSurah.collectAsState()
    val activeAudio by audio.active.collectAsState()
    val isAuto = autoAdvance || chainActive

    // derivedStateOf prevents recompositions from audio ticks hitting non-relevant parts
    val isPlayingThisSurah by remember {
        derivedStateOf { (chainActive && chainSurah == surahNumber) || (activeAudio && currentVerse?.s == surahNumber) }
    }
    val isAudioPlayingThisSurah = (isPlaying || activeAudio) && (currentVerse?.s == surahNumber)
    val narratingVerse = if (isAudioPlayingThisSurah && currentVerse != null) currentVerse!!.v else null
    val yellowHighlight = if (colors.isDark) Color(0xFFFFD700) else Color(0xFFB57C00)

    val onToggleAuto = {
        if (isAuto) {
            audio.setAutoAdvance(false)
            audio.cancelChain()
        } else {
            audio.setAutoAdvance(true)
        }
    }

    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    var popupVerse by remember { mutableStateOf<Verse?>(null) }
    var popupFollow by remember { mutableStateOf(false) }

    LaunchedEffect(currentVerse) {
        val cur = currentVerse
        val popup = popupVerse
        if (popup != null && popupFollow && cur != null && cur.v >= 1) {
            if (cur.s != popup.s || cur.v != popup.v) {
                val next = appViewModel.verse(cur.s, cur.v)
                if (next != null) {
                    popupVerse = next
                }
            }
        }
    }

    val baseSize = if (isUrdu) 14f else 15f
    val rawScale = if (isUrdu) settings.fontUr else settings.fontEn
    val scale = if (rawScale > 0) rawScale else 100
    val fontSize = (baseSize * scale) / 100f
    val lineMult = if (isUrdu) 2.2f else 1.7f
    val family = if (isUrdu) fonts.urdu else fonts.serif
    val textColor = if (isUrdu) {
        if (colors.isDark) Color.White else colors.text
    } else {
        colors.english
    }
    val textStyle = TextStyle(
        color = textColor,
        fontSize = fontSize.sp,
        fontFamily = family,
        textAlign = TextAlign.Center,
        lineHeight = (fontSize * lineMult).sp
    )

    val bismillah = if (bundleReady && surahNumber != 1 && surahNumber != 9) {
        val v1 = appViewModel.verses(1).firstOrNull()
        if (isUrdu) v1?.ur else v1?.en
    } else null

    val bookText = remember(verses, isUrdu, bismillah, colors.accent, surahNumber) {
        buildBookText(verses, isUrdu, colors.accent, bismillah, surahNumber)
    }

    var page by remember(surahNumber) { mutableIntStateOf(0) }
    var pageCount by remember(surahNumber) { mutableIntStateOf(0) }
    var pageRanges by remember(surahNumber) { mutableStateOf<List<IntRange>>(emptyList()) }
    var turnDir by remember { mutableIntStateOf(0) }
    val turnMotion = remember { Animatable(1f) }
    var bookPageTurn by remember { mutableStateOf<Int?>(null) }

    val safePage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

    fun turn(dir: Int) {
        if (pageCount >= 1) {
            val target = safePage + dir
            if (target in 0 until pageCount) {
                turnDir = dir
                page = target
            } else if (dir > 0 && target >= pageCount && surahNumber < 114) {
                nav.navigate(Routes.book(lang, surahNumber + 1)) {
                    popUpTo(Routes.BOOK) { inclusive = true }
                }
            } else if (dir < 0 && target < 0 && surahNumber > 1) {
                nav.navigate(Routes.book(lang, surahNumber - 1)) {
                    popUpTo(Routes.BOOK) { inclusive = true }
                }
            }
        }
    }

    DisposableEffect(settings.volumeKeysTurnPages, bundleReady) {
        if (settings.volumeKeysTurnPages && bundleReady) {
            val handler: (Int) -> Boolean = { keyCode ->
                val delta = if (keyCode == 25) 1 else -1
                turn(delta)
                true
            }
            container.volumeKeyBus.handler = handler
            onDispose {
                if (container.volumeKeyBus.handler === handler) {
                    container.volumeKeyBus.handler = null
                }
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(bookPageTurn) {
        val dir = bookPageTurn ?: return@LaunchedEffect
        bookPageTurn = null
        turn(dir)
    }

    LaunchedEffect(pageCount) {
        if (pageCount > 0 && page > pageCount - 1) {
            page = pageCount - 1
        }
    }

    LaunchedEffect(safePage, pageRanges) {
        if (pageRanges.isNotEmpty()) {
            if (turnDir != 0) {
                turnMotion.snapTo(0f)
                turnMotion.animateTo(1f, tween(160))
                turnDir = 0
            }
            val range = pageRanges.getOrNull(safePage) ?: return@LaunchedEffect
            if (bookText.isNotEmpty() && range.last >= range.first) {
                val annotation = bookText.getStringAnnotations("verse", range.first, range.last).firstOrNull()
                val tag = annotation?.item
                if (tag != null) {
                    val lastRead = SettingsRepository.parseLastRead(tag)
                    if (lastRead != null) {
                        appViewModel.setLastRead(lastRead.first, lastRead.second)
                    }
                }
            }
        }
    }

    LaunchedEffect(currentVerse, pageRanges) {
        val cur = currentVerse
        if (isAudioPlayingThisSurah && cur != null && cur.s == surahNumber && cur.v >= 1 && pageRanges.isNotEmpty()) {
            val targetTag = "${cur.s}:${cur.v}"
            val targetPage = pageRanges.indexOfFirst { range ->
                bookText.getStringAnnotations("verse", range.first, range.last).any { it.item == targetTag } ||
                bookText.getStringAnnotations("verse_text", range.first, range.last).any { it.item == targetTag }
            }
            if (targetPage >= 0 && targetPage != page) {
                turnDir = if (targetPage > page) 1 else -1
                page = targetPage
            }
        }
    }

    val transLabel = if (isUrdu) "Urdu" else "English"
    val chapterTitle = if (isUrdu) {
        SurahTitles.urduName(surahNumber)
    } else {
        SurahTitles.englishName(surahNumber)
    }
    val title = "${surahNumber}. $chapterTitle"
    val subtitle = if (pageCount > 0) "Page ${safePage + 1} of $pageCount · $transLabel" else transLabel

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(
                title = title,
                subtitle = subtitle,
                titleFont = if (isUrdu) fonts.urdu else null,
                onBack = { nav.popBackStack() },
                actions = {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircleIconButton(
                        onClick = {
                            if (isPlayingThisSurah) {
                                audio.stopAll()
                            } else {
                                ensureNotificationPermission(context, notifLauncher)
                                audio.startTranslationChain(surahNumber, if (isUrdu) "ur" else "en")
                            }
                        },
                        color = Color.Transparent,
                        borderColor = if (isPlayingThisSurah) colors.accent else colors.hairline,
                        size = 38.dp
                    ) {
                        if (isPlayingThisSurah) {
                            Icon(
                                painter = painterResource(R.drawable.ic_stop_outline),
                                contentDescription = "Stop narration",
                                modifier = Modifier.size(15.dp),
                                tint = colors.accent
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_outline),
                                contentDescription = "Play narration",
                                modifier = Modifier.size(16.dp),
                                tint = colors.text
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (surahNumber == 1) Modifier.fatihaBackground() else Modifier.tilesBackground())
                .padding(padding)
        ) {

            if (!bundleReady) {
                Text(
                    text = "Loading…",
                    modifier = Modifier.padding(16.dp),
                    color = colors.muted,
                    fontFamily = fonts.serif
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .surahSwipe(
                            isRtl = isUrdu,
                            onSwipeNext = { turn(1) },
                            onSwipePrev = { turn(-1) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = BorderStroke(1.dp, colors.hairline)
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            val textMeasurer = rememberTextMeasurer()
                            val fullLayout = remember(constraints.maxWidth, bookText, textStyle) {
                                textMeasurer.measure(
                                    text = bookText,
                                    style = textStyle,
                                    constraints = Constraints(maxWidth = constraints.maxWidth),
                                    layoutDirection = if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
                                )
                            }
                            val calculatedPages = remember(constraints.maxHeight, fullLayout) {
                                paginateBook(fullLayout, bookText.length, constraints.maxHeight)
                            }
                            SideEffect {
                                pageRanges = calculatedPages
                                pageCount = calculatedPages.size
                            }
                            val range = calculatedPages.getOrNull(safePage)
                            if (range != null) {
                                val rawPageText = remember(bookText, range) {
                                    bookText.subSequence(range.first, range.last + 1)
                                }
                                val pageText = remember(rawPageText, narratingVerse, range, yellowHighlight) {
                                    if (narratingVerse == null) {
                                        rawPageText
                                    } else {
                                        val targetTag = "$surahNumber:$narratingVerse"
                                        val span = bookText.getStringAnnotations("verse_text", 0, bookText.length)
                                            .firstOrNull { it.item == targetTag }
                                        if (span == null) {
                                            rawPageText
                                        } else {
                                            val vStart = maxOf(range.first, span.start) - range.first
                                            val vEnd = minOf(range.last + 1, span.end) - range.first
                                            if (vEnd > vStart) {
                                                val b = AnnotatedString.Builder(rawPageText)
                                                b.addStyle(
                                                    SpanStyle(color = yellowHighlight),
                                                    vStart,
                                                    vEnd
                                                )
                                                b.toAnnotatedString()
                                            } else {
                                                rawPageText
                                            }
                                        }
                                    }
                                }
                                var pageLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                                val alpha = if (turnDir != 0) turnMotion.value else 1f
                                val translationX = if (turnDir != 0) {
                                    (1f - turnMotion.value) * (if (turnDir > 0) 40f else -40f)
                                } else 0f

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            this.alpha = alpha
                                            this.translationX = translationX
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pageText,
                                        style = textStyle,
                                        onTextLayout = { pageLayout = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(pageText) {
                                                detectTapGestures(
                                                    onTap = { offset ->
                                                        openBookVerseAt(offset, pageText, pageLayout, appViewModel) { v ->
                                                            popupVerse = v
                                                        }
                                                    },
                                                    onLongPress = { offset ->
                                                        openNearestBookVerseAt(offset, pageText, pageLayout, appViewModel) { v ->
                                                            popupVerse = v
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Bottom Bar: media controls replace Page footer when audio is playing ---
                val isAudioActive = activeAudio || chainActive
                if (isAudioActive) {
                    AudioBottomBar(
                        audio = audio,
                        currentSurah = surahNumber,
                        totalAyahs = surahMeta?.ayahs,
                        showAuto = false,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 6.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val canPrev = if (!isUrdu) safePage > 0 else safePage < pageCount - 1
                        IconButton(
                            onClick = { turn(if (isUrdu) 1 else -1) },
                            enabled = canPrev
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = if (isUrdu) "Next page" else "Previous page",
                                tint = if (canPrev) colors.accent else colors.muted
                            )
                        }

                        Text(
                            text = "Page ${safePage + 1} of ${pageCount.coerceAtLeast(1)}",
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontFamily = fonts.serif
                        )

                        val canNext = if (!isUrdu) safePage < pageCount - 1 else safePage > 0
                        IconButton(
                            onClick = { turn(if (isUrdu) -1 else 1) },
                            enabled = canNext
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isUrdu) "Previous page" else "Next page",
                                tint = if (canNext) colors.accent else colors.muted
                            )
                        }
                    }
                }
            }
        }
    }

    val selectedVerse = popupVerse
    if (selectedVerse != null) {
        val progressPct by audio.progressPct.collectAsState()
        val curRef = currentVerse
        val isCurrentVerse = (curRef != null && curRef.s == selectedVerse.s && curRef.v == selectedVerse.v)
        val playingThis = isPlaying && isCurrentVerse
        val activeThis = activeAudio && isCurrentVerse
        val bookmarked = bookmarks.contains("${selectedVerse.s}:${selectedVerse.v}")
        val hasPrev = selectedVerse.v > 1
        val maxAyahs = surahMeta?.ayahs ?: selectedVerse.v
        val hasNext = selectedVerse.v < maxAyahs
        val prog = if (activeThis) progressPct else 0f

        VersePopupSheet(
            verse = selectedVerse,
            bookmarked = bookmarked,
            onToggleBookmark = { appViewModel.toggleBookmark(selectedVerse.s, selectedVerse.v) },
            onDismiss = {
                popupFollow = false
                popupVerse = null
            },
            hasPrev = hasPrev,
            hasNext = hasNext,
            playingThis = playingThis,
            activeThis = activeThis,
            progressPct = prog,
            autoAdvance = isAuto,
            onPlayPause = {
                ensureNotificationPermission(context, notifLauncher)
                if (activeThis) {
                    audio.togglePlayPause()
                } else {
                    popupFollow = true
                    audio.play(selectedVerse.s, selectedVerse.v)
                }
            },
            onPrev = {
                val prevV = selectedVerse.v - 1
                if (prevV >= 1) {
                    val prev = appViewModel.verse(selectedVerse.s, prevV)
                    if (prev != null) {
                        popupVerse = prev
                        if (playingThis) {
                            audio.play(selectedVerse.s, prevV)
                        }
                    }
                }
            },
            onNext = {
                val nextV = selectedVerse.v + 1
                if (nextV <= maxAyahs) {
                    val next = appViewModel.verse(selectedVerse.s, nextV)
                    if (next != null) {
                        popupVerse = next
                        if (playingThis) {
                            audio.play(selectedVerse.s, nextV)
                        }
                    }
                }
            },
            onToggleAuto = onToggleAuto,
            compact = false,
            playbackOnly = false,
            totalVerses = maxAyahs
        )
    }
}

private fun ensureNotificationPermission(
    context: Context,
    launcher: ManagedActivityResultLauncher<String, Boolean>
) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun buildBookText(
    verses: List<Verse>,
    isUrdu: Boolean,
    accentColor: Color,
    bismillah: String?,
    surahNumber: Int
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    if (!bismillah.isNullOrBlank()) {
        val bStart = builder.length
        builder.append(bismillah.trim())
        val bEnd = builder.length
        builder.addStringAnnotation("verse_text", "$surahNumber:0", bStart, bEnd)
        builder.append("\n\n")
    }
    var first = true
    for (v in verses) {
        val text = if (isUrdu) v.ur else v.en
        if (!text.isNullOrBlank()) {
            if (!first) {
                builder.append("  ")
            }
            val verseStart = builder.length
            builder.append(text.trim())
            val textEnd = builder.length
            builder.append(" ")
            val start = builder.length
            builder.append(v.v.toString())
            builder.addStyle(
                SpanStyle(
                    color = accentColor,
                    fontSize = 0.6.em,
                    baselineShift = BaselineShift.Superscript
                ),
                start,
                builder.length
            )
            builder.addStringAnnotation("verse", "${v.s}:${v.v}", start, builder.length)
            builder.addStringAnnotation("verse_text", "${v.s}:${v.v}", verseStart, textEnd)
            first = false
        }
    }
    return builder.toAnnotatedString()
}

private fun String?.isNullExBlank(): Boolean {
    return this == null || this.isBlank()
}

private fun paginateBook(
    textLayout: TextLayoutResult,
    textLength: Int,
    maxHeightPx: Int
): List<IntRange> {
    if (textLength == 0 || textLayout.lineCount == 0) {
        return listOf(0 until textLength)
    }
    val lineHeight = textLayout.getLineBottom(0) - textLayout.getLineTop(0)
    if (lineHeight <= 0f) {
        return listOf(0 until textLength)
    }
    val limit = maxHeightPx.toFloat() - (lineHeight / 2f)
    val pages = mutableListOf<IntRange>()
    var lineIdx = 0
    while (lineIdx < textLayout.lineCount) {
        val top = textLayout.getLineTop(lineIdx)
        var endLine = lineIdx
        while (endLine + 1 < textLayout.lineCount && (textLayout.getLineBottom(endLine + 1) - top) <= limit) {
            endLine++
        }
        val startOffset = textLayout.getLineStart(lineIdx)
        val endOffset = if (endLine + 1 < textLayout.lineCount) textLayout.getLineStart(endLine + 1) else textLength
        if (endOffset > startOffset) {
            pages.add(startOffset until endOffset)
        }
        lineIdx = endLine + 1
    }
    return if (pages.isEmpty()) listOf(0 until textLength) else pages
}

fun openBookVerseAt(
    position: androidx.compose.ui.geometry.Offset,
    annotated: AnnotatedString,
    layoutResult: TextLayoutResult?,
    vm: AppViewModel,
    onVerseOpen: (Verse) -> Unit
) {
    if (layoutResult == null || annotated.isEmpty()) return
    val offset = layoutResult.getOffsetForPosition(position).coerceIn(0, (annotated.length - 1).coerceAtLeast(0))
    val range = annotated.getStringAnnotations("verse", offset, offset + 1).firstOrNull() ?: return
    val lastRead = SettingsRepository.parseLastRead(range.item) ?: return
    val verse = vm.verse(lastRead.first, lastRead.second) ?: return
    onVerseOpen(verse)
}

fun openNearestBookVerseAt(
    position: androidx.compose.ui.geometry.Offset,
    annotated: AnnotatedString,
    layoutResult: TextLayoutResult?,
    vm: AppViewModel,
    onVerseOpen: (Verse) -> Unit
) {
    if (layoutResult == null || annotated.isEmpty()) return
    val offset = layoutResult.getOffsetForPosition(position).coerceIn(0, (annotated.length - 1).coerceAtLeast(0))
    val all = annotated.getStringAnnotations("verse", 0, annotated.length)
    val exact = all.firstOrNull { offset in it.start..it.end }
    val candidate = exact ?: all.firstOrNull { it.start >= offset } ?: all.lastOrNull() ?: return
    val lastRead = SettingsRepository.parseLastRead(candidate.item) ?: return
    val verse = vm.verse(lastRead.first, lastRead.second) ?: return
    onVerseOpen(verse)
}
