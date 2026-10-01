package org.justquran.app.ui.reader

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.justquran.app.data.Verse
import org.justquran.app.data.displayArabic
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts

fun verseShareText(v: Verse): String {
    val sb = StringBuilder()
    sb.append(v.displayArabic())
    sb.append("\n\n")
    sb.append(v.en)
    if (v.ur != null) {
        sb.append("\n\n")
        sb.append(v.ur)
    }
    sb.append("\n\n— Surah ${v.s}, verse ${v.v} · JustQuran")
    return sb.toString()
}

fun shareVerse(context: Context, verse: Verse) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, verseShareText(verse))
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Composable
fun AutoChip(
    auto: Boolean = false,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = auto
) {
    val isChecked = if (auto) auto else checked
    val colors = appColors
    val fonts = rememberAppFonts()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onToggle)
            .then(if (isChecked) Modifier.background(colors.accent) else Modifier)
            .border(1.dp, if (isChecked) colors.accent else colors.hairline, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AUTO",
            color = if (isChecked) colors.bg else colors.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fonts.serif,
            letterSpacing = 0.8.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersePopupSheet(
    verse: Verse,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit,
    hasPrev: Boolean,
    hasNext: Boolean,
    playingThis: Boolean,
    activeThis: Boolean,
    progressPct: Float,
    autoAdvance: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleAuto: () -> Unit,
    compact: Boolean = false,
    playbackOnly: Boolean = false,
    totalVerses: Int = verse.v
) {
    val fonts = rememberAppFonts()
    val colors = appColors
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.card
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(48.dp),
                    enabled = hasPrev
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous verse",
                        tint = if (hasPrev) colors.accent else colors.muted.copy(alpha = 0.35f)
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (playingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playingThis) "Pause" else "Play",
                        tint = colors.accent
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp),
                    enabled = hasNext
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next verse",
                        tint = if (hasNext) colors.accent else colors.muted.copy(alpha = 0.35f)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                        trackColor = colors.hairline
                    )
                }
                AutoChip(
                    auto = autoAdvance,
                    onToggle = onToggleAuto
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop and close",
                        tint = colors.muted
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title = "Surah ${verse.s} : ${verse.v}".uppercase(Locale.ROOT)
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        color = colors.accent,
                        fontSize = 12.sp,
                        fontFamily = fonts.serif,
                        letterSpacing = 1.7.sp
                    )
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (bookmarked) "Remove bookmark" else "Bookmark",
                            tint = if (bookmarked) colors.accent else colors.muted
                        )
                    }
                    IconButton(
                        onClick = { shareVerse(context, verse) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = colors.muted
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.muted
                        )
                    }
                }

                if (!playbackOnly) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = verse.displayArabic(),
                            color = colors.arabic,
                            fontSize = 24.sp,
                            fontFamily = fonts.arabic,
                            lineHeight = 44.sp
                        )
                    }
                    if (verse.ur != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = verse.ur,
                                color = colors.urdu,
                                fontSize = 16.sp,
                                fontFamily = fonts.urdu,
                                lineHeight = 32.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = verse.en,
                        color = colors.english,
                        fontSize = 15.sp,
                        fontFamily = fonts.serif,
                        lineHeight = 22.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.hairline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrev,
                            modifier = Modifier.size(48.dp),
                            enabled = hasPrev
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous verse",
                                tint = if (hasPrev) colors.accent else colors.muted.copy(alpha = 0.35f)
                            )
                        }
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (playingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playingThis) "Pause" else "Play",
                                tint = colors.accent
                            )
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(48.dp),
                            enabled = hasNext
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next verse",
                                tint = if (hasNext) colors.accent else colors.muted.copy(alpha = 0.35f)
                            )
                        }
                    }
                    AutoChip(
                        auto = autoAdvance,
                        onToggle = onToggleAuto,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}
