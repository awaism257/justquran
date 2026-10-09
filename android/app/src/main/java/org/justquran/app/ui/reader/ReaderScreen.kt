package org.justquran.app.ui.reader

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import org.justquran.app.ui.AudioBottomBar
import org.justquran.app.ui.book.PagedMode
import org.justquran.app.ui.book.PagedModePillsRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import org.justquran.app.ui.NumberBadge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.justquran.app.AppContainer
import org.justquran.app.R
import org.justquran.app.audio.AudioController
import org.justquran.app.data.AppSettings
import org.justquran.app.data.SurahMeta
import org.justquran.app.data.Verse
import org.justquran.app.data.displayArabic
import org.justquran.app.ui.theme.AppColors
import org.justquran.app.ui.theme.AppFonts
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.CircleIconButton
import org.justquran.app.ui.WaqafMark
import org.justquran.app.ui.fatihaBackground
import org.justquran.app.ui.nav.Routes
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground
import java.util.Locale

private const val BASE_AR = 26f
private const val BASE_UR = 14f
private const val BASE_EN = 15f
private const val BASE_TR = 12f
private const val SWIPE_THRESHOLD_DP = 72f
const val BISMILLAH = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ"

@Composable
private fun readerMaxWidth(): Dp {
    val sw = LocalConfiguration.current.screenWidthDp
    return when {
        sw >= 1600 -> 1200.dp
        sw >= 1200 -> 1040.dp
        sw >= 900 -> 880.dp
        else -> 720.dp
    }
}

private fun goToSurah(nav: NavHostController, surah: Int, end: Boolean = false) {
    nav.navigate(Routes.reader(surah, null, end)) {
        popUpTo(Routes.HOME)
    }
}

@Composable
fun Modifier.surahSwipe(
    isRtl: Boolean = true,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit
): Modifier {
    val next by rememberUpdatedState(onSwipeNext)
    val prev by rememberUpdatedState(onSwipePrev)
    return pointerInput(isRtl) {
        val thresh = SWIPE_THRESHOLD_DP.dp.toPx()
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (isRtl) {
                    // RTL (Arabic Mushaf, Urdu): dragging right (left-to-right) turns to next page/surah
                    if (total >= thresh) {
                        next()
                    } else if (total <= -thresh) {
                        prev()
                    }
                } else {
                    // LTR (English): dragging left (right-to-left) turns to next page/surah
                    if (total <= -thresh) {
                        next()
                    } else if (total >= thresh) {
                        prev()
                    }
                }
                total = 0f
            },
            onDragCancel = { total = 0f },
            onHorizontalDrag = { change, dragAmount ->
                total += dragAmount
                change.consume()
            }
        )
    }
}

private fun playingCardColor(playing: Boolean, card: Color, accent: Color): Color =
    if (playing) accent.copy(alpha = 0.1f).compositeOver(card) else card

private fun arabicIndic(n: Int): String =
    n.toString().map { (it.code + 1584).toChar() }.joinToString("")

private fun ensureNotificationPermission(
    context: Context,
    launcher: ManagedActivityResultLauncher<String, Boolean>
) {
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun verseIndex(hasBismillah: Boolean, v: Int): Int =
    if (v <= 0) 0 else ((if (hasBismillah) 1 else 0) + (v - 1))

private suspend fun snapToItem(listState: LazyListState, target: Int) {
    snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > target }
    listState.scrollToItem(target)
    repeat(2) {
        withFrameNanos { }
        if (listState.firstVisibleItemIndex != target) {
            listState.scrollToItem(target)
        }
    }
}

