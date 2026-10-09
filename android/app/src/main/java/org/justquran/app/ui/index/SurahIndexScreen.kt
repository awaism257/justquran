package org.justquran.app.ui.index

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.justquran.app.ui.NumberBadge
import androidx.navigation.NavHostController
import org.justquran.app.AppContainer
import org.justquran.app.data.SurahMeta
import org.justquran.app.data.SurahTitles
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.nav.Routes
import org.justquran.app.ui.theme.Tokens
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

@Composable
fun SurahIndexScreen(
    container: AppContainer,
    nav: NavHostController
) {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val bundleReady by appViewModel.bundleReady.collectAsState()
    val fonts = rememberAppFonts()
    val colors = appColors
    var query by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(
                title = "Surahs",
                subtitle = "114 surahs",
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
            IndexFilterField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Filter by name, meaning or number…"
            )

            if (!bundleReady) {
                return@Column
            }

            val allSurahs = remember(bundleReady) { appViewModel.surahMeta() }
            val filtered = remember(allSurahs, query) {
                if (query.isBlank()) {
                    allSurahs
                } else {
                    val q = foldQuery(query.trim())
                    allSurahs.filter { s ->
                        s.n.toString().startsWith(q) ||
                                foldQuery(s.nameEn).contains(q) ||
                                foldQuery(s.nameMeaning).contains(q) ||
                                foldQuery(SurahTitles.englishName(s.n)).contains(q) ||
                                s.nameAr.contains(q)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.n }) { surah ->
                    Card(
                        onClick = { nav.navigate(Routes.reader(surah.n)) },
                        modifier = Modifier.fillMaxWidth(),
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
                            NumberBadge(
                                number = surah.n,
                                backgroundColor = colors.folioFrame,
                                textColor = Tokens.CreamFolioPaper,
                                fontFamily = fonts.serif
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = surah.nameEn,
                                    color = colors.text,
                                    fontSize = 16.sp,
                                    fontFamily = fonts.serif,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${surah.nameMeaning} · ${surah.ayahs} verses",
                                    color = colors.muted,
                                    fontSize = 12.sp,
                                    fontFamily = fonts.serif
                                )
                            }
                            Text(
                                text = surah.nameAr,
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
        }
    }
}
