package org.justquran.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.justquran.app.AppContainer
import org.justquran.app.CrashReporter
import org.justquran.app.R
import org.justquran.app.data.SettingsRepository
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.AudioBottomBar
import org.justquran.app.ui.CircleIconButton
import org.justquran.app.ui.nav.Routes
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

@Composable
fun HomeScreen(
    container: AppContainer,
    nav: NavHostController
) {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val settings by appViewModel.settings.collectAsState()
    val bundleReady by appViewModel.bundleReady.collectAsState()
    val fonts = rememberAppFonts()
    val colors = appColors

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            val audioActive by container.audioController.active.collectAsState()
            val chainActive by container.audioController.chainActive.collectAsState()
            if (audioActive || chainActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 6.dp)
                ) {
                    AudioBottomBar(
                        audio = container.audioController,
                        showAuto = false
                    )
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .statusBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "JustQuran",
                            color = colors.text,
                            fontSize = 24.sp,
                            fontFamily = fonts.serif,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "Arabic · اردو · English",
                            color = colors.muted,
                            fontSize = 11.5.sp,
                            fontFamily = fonts.serif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HeaderIcon(
                        painter = painterResource(R.drawable.ic_search),
                        label = "Search",
                        onClick = { nav.navigate(Routes.SEARCH) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    HeaderIcon(
                        painter = painterResource(R.drawable.ic_book_marked),
                        label = "Bookmarks",
                        onClick = { nav.navigate(Routes.BOOKMARKS) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    HeaderIcon(
                        painter = painterResource(R.drawable.ic_settings),
                        label = "Settings",
                        onClick = { nav.navigate(Routes.SETTINGS) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(colors.divider)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .tilesBackground()
                .padding(padding)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val minH = if (maxHeight.value.isFinite() && maxHeight.value > 0f) maxHeight else 0.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minH)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically)
                ) {
                    // Crash banner
                    val context = LocalContext.current
                    var showCrashBanner by remember { mutableStateOf(CrashReporter.hasUnseenReport(context)) }
                    if (showCrashBanner) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.card),
                            border = null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "JustQuran closed unexpectedly last time — details in Settings → Diagnostics",
                                    modifier = Modifier.weight(1f),
                                    color = colors.text,
                                    fontSize = 13.sp,
                                    fontFamily = fonts.serif,
                                    lineHeight = 18.sp
                                )
                                IconButton(
                                    onClick = {
                                        CrashReporter.markSeen(context)
                                        showCrashBanner = false
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss crash notice",
                                        tint = colors.muted
                                    )
                                }
                            }
                        }
                    }

                    // Continue reading
                    val lastRead = SettingsRepository.parseLastRead(settings.lastRead)
                    if (bundleReady && lastRead != null) {
                        val surahMeta = appViewModel.surah(lastRead.first)
                        if (surahMeta != null) {
                            MenuCard(
                                title = "Continue reading",
                                subtitle = "${surahMeta.nameEn} · verse ${lastRead.second}",
                                icon = Icons.Outlined.History,
                                onClick = { nav.navigate(Routes.reader(lastRead.first, lastRead.second)) }
                            )
                        }
                    }

                    // Surahs
                    MenuCard(
                        title = "Surahs",
                        subtitle = "All 114 surahs",
                        painter = painterResource(R.drawable.ic_book_open),
                        onClick = { nav.navigate(Routes.SURAHS) }
                    )

                    // Juz
                    MenuCard(
                        title = "Juz",
                        subtitle = "30 parts of the Quran",
                        painter = painterResource(R.drawable.ic_rows3),
                        onClick = { nav.navigate(Routes.JUZ) }
                    )

                    // Bookmarks
                    MenuCard(
                        title = "Bookmarks",
                        subtitle = "Saved verses · last read",
                        painter = painterResource(R.drawable.ic_book_marked),
                        onClick = { nav.navigate(Routes.BOOKMARKS) }
                    )

                    // Completing the Quran
                    MenuCard(
                        title = "Completing the Quran",
                        subtitle = "Khatm dua · دعائے ختمِ قرآن",
                        icon = Icons.Outlined.VolunteerActivism,
                        onClick = { nav.navigate(Routes.KHATM) }
                    )

                    // Translations
                    MenuCard(
                        title = "Translations",
                        subtitle = "Read translations as flowing pages",
                        painter = painterResource(R.drawable.ic_library_books),
                        onClick = { nav.navigate(Routes.BOOK_HOME) }
                    )

                    // How to use
                    MenuCard(
                        title = "How to use",
                        subtitle = "Quick guide — reading, search, bookmarks",
                        painter = painterResource(R.drawable.ic_circle_help),
                        onClick = { nav.navigate(Routes.HELP) }
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Fully offline · no ads · no tracking",
                        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun HeaderIcon(
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    onClick: () -> Unit
) {
    CircleIconButton(
        onClick = onClick,
        size = 38.dp
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            modifier = Modifier.size(17.dp),
            tint = appColors.text
        )
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    MenuCardLayout(title, subtitle, onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = appColors.accent
        )
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    painter: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit
) {
    MenuCardLayout(title, subtitle, onClick) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = appColors.accent
        )
    }
}

@Composable
private fun MenuCardLayout(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    val fonts = rememberAppFonts()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconContent()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = appColors.text,
                    fontSize = 16.5.sp,
                    fontFamily = fonts.serif,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = appColors.muted,
                    fontSize = 11.5.sp,
                    fontFamily = fonts.serif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = appColors.muted
            )
        }
    }
}