@Composable
private fun CardActionCircle(
    onClick: () -> Unit,
    description: String,
    filled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = appColors
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(colors.folioFrame2) else Modifier)
            .border(1.dp, if (filled) colors.folioFrame2 else colors.folioFrame, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun WebChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(if (selected) colors.accent else Color.Transparent)
            .border(1.dp, if (selected) colors.accent else colors.hairline, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF111113) else colors.muted,
            fontSize = 12.5.sp,
            fontFamily = fonts.serif
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsRow(
    settings: AppSettings,
    vm: AppViewModel,
    chainOnThisSurah: Boolean,
    onChainToggle: () -> Unit
) {
    val selectedCount = listOf(settings.showAr, settings.showEn, settings.showUr, settings.showTr).count { it }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WebChip(
            label = if (chainOnThisSurah) "✕ Stop" else "▶ Play surah",
            selected = chainOnThisSurah,
            enabled = true,
            onClick = onChainToggle
        )
        WebChip(
            label = "Arabic",
            selected = settings.showAr,
            enabled = !(selectedCount <= 1 && settings.showAr),
            onClick = { vm.setShowAr(!settings.showAr) }
        )
        WebChip(
            label = "English",
            selected = settings.showEn,
            enabled = !(selectedCount <= 1 && settings.showEn),
            onClick = { vm.setShowEn(!settings.showEn) }
        )
        WebChip(
            label = "اردو",
            selected = settings.showUr,
            enabled = !(selectedCount <= 1 && settings.showUr),
            onClick = { vm.setShowUr(!settings.showUr) }
        )
        WebChip(
            label = "Translit",
            selected = settings.showTr,
            enabled = !(selectedCount <= 1 && settings.showTr),
            onClick = { vm.setShowTr(!settings.showTr) }
        )
    }
}

@Composable
private fun AudioErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = colors.text,
                fontSize = 13.sp,
                fontFamily = fonts.serif
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = colors.muted
                )
            }
        }
    }
}

