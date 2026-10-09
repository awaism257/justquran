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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.justquran.app.ui.NumberBadge
import org.justquran.app.AppContainer
import org.justquran.app.data.JUZ_NAMES
import org.justquran.app.data.JUZ_NAMES_AR
import org.justquran.app.data.JUZ_STARTS
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.nav.Routes
import org.justquran.app.ui.theme.Tokens
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

@Composable
fun JuzIndexScreen(
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
                title = "Juz",
                subtitle = "30 ajzāʾ",
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
                placeholder = "Filter by name or number…"
            )

            if (!bundleReady) {
                return@Column
            }

            val filteredJuz = remember(query) {
                if (query.isBlank()) {
                    JUZ_STARTS
                } else {
                    val q = foldQuery(query.trim())
                    JUZ_STARTS.filter { j ->
                        val juzNum = j.juz.toString()
                        val enName = foldQuery(JUZ_NAMES.getOrElse(j.juz - 1) { "" })
                        val arName = JUZ_NAMES_AR.getOrElse(j.juz - 1) { "" }
                        juzNum.startsWith(q) || enName.contains(q) || arName.contains(query.trim())
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredJuz, key = { it.juz }) { juz ->
                    val nextJuz = JUZ_STARTS.firstOrNull { it.juz == juz.juz + 1 }
                    val (endS, endV) = if (nextJuz == null) {
                        114 to 6
                    } else if (nextJuz.v == 1) {
                        val prevS = nextJuz.s - 1
                        prevS to (appViewModel.surah(prevS)?.ayahs ?: 0)
                    } else {
                        nextJuz.s to (nextJuz.v - 1)
                    }

                    Card(
                        onClick = { nav.navigate(Routes.reader(juz.s, juz.v)) },
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
                                number = juz.juz,
                                backgroundColor = colors.folioFrame,
                                textColor = Tokens.CreamFolioPaper,
                                fontFamily = fonts.serif
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Juz ${juz.juz}",
                                        color = colors.text,
                                        fontSize = 16.sp,
                                        fontFamily = fonts.serif,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = JUZ_NAMES_AR[juz.juz - 1],
                                        color = colors.text,
                                        fontSize = 20.sp,
                                        fontFamily = fonts.arabic
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${JUZ_NAMES[juz.juz - 1]} · ${juz.s}:${juz.v} → $endS:$endV",
                                    color = colors.muted,
                                    fontSize = 12.sp,
                                    fontFamily = fonts.serif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
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
