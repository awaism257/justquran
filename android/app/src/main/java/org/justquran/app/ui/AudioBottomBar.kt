package org.justquran.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.justquran.app.audio.AudioController
import org.justquran.app.data.SurahTitles
import org.justquran.app.ui.reader.AutoChip
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts

@Composable
fun AudioBottomBar(
    audio: AudioController,
    modifier: Modifier = Modifier,
    currentSurah: Int? = null,
    totalAyahs: Int? = null,
    showAuto: Boolean = true
) {
    val active by audio.active.collectAsState()
    val chainActive by audio.chainActive.collectAsState()
    if (!active && !chainActive) return

    val isPlaying by audio.isPlaying.collectAsState()
    val progressPct by audio.progressPct.collectAsState()
    val currentVerse by audio.currentVerse.collectAsState()
    val prelude by audio.prelude.collectAsState()
    val autoAdvance by audio.autoAdvance.collectAsState()
    val translationLang by audio.translationLang.collectAsState()

    val colors = appColors
    val fonts = rememberAppFonts()
    val isUrdu = (translationLang == "ur")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.verseBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { audio.previousVerse() }) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous verse",
                    tint = colors.accent
                )
            }
            IconButton(onClick = { audio.togglePlayPause() }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = colors.accent
                )
            }
            IconButton(onClick = { audio.nextVerse() }) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next verse",
                    tint = colors.accent
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                val label = when {
                    prelude -> if (isUrdu) "بسم اللہ" else "Bismillah"
                    currentVerse != null && currentVerse!!.v >= 1 -> {
                        val cv = currentVerse!!
                        val isSameSurah = (currentSurah != null && cv.s == currentSurah)
                        val total = if (isSameSurah) (totalAyahs ?: 0) else 0
                        when {
                            isUrdu -> {
                                val sName = SurahTitles.urduName(cv.s)
                                if (isSameSurah && total > 0) "آیت ${cv.v} از $total"
                                else "$sName · آیت ${cv.v}"
                            }
                            translationLang == "en" -> {
                                if (isSameSurah && total > 0) "Verse ${cv.v} of $total · Translation"
                                else "Surah ${cv.s}:${cv.v} · Translation"
                            }
                            else -> {
                                if (isSameSurah && total > 0) "verse ${cv.v} of $total"
                                else "Surah ${cv.s}:${cv.v}"
                            }
                        }
                    }
                    else -> {
                        if (currentSurah != null && totalAyahs != null && totalAyahs > 0) {
                            "$totalAyahs verses"
                        } else {
                            "…"
                        }
                    }
                }
                Text(
                    text = label,
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontFamily = if (isUrdu) fonts.urdu else fonts.serif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.hairline
                )
            }
            if (showAuto) {
                AutoChip(
                    checked = autoAdvance || chainActive,
                    onToggle = {
                        if (autoAdvance || chainActive) {
                            audio.setAutoAdvance(false)
                            audio.cancelChain()
                        } else {
                            audio.setAutoAdvance(true)
                        }
                    }
                )
            }
            IconButton(
                onClick = { audio.stopAll() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Stop playback",
                    tint = colors.muted
                )
            }
        }
    }
}
