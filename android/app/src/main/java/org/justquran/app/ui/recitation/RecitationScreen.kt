package org.justquran.app.ui.recitation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.justquran.app.AppContainer
import org.justquran.app.data.AudioDownloadRepository
import org.justquran.app.data.SurahMeta
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.NumberBadge
import org.justquran.app.ui.theme.AppColors
import org.justquran.app.ui.theme.AppFonts
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

private sealed interface ConfirmAction {
    data class All(val lang: String) : ConfirmAction
    data class Delete(val surah: Int, val name: String, val lang: String) : ConfirmAction
    data class DeleteAll(val lang: String) : ConfirmAction
}

private data class AudioTrackInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultEstimate: String
)

private val TRACKS = listOf(
    AudioTrackInfo("ar", "Arabic Recitation", "Sheikh Mahmoud Khalil Al-Hussary · Murattal", "~1 GB"),
    AudioTrackInfo("en", "English Translation", "Brian · Verse-by-verse English narration", "~500 MB"),
    AudioTrackInfo("ur", "Urdu Translation", "Fateh Muhammad Jalandhari · Verse-by-verse Urdu narration", "~500 MB")
)

@Composable
fun RecitationScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val ready by appViewModel.bundleReady.collectAsState()
    val repo = container.audioDownloadRepository

    val scanAr by repo.scanAr.collectAsState()
    val scanEn by repo.scanEn.collectAsState()
    val scanUr by repo.scanUr.collectAsState()
    val downloadingRaw by repo.downloading.collectAsState()

    var expandedLang by remember { mutableStateOf<String?>("ar") }
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    val downloading = if (downloadingRaw?.error != null) null else downloadingRaw
    var allDoneNote by remember { mutableStateOf<String?>(null) }
    var wasAllLang by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(downloadingRaw) {
        val dl = downloadingRaw
        if (dl != null && dl.all && dl.error == null) {
            wasAllLang = dl.lang
        } else if (dl == null && wasAllLang != null) {
            allDoneNote = wasAllLang
            wasAllLang = null
        } else if (dl?.error != null) {
            wasAllLang = null
        }
    }

    LaunchedEffect(Unit) {
        repo.refresh()
    }

    val surahs = appViewModel.surahMeta()
    val totalAyahs = surahs.sumOf { it.ayahs }

    fun scanFor(lang: String): Map<Int, Pair<Int, Long>> = when (lang) {
        "en" -> scanEn
        "ur" -> scanUr
        else -> scanAr
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(
                title = "Audio Downloads",
                subtitle = "Recitation & Translation Audio",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .tilesBackground()
                .padding(padding)
        ) {
            if (!ready) {
                Text(
                    text = "Loading…",
                    modifier = Modifier.padding(16.dp),
                    color = colors.muted,
                    fontFamily = fonts.serif
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "intro") {
                        Text(
                            text = "Download audio for offline listening. Choose a section below to manage downloads for each language.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            color = colors.muted,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            fontFamily = fonts.serif
                        )
                    }

                    TRACKS.forEach { track ->
                        val trackLang = track.id
                        val trackScan = scanFor(trackLang)
                        val completeSurahs = surahs.count { (trackScan[it.n]?.first ?: 0) >= it.ayahs }
                        val isExpanded = (expandedLang == trackLang)
                        val isDownloadingThisLang = (downloading?.lang == trackLang)

                        val overallPct: Int? = if (isDownloadingThisLang && downloading?.all == true && totalAyahs > 0) {
                            val priorAyahs = surahs.filter { it.n < downloading!!.surah }.sumOf { it.ayahs }
                            (((priorAyahs + downloading!!.done) * 100) / totalAyahs).coerceIn(0, 100)
                        } else {
                            null
                        }

                        item(key = "header_${track.id}") {
                            SectionHeaderCard(
                                title = track.title,
                                subtitle = track.subtitle,
                                downloadedCount = completeSurahs,
                                totalCount = 114,
                                expanded = isExpanded,
                                isDownloading = isDownloadingThisLang,
                                onClick = {
                                    expandedLang = if (isExpanded) null else trackLang
                                }
                            )
                        }

                        if (isExpanded) {
                            item(key = "hero_${track.id}") {
                                val activeJob = if (isDownloadingThisLang) downloading else null
                                HeroCard(
                                    langName = track.title,
                                    estimate = track.defaultEstimate,
                                    active = activeJob,
                                    overallPct = overallPct,
                                    onClick = {
                                        if (activeJob != null) {
                                            repo.cancelDownload()
                                        } else {
                                            allDoneNote = null
                                            confirm = ConfirmAction.All(trackLang)
                                        }
                                    }
                                )
                            }

                            val errorNote = if (downloadingRaw?.lang == trackLang) downloadingRaw?.error else null
                            val successNote = if (allDoneNote == trackLang) "All ${track.title} audio downloaded for offline use." else null
                            val note = errorNote ?: successNote
                            if (note != null) {
                                item(key = "note_${track.id}") {
                                    Text(
                                        text = note,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        color = if (errorNote != null) colors.accent else colors.muted,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        fontFamily = fonts.serif
                                    )
                                }
                            }

                            item(key = "counter_${track.id}") {
                                val hasAudio = trackScan.values.any { it.first > 0 }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$completeSurahs of 114 surahs downloaded",
                                        modifier = Modifier.weight(1f),
                                        color = colors.muted,
                                        fontSize = 11.5.sp,
                                        fontFamily = fonts.serif
                                    )
                                    if (hasAudio && !isDownloadingThisLang) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .clickable {
                                                    allDoneNote = null
                                                    confirm = ConfirmAction.DeleteAll(trackLang)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = colors.accent
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Delete all",
                                                color = colors.accent,
                                                fontSize = 12.sp,
                                                fontFamily = fonts.serif
                                            )
                                        }
                                    }
                                }
                            }

                            items(
                                count = surahs.size,
                                key = { "${track.id}_${surahs[it].n}" }
                            ) { index ->
                                val meta = surahs[index]
                                val pair = trackScan[meta.n]
                                val count = pair?.first ?: 0
                                val complete = count >= meta.ayahs
                                val rowJob = if (isDownloadingThisLang && downloading?.surah == meta.n) downloading else null

                                SurahCard(
                                    meta = meta,
                                    count = count,
                                    complete = complete,
                                    rowJob = rowJob,
                                    onClick = {
                                        if (rowJob != null) {
                                            repo.cancelDownload()
                                        } else if (!complete) {
                                            allDoneNote = null
                                            repo.downloadSurah(meta.n, meta.ayahs, trackLang)
                                        } else {
                                            confirm = ConfirmAction.Delete(meta.n, meta.nameEn, trackLang)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item(key = "caption") {
                        Text(
                            text = "Streaming works online · Download for offline listening",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 12.dp),
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontFamily = fonts.serif,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    val action = confirm
    if (action != null) {
        val (dialogTitle, dialogText, confirmBtn) = when (action) {
            is ConfirmAction.All -> {
                val track = TRACKS.firstOrNull { it.id == action.lang }
                Triple(
                    "Download all ${track?.title ?: "audio"}?",
                    "This downloads all 114 surahs (${track?.defaultEstimate ?: "~1 GB"}). Wi-Fi is recommended.",
                    "Download"
                )
            }
            is ConfirmAction.Delete -> {
                val track = TRACKS.firstOrNull { it.id == action.lang }
                Triple(
                    "Delete download?",
                    "Remove the downloaded ${track?.title ?: ""} audio for ${action.name}? You can re-download it any time.",
                    "Delete"
                )
            }
            is ConfirmAction.DeleteAll -> {
                val track = TRACKS.firstOrNull { it.id == action.lang }
                Triple(
                    "Delete all downloads?",
                    "This removes every downloaded surah for ${track?.title ?: ""} from this device. You can re-download them any time.",
                    "Delete all"
                )
            }
        }

        AlertDialog(
            onDismissRequest = { confirm = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = null
                        when (action) {
                            is ConfirmAction.All -> repo.downloadAll(surahs, action.lang)
                            is ConfirmAction.Delete -> repo.deleteSurah(action.surah, action.lang)
                            is ConfirmAction.DeleteAll -> repo.deleteAll(action.lang)
                        }
                    }
                ) {
                    Text(
                        text = confirmBtn,
                        color = colors.accent,
                        fontFamily = fonts.serif
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text(
                        text = "Cancel",
                        color = colors.muted,
                        fontFamily = fonts.serif
                    )
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontFamily = fonts.serif
                )
            },
            text = {
                Text(
                    text = dialogText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontFamily = fonts.serif
                )
            },
            containerColor = colors.card,
            titleContentColor = colors.text,
            textContentColor = colors.muted
        )
    }
}

@Composable
private fun SectionHeaderCard(
    title: String,
    subtitle: String,
    downloadedCount: Int,
    totalCount: Int,
    expanded: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) colors.card else colors.card.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (expanded) colors.accent.copy(alpha = 0.5f) else colors.hairline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = colors.text,
                        fontSize = 16.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (downloadedCount == totalCount) colors.accent.copy(alpha = 0.2f)
                                else colors.hairline
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDownloading) "Downloading…" else "$downloadedCount/$totalCount",
                            color = if (downloadedCount == totalCount) colors.accent else colors.muted,
                            fontSize = 11.sp,
                            fontFamily = fonts.serif
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontFamily = fonts.serif,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(22.dp),
                tint = if (expanded) colors.accent else colors.muted
            )
        }
    }
}