@Composable
private fun BismillahCard(
    settings: AppSettings,
    vm: AppViewModel,
    surah: Int,
    playing: Boolean = false
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    val meta = vm.surah(surah)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = playingCardColor(playing, colors.card, colors.accent)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (playing) 1.5.dp else 1.dp,
            if (playing) colors.accent else colors.bandBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (meta != null) {
                Text(
                    text = meta.nameEn.uppercase(Locale.ROOT),
                    modifier = Modifier.padding(bottom = 6.dp),
                    color = colors.folioFrame2,
                    fontSize = 11.sp,
                    fontFamily = fonts.serif,
                    letterSpacing = 2.4.sp,
                    textAlign = TextAlign.Center
                )
            }
            val fontArScale = if (settings.fontAr > 0) settings.fontAr else 100
            val fontTrScale = if (settings.fontTr > 0) settings.fontTr else 100
            val fontEnScale = if (settings.fontEn > 0) settings.fontEn else 100
            val fontUrScale = if (settings.fontUr > 0) settings.fontUr else 100
            Rtl {
                Text(
                    text = BISMILLAH,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.text,
                    fontSize = (fontArScale * BASE_AR / 100f).sp,
                    fontFamily = fonts.arabic,
                    textAlign = TextAlign.Center,
                    lineHeight = (fontArScale * BASE_AR / 100f * 2.0f).sp,
                    style = TextStyle(
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            }
            if (!settings.arabicOnly) {
                val v1 = vm.verse(1, 1)
                val trText = v1?.tr ?: "Bismillaahir Rahmaanir Raheem"
                val enText = v1?.en ?: "In the name of Allah, the Entirely Merciful, the Especially Merciful."
                val urText = v1?.ur ?: "شروع اللہ کا نام لے کر جو بڑا مہربان نہایت رحم والا ہے"
                if (settings.showTr) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = trText,
                        color = colors.translit,
                        fontSize = (fontTrScale * BASE_TR / 100f).sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = fonts.serif,
                        textAlign = TextAlign.Center
                    )
                }
                if (settings.showEn) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = enText,
                        color = colors.english,
                        fontSize = (fontEnScale * BASE_EN / 100f).sp,
                        fontFamily = fonts.serif,
                        textAlign = TextAlign.Center,
                        lineHeight = (fontEnScale * BASE_EN / 100f * 1.7f).sp
                    )
                }
                if (settings.showUr) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Rtl {
                        Text(
                            text = urText,
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.urdu,
                            fontSize = (fontUrScale * BASE_UR / 100f).sp,
                            fontFamily = fonts.urdu,
                            textAlign = TextAlign.Center,
                            lineHeight = (fontUrScale * BASE_UR / 100f * 2.1f).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseCard(
    verse: Verse,
    settings: AppSettings,
    playing: Boolean = false,
    bookmarked: Boolean = false,
    onToggleBookmark: () -> Unit,
    onPlay: () -> Unit,
    onOpenVerse: () -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val fontAr = ((if (settings.fontAr > 0) settings.fontAr else 100) * BASE_AR) / 100f
    val fontUr = ((if (settings.fontUr > 0) settings.fontUr else 100) * BASE_UR) / 100f
    val fontEn = ((if (settings.fontEn > 0) settings.fontEn else 100) * BASE_EN) / 100f
    val fontTr = ((if (settings.fontTr > 0) settings.fontTr else 100) * BASE_TR) / 100f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onOpenVerse),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = playingCardColor(playing, colors.card, colors.accent)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (playing) 1.5.dp else 1.dp,
            if (playing) colors.accent else colors.verseBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (verse.wq != null) {
                        WaqafMark(verse.wq, colors.accent)
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    NumberBadge(
                        number = verse.v,
                        backgroundColor = colors.accent,
                        textColor = colors.bg,
                        fontFamily = fonts.serif,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardActionCircle(
                        onClick = onToggleBookmark,
                        description = if (bookmarked) "Remove bookmark" else "Bookmark verse"
                    ) {
                        Icon(
                            if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = colors.folioFrame2
                        )
                    }
                    CardActionCircle(
                        onClick = {
                            clipboard.setText(AnnotatedString(verseShareText(verse)))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        description = "Copy verse"
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = colors.folioFrame2
                        )
                    }
                    CardActionCircle(
                        onClick = onPlay,
                        description = if (playing) "Pause this verse" else "Play this verse",
                        filled = playing
                    ) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (playing) colors.card else colors.folioFrame2
                        )
                    }
                }
            }

            if (settings.showAr) {
                Spacer(modifier = Modifier.height(12.dp))
                Rtl {
                    Text(
                        text = verse.displayArabic(),
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.text,
                        fontSize = fontAr.sp,
                        fontFamily = fonts.arabic,
                        textAlign = TextAlign.Center,
                        lineHeight = (2f * fontAr).sp,
                        style = TextStyle(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }
            }

            if (!settings.arabicOnly) {
                if (settings.showTr) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = verse.tr,
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.translit,
                        fontSize = fontTr.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = fonts.serif,
                        textAlign = TextAlign.Center,
                        lineHeight = (1.6f * fontTr).sp
                    )
                }
                if (settings.showEn) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = verse.en,
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.english,
                        fontSize = fontEn.sp,
                        fontFamily = fonts.serif,
                        textAlign = TextAlign.Center,
                        lineHeight = (1.7f * fontEn).sp
                    )
                }
                if (settings.showUr && verse.ur != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Rtl {
                        Text(
                            text = verse.ur,
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.urdu,
                            fontSize = fontUr.sp,
                            fontFamily = fonts.urdu,
                            textAlign = TextAlign.Center,
                            lineHeight = (2.2f * fontUr).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahPager(verseCount: Int, surah: Int, nav: NavHostController) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val canNextSurah = surah < 114
        IconButton(
            onClick = { if (canNextSurah) goToSurah(nav, surah + 1) },
            enabled = canNextSurah
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Next surah",
                tint = if (canNextSurah) colors.accent else colors.muted
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$verseCount verses",
            color = colors.muted,
            fontSize = 12.sp,
            fontFamily = fonts.serif
        )
        Spacer(modifier = Modifier.weight(1f))
        val canPrevSurah = surah > 1
        IconButton(
            onClick = { if (canPrevSurah) goToSurah(nav, surah - 1) },
            enabled = canPrevSurah
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Previous surah",
                tint = if (canPrevSurah) colors.accent else colors.muted
            )
        }
    }
}

@Composable
private fun MushafPagePager(
    label: String,
    canPrev: Boolean,
    canNext: Boolean,
    onTurn: (Int) -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (canNext) onTurn(1) },
            enabled = canNext
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Next page",
                tint = if (canNext) colors.accent else colors.muted
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = label,
            color = colors.muted,
            fontSize = 12.sp,
            fontFamily = fonts.serif
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = { if (canPrev) onTurn(-1) },
            enabled = canPrev
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Previous page",
                tint = if (canPrev) colors.accent else colors.muted
            )
        }
    }
}

