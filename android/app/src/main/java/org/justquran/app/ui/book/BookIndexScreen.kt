package org.justquran.app.ui.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import org.justquran.app.ui.index.IndexFilterField
import org.justquran.app.ui.index.foldQuery
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.justquran.app.ui.AudioBottomBar
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.justquran.app.AppContainer
import org.justquran.app.R
import org.justquran.app.data.SurahTitles
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.nav.Routes
import org.justquran.app.ui.theme.Tokens
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

@Composable
fun BookIndexScreen(
    container: AppContainer,
    nav: NavHostController
) {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val loaded by appViewModel.bundleLoaded.collectAsState()
    val bookLang by container.settingsRepository.bookLang.collectAsState(initial = "en")
    val fonts = rememberAppFonts()
    val colors = appColors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(
                title = "Translations",
                subtitle = if (bookLang == "en") "English translation" else "Urdu translation",
                onBack = { nav.popBackStack() }
            )
        },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .tilesBackground()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                BookLangPill(
                    label = "English",
                    selected = (bookLang == "en"),
                    isUrdu = false,
                    onSelect = {
                        scope.launch { container.settingsRepository.setBookLang("en") }
                    }
                )
                BookLangPill(
                    label = "اردو",
                    selected = (bookLang == "ur"),
                    isUrdu = true,
                    onSelect = {
                        scope.launch { container.settingsRepository.setBookLang("ur") }
                    }
                )
            }

            IndexFilterField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Filter by name, meaning or number…"
            )

            if (loaded) {
                val allSurahs = remember(loaded) { appViewModel.surahMeta() }
                val filteredSurahs = remember(allSurahs, query, bookLang) {
                    if (query.isBlank()) {
                        allSurahs
                    } else {
                        val q = foldQuery(query.trim())
                        allSurahs.filter { s ->
                            val urduTitle = foldQuery(SurahTitles.urduName(s.n))
                            val englishTitle = foldQuery(SurahTitles.englishName(s.n))
                            s.n.toString().startsWith(q) ||
                                    foldQuery(s.nameEn).contains(q) ||
                                    foldQuery(s.nameMeaning).contains(q) ||
                                    englishTitle.contains(q) ||
                                    s.nameAr.contains(query.trim()) ||
                                    urduTitle.contains(q)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (query.isBlank()) {
                        // English edition card
                        item(key = "edition_en") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clickable {
                                        scope.launch { container.settingsRepository.setBookLang("en") }
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.card),
                                border = if (bookLang == "en") BorderStroke(1.dp, colors.accent) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_book_open),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = colors.accent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "English",
                                            color = colors.text,
                                            fontSize = 17.sp,
                                            fontFamily = fonts.serif,
                                            fontWeight = FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "ClearQuran translation",
                                            color = colors.muted,
                                            fontSize = 12.sp,
                                            fontFamily = fonts.serif
                                        )
                                    }
                                }
                            }
                        }

                        // Urdu edition card
                        item(key = "edition_ur") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clickable {
                                        scope.launch { container.settingsRepository.setBookLang("ur") }
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.card),
                                border = if (bookLang == "ur") BorderStroke(1.dp, colors.accent) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_book_open),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = colors.accent
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "اردو",
                                            color = colors.text,
                                            fontSize = 17.sp,
                                            fontFamily = fonts.urdu
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "جلندھری ترجمہ",
                                            color = colors.muted,
                                            fontSize = 12.sp,
                                            fontFamily = fonts.urdu
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Surah list
                    items(filteredSurahs, key = { it.n }) { surahMeta ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    nav.navigate(Routes.book(bookLang, surahMeta.n))
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
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(colors.folioFrame, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = surahMeta.n.toString(),
                                        color = Tokens.CreamFolioPaper,
                                        fontSize = 14.sp,
                                        fontFamily = fonts.serif
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (bookLang == "ur") {
                                        Text(
                                            text = SurahTitles.urduName(surahMeta.n),
                                            color = colors.text,
                                            fontSize = 18.sp,
                                            fontFamily = fonts.urdu
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${surahMeta.nameEn} · ${surahMeta.ayahs} verses",
                                            color = colors.muted,
                                            fontSize = 12.sp,
                                            fontFamily = fonts.serif
                                        )
                                    } else {
                                        Text(
                                            text = SurahTitles.englishName(surahMeta.n),
                                            color = colors.text,
                                            fontSize = 16.sp,
                                            fontFamily = fonts.serif
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${surahMeta.nameEn} · ${surahMeta.ayahs} verses",
                                            color = colors.muted,
                                            fontSize = 12.sp,
                                            fontFamily = fonts.serif
                                        )
                                    }
                                }
                                Text(
                                    text = surahMeta.nameAr,
                                    color = colors.text,
                                    fontSize = 20.sp,
                                    fontFamily = fonts.arabic
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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
            } else {
                Text(
                    text = "Loading…",
                    modifier = Modifier.padding(16.dp),
                    color = colors.muted,
                    fontFamily = fonts.serif
                )
            }
        }
    }
}

@Composable
private fun BookLangPill(
    label: String,
    selected: Boolean,
    isUrdu: Boolean = false,
    onSelect: () -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 78.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else Color.Transparent)
            .border(1.dp, if (selected) colors.accent else colors.hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF101913) else colors.muted,
            fontSize = 13.sp,
            fontFamily = if (isUrdu) fonts.urdu else fonts.serif,
            textAlign = TextAlign.Center
        )
    }
}