@Composable
private fun HeroCard(
    langName: String,
    estimate: String,
    active: AudioDownloadRepository.Downloading?,
    overallPct: Int?,
    onClick: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(colors.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.bg
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (active != null) "Cancel download" else "Download all $langName",
                    color = colors.text,
                    fontSize = 14.5.sp,
                    fontFamily = fonts.serif
                )
                Spacer(Modifier.height(2.dp))
                val subtitle = if (overallPct != null && active != null) {
                    "Surah ${active.surah} of 114 · $overallPct%"
                } else if (active != null) {
                    "Downloading surah ${active.surah}… ${active.progressPct.toInt()}%"
                } else {
                    "Whole Quran · $estimate · Wi-Fi recommended"
                }
                Text(
                    text = subtitle,
                    color = colors.muted,
                    fontSize = 11.5.sp,
                    fontFamily = fonts.serif
                )
                if (active != null) {
                    Spacer(Modifier.height(8.dp))
                    val pct = overallPct ?: active.progressPct.toInt()
                    ThinBar(
                        fraction = (pct / 100f).coerceIn(0f, 1f),
                        height = 4.dp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = if (active != null) Icons.Filled.Close else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colors.muted
            )
        }
    }
}

@Composable
private fun SurahCard(
    meta: SurahMeta,
    count: Int,
    complete: Boolean,
    rowJob: AudioDownloadRepository.Downloading?,
    onClick: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberBadge(
                number = meta.n,
                backgroundColor = colors.accent,
                textColor = colors.bg,
                fontFamily = fonts.serif
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.nameEn,
                    color = colors.text,
                    fontSize = 14.5.sp,
                    fontFamily = fonts.serif
                )
                Spacer(Modifier.height(2.dp))
                val subtitle = if (rowJob != null) {
                    "Downloading… ${rowJob.progressPct.toInt()}%"
                } else if (complete) {
                    "Downloaded ✓"
                } else if (count > 0) {
                    "$count of ${meta.ayahs} verses · Tap to resume"
                } else {
                    "Not downloaded · ${meta.ayahs} verses"
                }
                Text(
                    text = subtitle,
                    color = colors.muted,
                    fontSize = 11.5.sp,
                    fontFamily = fonts.serif
                )
                if (rowJob != null) {
                    Spacer(Modifier.height(8.dp))
                    ThinBar(
                        fraction = (rowJob.progressPct / 100f).coerceIn(0f, 1f),
                        height = 3.dp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            if (complete) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Downloaded",
                    modifier = Modifier.size(18.dp),
                    tint = colors.accent
                )
            } else if (rowJob != null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.muted
                )
            }
        }
    }
}

@Composable
private fun ThinBar(
    fraction: Float,
    height: Dp
) {
    val colors = appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.hairline),
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.accent)
        )
    }
}