@Composable
private fun Rtl(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
fun ReaderScreen(
    container: AppContainer,
    nav: NavHostController,
    surah: Int,
    anchorVerse: Int? = null,
    anchorEnd: Boolean = false
) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val ready by vm.bundleReady.collectAsState()
    val settings by vm.settings.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState()
    val audioController = container.audioController

    val currentVerse by audioController.currentVerse.collectAsState()
    val isPlaying by audioController.isPlaying.collectAsState()
    val autoAdvance by audioController.autoAdvance.collectAsState()
    val chainActive by audioController.chainActive.collectAsState()
    val chainSurah by audioController.chainSurah.collectAsState()
    val prelude by audioController.prelude.collectAsState()
    val audioActive by audioController.active.collectAsState()
    val audioError by audioController.error.collectAsState()

    val toggleAutoAdvance = remember(autoAdvance, audioController) {
        {
            if (!autoAdvance || chainActive) {
                audioController.toggleAutoAdvance()
            }
        }
    }

    var activeSurah by remember(surah) { mutableIntStateOf(surah) }
    val meta = vm.surah(activeSurah)
    val activeMeta = meta
    val chainOnThisSurah = chainActive && chainSurah == activeSurah
    val playingVerse = if (audioActive && currentVerse != null && currentVerse!!.s == activeSurah) {
        currentVerse!!.v
    } else {
        null
    }
    val isBismPlaying = audioActive && (
        (currentVerse != null && currentVerse!!.s == activeSurah && currentVerse!!.v == 0) ||
        (prelude && (chainSurah == activeSurah || currentVerse?.s == activeSurah)) ||
        (activeSurah == 1 && playingVerse == 1)
    )

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    var popupVerse by remember { mutableStateOf<Verse?>(null) }
    var popupFollow by remember { mutableStateOf(false) }

    var topVerse by remember { mutableIntStateOf(anchorVerse ?: 1) }
    var switchAnchor by remember { mutableStateOf<Int?>(null) }
    var cardAnchorDone by remember { mutableStateOf(false) }
    var folioAnchorDone by remember { mutableStateOf(false) }

    var turnRequest by remember { mutableStateOf<Int?>(null) }
    var volumeTurnRequest by remember { mutableStateOf<Int?>(null) }
    var pageLabel by remember { mutableStateOf("") }
    var pageCanPrev by remember { mutableStateOf(false) }
    var pageCanNext by remember { mutableStateOf(false) }

    var folioFollowVerse by remember { mutableStateOf<Int?>(null) }
    var pagedFollowVerse by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val verses = vm.verses(activeSurah)
    val hasBismillah = activeSurah != 1 && activeSurah != 9
    val folioMode = settings.arabicOnly
    val pagedMushaf = settings.arabicOnly && settings.mushafPaged

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Anchor effect for card view
    LaunchedEffect(ready, folioMode, anchorVerse, anchorEnd) {
        if (ready && !folioMode && !cardAnchorDone) {
            if (switchAnchor != null) {
                cardAnchorDone = true
                return@LaunchedEffect
            }
            val target = if (anchorEnd) {
                verseIndex(hasBismillah, meta?.ayahs ?: 1)
            } else if (anchorVerse != null && anchorVerse > 1) {
                verseIndex(hasBismillah, anchorVerse.coerceIn(1, meta?.ayahs ?: anchorVerse))
            } else if (anchorVerse != null && anchorVerse <= 1) {
                0
            } else null
            if (target != null) {
                snapToItem(listState, target)
            }
            cardAnchorDone = true
        }
    }

    // Scroll listener for card view to track topVerse and lastRead
    LaunchedEffect(ready, folioMode, cardAnchorDone) {
        if (ready && !folioMode && cardAnchorDone) {
            snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
                val v = (index - if (hasBismillah) 1 else 0) + 1
                val clamped = v.coerceIn(1, meta?.ayahs ?: 1)
                topVerse = clamped
                vm.setLastRead(activeSurah, clamped)
            }
        }
    }

    // Anchor effect when toggling views via switchAnchor
    LaunchedEffect(switchAnchor) {
        val v = switchAnchor
        if (v != null) {
            if (!ready || folioMode) return@LaunchedEffect
            val ayahs = meta?.ayahs ?: v
            snapToItem(listState, verseIndex(hasBismillah, v.coerceIn(1, ayahs)))
            switchAnchor = null
            cardAnchorDone = true
        }
    }

    // Volume keys listener
    val currentAudioActive by rememberUpdatedState(audioActive)
    val currentPagedMushaf by rememberUpdatedState(pagedMushaf)
    val currentTopVerse by rememberUpdatedState(topVerse)
    val currentTotalAyahs by rememberUpdatedState(meta?.ayahs ?: verses.size)

    DisposableEffect(settings.volumeKeysTurnPages, ready) {
        if (settings.volumeKeysTurnPages && ready) {
            val handler: (Int) -> Boolean = { keyCode ->
                val delta = if (keyCode == 25) 1 else -1
                if (currentAudioActive) {
                    false
                } else if (!currentPagedMushaf) {
                    coroutineScope.launch {
                        val target = (currentTopVerse + delta).coerceIn(1, currentTotalAyahs.coerceAtLeast(1))
                        snapToItem(listState, verseIndex(hasBismillah, target))
                    }
                    true
                } else {
                    volumeTurnRequest = delta
                    true
                }
            }
            container.volumeKeyBus.handler = handler
            onDispose {
                if (container.volumeKeyBus.handler === handler) {
                    container.volumeKeyBus.handler = null
                }
            }
        } else {
            onDispose { }
        }
    }

    // Follow playing verse in reader view
    LaunchedEffect(currentVerse, isPlaying, ready, folioMode, pagedMushaf) {
        val cv = currentVerse
        if (!ready || !isPlaying || cv == null || cv.v < 0) return@LaunchedEffect
        if (pagedMushaf) {
            pagedFollowVerse = cv.s to cv.v
        } else if (folioMode) {
            if (cv.s == activeSurah) {
                folioFollowVerse = cv.v
            }
        } else {
            if (cv.s == activeSurah) {
                val ayahs = meta?.ayahs ?: cv.v
                val targetIndex = if (cv.v == 0) 0 else verseIndex(hasBismillah, cv.v.coerceIn(1, ayahs))
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Popup follow update
    LaunchedEffect(currentVerse) {
        val cv = currentVerse
        val pv = popupVerse
        if (pv != null && popupFollow && cv != null && cv.v >= 1 && (cv.s != pv.s || cv.v != pv.v)) {
            val v = vm.verse(cv.s, cv.v)
            if (v != null) {
                popupVerse = v
            }
        }
    }

    val maxWidth = readerMaxWidth()
    val colors = appColors
    val fonts = rememberAppFonts()

    fun onSwipeNextAction() {
        if (pagedMushaf) {
            turnRequest = 1
        } else if (surah < 114) {
            goToSurah(nav, surah + 1, false)
        }
    }

    fun onSwipePrevAction() {
        if (pagedMushaf) {
            turnRequest = -1
        } else if (surah > 1) {
            goToSurah(nav, surah - 1, true)
        }
    }

    Scaffold(
        topBar = {
            BackBar(
                title = if (activeMeta != null) "${activeMeta.n}. ${activeMeta.nameEn}" else "Surah $activeSurah",
                subtitle = if (activeMeta != null) "${activeMeta.ayahs} verses · Juz ${org.justquran.app.data.juzOf(activeSurah, topVerse)}" else null,
                home = true,
                onBack = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = false } } },
                actions = {
                    if (ready) {
                        CircleIconButton(
                            onClick = {
                                if (chainOnThisSurah) {
                                    audioController.stopAll()
                                } else {
                                    ensureNotificationPermission(context, permissionLauncher)
                                    audioController.startChain(activeSurah, 1)
                                }
                            },
                            color = Color.Transparent,
                            borderColor = if (chainOnThisSurah) colors.accent else colors.hairline
                        ) {
                            if (chainOnThisSurah) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_stop_outline),
                                    contentDescription = "Stop recitation",
                                    modifier = Modifier.size(15.dp),
                                    tint = colors.accent
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_outline),
                                    contentDescription = "Play surah",
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.text
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    CircleIconButton(
                        onClick = {
                            vm.toggleArabicOnly()
                            switchAnchor = topVerse
                        },
                        color = Color.Transparent,
                        borderColor = if (settings.arabicOnly) colors.accent else colors.hairline
                    ) {
                        if (settings.arabicOnly) {
                            Icon(
                                painter = painterResource(R.drawable.ic_rows3),
                                contentDescription = "Verse-by-verse view",
                                modifier = Modifier.size(16.dp),
                                tint = colors.accent
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_book_open_text),
                                contentDescription = "Arabic-only continuous view",
                                modifier = Modifier.size(16.dp),
                                tint = colors.text
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (ready) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val ayahs = activeMeta?.ayahs ?: verses.size
                    val isAudioActive = audioActive || chainActive
                    if (isAudioActive) {
                        AudioBottomBar(
                            audio = audioController,
                            currentSurah = activeSurah,
                            totalAyahs = ayahs,
                            showAuto = true
                        )
                    } else if (pagedMushaf) {
                        MushafPagePager(
                            label = pageLabel,
                            canPrev = pageCanPrev,
                            canNext = pageCanNext,
                            onTurn = { turnRequest = it }
                        )
                    } else {
                        SurahPager(
                            verseCount = ayahs,
                            surah = activeSurah,
                            nav = nav
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val isRtlReading = pagedMushaf || settings.arabicOnly || (settings.showUr && !settings.showEn && !settings.showTr)
        val bgModifier = if (activeSurah == 1) Modifier.fatihaBackground() else Modifier.tilesBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .padding(paddingValues)
                .surahSwipe(
                    isRtl = isRtlReading,
                    onSwipeNext = { onSwipeNextAction() },
                    onSwipePrev = { onSwipePrevAction() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (ready && !settings.arabicOnly) {
                Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
                    ChipsRow(
                        settings = settings,
                        vm = vm,
                        chainOnThisSurah = chainOnThisSurah,
                        onChainToggle = {
                            if (chainOnThisSurah) {
                                audioController.stopAll()
                            } else {
                                ensureNotificationPermission(context, permissionLauncher)
                                audioController.startChain(activeSurah, 1)
                            }
                        }
                    )
                }
            } else if (ready && settings.arabicOnly) {
                Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
                    PagedModePillsRow(
                        currentMode = PagedMode.ARABIC,
                        onSelectArabic = { /* Already in Arabic */ },
                        onSelectEnglish = {
                            if (audioActive || chainActive) {
                                audioController.stopAll()
                            }
                            nav.navigate(Routes.book("en", activeSurah, topVerse))
                        },
                        onSelectUrdu = {
                            if (audioActive || chainActive) {
                                audioController.stopAll()
                            }
                            nav.navigate(Routes.book("ur", activeSurah, topVerse))
                        }
                    )
                }
            }

            audioError?.let { err ->
                Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
                    AudioErrorBanner(
                        message = err,
                        onDismiss = { audioController.clearError() }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!ready) {
                    Text(
                        text = "Loading…",
                        color = colors.muted,
                        fontSize = 16.sp,
                        fontFamily = fonts.serif
                    )
                } else if (settings.arabicOnly) {
                    if (settings.mushafPaged) {
                        val initialVerse = switchAnchor ?: if (folioAnchorDone) null else anchorVerse
                        val initialEnd = if (switchAnchor != null || folioAnchorDone) false else anchorEnd
                        PagedMushafView(
                            surah = surah,
                            settings = settings,
                            anchorVerse = initialVerse,
                            anchorEnd = initialEnd,
                            onAnchorConsumed = {
                                folioAnchorDone = true
                                switchAnchor = null
                            },
                            onVerseOpen = { popupVerse = it },
                            onVisibleVerse = { s, v ->
                                activeSurah = s
                                topVerse = v
                                vm.setLastRead(s, v)
                            },
                            onPageChange = { label, prev, next ->
                                if (pageLabel != label) pageLabel = label
                                if (pageCanPrev != prev) pageCanPrev = prev
                                if (pageCanNext != next) pageCanNext = next
                            },
                            audioSurah = if (audioActive && currentVerse != null) currentVerse!!.s else null,
                            playingVerse = playingVerse,
                            followTarget = pagedFollowVerse,
                            onFollowConsumed = { pagedFollowVerse = null },
                            pageTurn = turnRequest ?: volumeTurnRequest,
                            onPageTurnConsumed = {
                                turnRequest = null
                                volumeTurnRequest = null
                            },
                            versesOf = { s -> vm.verses(s) },
                            metaOf = { s -> vm.surah(s) }
                        )
                    } else {
                        val initialVerse = switchAnchor ?: if (folioAnchorDone) null else anchorVerse
                        val initialEnd = if (switchAnchor != null || folioAnchorDone) false else anchorEnd
                        FolioView(
                            verses = verses,
                            surah = activeSurah,
                            meta = meta,
                            settings = settings,
                            anchorVerse = initialVerse,
                            anchorEnd = initialEnd,
                            onAnchorConsumed = {
                                folioAnchorDone = true
                                switchAnchor = null
                            },
                            onVerseOpen = { popupVerse = it },
                            onVisibleVerse = { v ->
                                topVerse = v
                                vm.setLastRead(activeSurah, v)
                            },
                            playingVerse = if (playingVerse != null && playingVerse >= 1) playingVerse else null,
                            bismillahPlaying = isBismPlaying,
                            followVerse = folioFollowVerse,
                            onFollowConsumed = { folioFollowVerse = null },
                            pageTurn = volumeTurnRequest,
                            onPageTurnConsumed = { volumeTurnRequest = null }
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        if (hasBismillah) {
                            item(key = "bismillah") {
                                BismillahCard(
                                    settings = settings,
                                    vm = vm,
                                    surah = activeSurah,
                                    playing = isBismPlaying
                                )
                            }
                        }
                        items(verses, key = { it.v }) { verse ->
                            val isPlayingThis = (playingVerse != null && playingVerse == verse.v)
                            val isBookmarkedThis = bookmarks.contains("${verse.s}:${verse.v}")
                            VerseCard(
                                verse = verse,
                                settings = settings,
                                playing = isPlayingThis,
                                bookmarked = isBookmarkedThis,
                                onToggleBookmark = { vm.toggleBookmark(verse.s, verse.v) },
                                onPlay = {
                                    ensureNotificationPermission(context, permissionLauncher)
                                    audioController.playVerse(verse.s, verse.v)
                                },
                                onOpenVerse = { popupVerse = verse }
                            )
                        }
                    }
                }
            }
        }
    }

    popupVerse?.let { verse ->
        val progressPct by audioController.progressPct.collectAsState()
        val isPlayingThis = (isPlaying && currentVerse?.s == verse.s && currentVerse?.v == verse.v)
        val isBookmarkedThis = bookmarks.contains("${verse.s}:${verse.v}")
        VersePopupSheet(
            verse = verse,
            bookmarked = isBookmarkedThis,
            onToggleBookmark = { vm.toggleBookmark(verse.s, verse.v) },
            onDismiss = {
                popupFollow = false
                popupVerse = null
            },
            hasPrev = verse.v > 1,
            hasNext = verse.v < (meta?.ayahs ?: verse.v),
            playingThis = isPlayingThis,
            activeThis = isPlayingThis,
            progressPct = if (isPlayingThis) progressPct else 0f,
            autoAdvance = autoAdvance,
            onPlayPause = {
                popupFollow = true
                if (isPlayingThis) {
                    audioController.togglePlayPause()
                } else {
                    ensureNotificationPermission(context, permissionLauncher)
                    audioController.playVerse(verse.s, verse.v)
                }
            },
            onPrev = {
                if (verse.v > 1) {
                    vm.verse(verse.s, verse.v - 1)?.let { prev ->
                        popupVerse = prev
                        if (isPlayingThis) {
                            audioController.playVerse(prev.s, prev.v)
                        }
                    }
                }
            },
            onNext = {
                if (verse.v < (meta?.ayahs ?: verse.v)) {
                    vm.verse(verse.s, verse.v + 1)?.let { next ->
                        popupVerse = next
                        if (isPlayingThis) {
                            audioController.playVerse(next.s, next.v)
                        }
                    }
                }
            },
            onToggleAuto = toggleAutoAdvance,
            compact = false,
            playbackOnly = false,
            totalVerses = meta?.ayahs ?: verse.v
        )
    }
}
