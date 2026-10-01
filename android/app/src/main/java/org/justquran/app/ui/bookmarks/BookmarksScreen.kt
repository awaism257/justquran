package org.justquran.app.ui.bookmarks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.justquran.app.AppContainer
import org.justquran.app.data.AppSettings
import org.justquran.app.data.SettingsRepository
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.nav.Routes
import org.justquran.app.ui.theme.Tokens
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

private data class Mark(val s: Int, val v: Int)

private fun firstWords(text: String, n: Int): String {
    val words = Regex("\\s+").split(text).filter { it.isNotEmpty() }
    return if (words.size <= n) text else words.take(n).joinToString(" ") + "…"
}

@Composable
fun BookmarksScreen(
    container: AppContainer,
    nav: NavHostController
) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val ready by viewModel.bundleReady.collectAsState()
    val rawMarks by viewModel.bookmarks.collectAsState()
    val settings by container.settingsRepository.settings.collectAsState(initial = AppSettings())
    val lastRead = SettingsRepository.parseLastRead(settings.lastRead)
    val fonts = rememberAppFonts()
    val colors = appColors

    val marks = rawMarks.mapNotNull { str ->
        val parts = str.split(":")
        val s = parts.getOrNull(0)?.toIntOrNull()
        val v = parts.getOrNull(1)?.toIntOrNull()
        if (s != null && v != null) Mark(s, v) else null
    }.reversed() // newest first, matching PWA getBookmarks().slice().reverse()

    val countLabel = if (marks.isEmpty()) null else "${marks.size} saved"
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(
                title = "Bookmarks",
                subtitle = countLabel,
                onBack = { nav.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .tilesBackground()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Last read card (if present)
                if (lastRead != null && ready) {
                    val lastSurah = viewModel.surah(lastRead.first)
                    item(key = "last_read") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    nav.navigate(Routes.reader(s = lastRead.first, v = lastRead.second))
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.card),
                            border = null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.accent
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Last read",
                                        color = colors.text,
                                        fontSize = 16.sp,
                                        fontFamily = fonts.serif,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${lastSurah?.nameEn ?: "Surah ${lastRead.first}"} ${lastRead.first}:${lastRead.second}",
                                        color = colors.muted,
                                        fontSize = 12.sp,
                                        fontFamily = fonts.serif
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.muted
                                )
                            }
                        }
                    }
                }

                if (marks.isEmpty()) {
                    item(key = "empty_state") {
                        Text(
                            text = "No bookmarks yet — long-press a verse while reading to save it here.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 16.dp),
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontFamily = fonts.serif,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(
                        items = marks,
                        key = { "${it.s}:${it.v}" }
                    ) { mark ->
                        val surah = if (ready) viewModel.surah(mark.s) else null
                        val verse = if (ready) viewModel.verse(mark.s, mark.v) else null
                        val title = "${surah?.nameEn ?: "Surah ${mark.s}"} ${mark.s}:${mark.v}"
                        val snippet = verse?.en?.let { firstWords(it, 8) }

                        BookmarkRow(
                            mark = mark,
                            title = title,
                            snippet = snippet,
                            onOpen = {
                                nav.navigate(Routes.reader(s = mark.s, v = mark.v))
                            },
                            onRemove = {
                                viewModel.removeBookmark(mark.s, mark.v)
                            }
                        )
                    }
                }

                item(key = "footer") {
                    Text(
                        text = "Stored locally · no account · no sync",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        color = colors.muted,
                        fontSize = 11.5.sp,
                        fontFamily = fonts.serif,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    mark: Mark,
    title: String,
    snippet: String?,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.Settled) {
                    false
                } else {
                    onRemove()
                    true
                }
            }
        ),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.verseBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = colors.bg
                )
            }
        },
        enableDismissFromStartToEnd = false,
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onOpen,
                        onLongClick = onRemove
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(colors.folioFrame, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mark.v.toString(),
                            color = Tokens.CreamFolioPaper,
                            fontSize = 14.sp,
                            fontFamily = fonts.serif
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = colors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = fonts.serif
                        )
                        if (snippet != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = snippet,
                                color = colors.muted,
                                fontSize = 12.5.sp,
                                fontFamily = fonts.serif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove bookmark",
                            modifier = Modifier.size(16.dp),
                            tint = colors.muted
                        )
                    }
                }
            }
        }
    )
}
