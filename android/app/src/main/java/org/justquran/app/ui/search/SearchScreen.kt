package org.justquran.app.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.justquran.app.AppContainer
import org.justquran.app.data.SurahMeta
import org.justquran.app.data.SurahTitles
import org.justquran.app.data.Verse
import org.justquran.app.data.displayArabic
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.nav.Routes
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

private const val MAX_RESULTS = 100

private fun stripArabicDiacritics(t: String): String {
    val stripped = Regex("[ً-ٰٟۖ-ۭـ͏ؔ-ؚ\u089c\u0895-\u08e2]").replace(t, "")
    return Regex("[أإآٱ]").replace(stripped, "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")
}

private fun normArScript(s: String): String {
    return stripArabicDiacritics(s)
        .replace("ہ", "ه")
        .replace("ھ", "ه")
        .replace("ی", "ي")
        .replace("ک", "ك")
}

private fun normLatin(s: String): String {
    val n = Normalizer.normalize(s, Normalizer.Form.NFD)
    val stripped = Regex("[ʿʾ'`]").replace(Regex("[̀-ͯ]").replace(n, ""), "").lowercase(Locale.ROOT)
    return Regex("[^a-z]").replace(stripped, "")
}

private fun normDigits(s: String): String {
    val sb = StringBuilder()
    for (ch in s) {
        if (ch in '\u0660'..'\u0669') {
            sb.append(ch - '\u0660')
        } else if (ch in '\u06F0'..'\u06F9') {
            sb.append(ch - '\u06F0')
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

private class SearchDoc(
    val verse: Verse,
    val normAr: String,
    val enLower: String,
    val trLower: String
)

private data class VerseRef(
    val s: Int,
    val v: Int,
    val name: String
)

@Composable
fun SearchScreen(
    container: AppContainer,
    nav: NavHostController
) {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val ready by vm.bundleReady.collectAsState()
    val settings by vm.settings.collectAsState()
    val fonts = rememberAppFonts()
    val colors = appColors

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var raw by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }

    LaunchedEffect(raw) {
        delay(250L)
        q = raw.trim()
    }

    val docs by produceState<List<SearchDoc>?>(initialValue = null, ready) {
        if (ready) {
            value = withContext(Dispatchers.Default) {
                vm.allVerses().map { verse ->
                    SearchDoc(
                        verse = verse,
                        normAr = normArScript(verse.ar),
                        enLower = verse.en.lowercase(Locale.ROOT),
                        trLower = verse.tr.lowercase(Locale.ROOT)
                    )
                }
            }
        }
    }

    val directRef: VerseRef? = remember(q, ready) {
        if (ready) {
            val match = Regex("^(\\d{1,3})(?:\\s*[:./,\\-]\\s*|\\s+)(\\d{1,3})$").find(normDigits(q))
            if (match != null) {
                val s = match.groupValues[1].toIntOrNull()
                val v = match.groupValues[2].toIntOrNull()
                if (s != null && v != null && s in 1..114) {
                    val surah = vm.surah(s)
                    if (surah != null && v in 1..surah.ayahs) {
                        VerseRef(s, v, surah.nameEn)
                    } else null
                } else null
            } else null
        } else null
    }

    val matchedSurahs: List<SurahMeta> = remember(q, ready, directRef) {
        if (!ready || q.length < 2 || directRef != null) {
            emptyList()
        } else {
            val qLatin = normLatin(q)
            val qAr = normArScript(q)
            val all = vm.surahMeta()
            val matches = all.filter { s ->
                (qLatin.isNotEmpty() && (normLatin(s.nameEn).contains(qLatin) || normLatin(s.nameMeaning).contains(qLatin) || normLatin(SurahTitles.englishName(s.n)).contains(qLatin))) ||
                    (qAr.isNotEmpty() && normArScript(s.nameAr).contains(qAr))
            }
            matches.sortedWith(
                compareBy(
                    { if (qLatin.isNotEmpty() && normLatin(it.nameEn).startsWith(qLatin)) 0 else 1 },
                    { it.n }
                )
            ).take(3)
        }
    }

    val matchedVerses: List<Verse> = remember(q, docs, directRef, settings.showTr) {
        val list = docs
        if (list == null || q.length < 2 || directRef != null) {
            emptyList()
        } else {
            val qLower = q.lowercase(Locale.ROOT)
            val qAr = normArScript(q)
            val results = ArrayList<Verse>(MAX_RESULTS)
            for (doc in list) {
                val matchEn = doc.enLower.contains(qLower)
                val matchAr = qAr.isNotEmpty() && doc.normAr.contains(qAr)
                val matchUr = doc.verse.ur?.contains(q) == true
                val matchTr = settings.showTr && doc.trLower.contains(qLower)

                if (matchEn || matchAr || matchUr || matchTr) {
                    results.add(doc.verse)
                    if (results.size >= MAX_RESULTS) break
                }
            }
            results
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(
                title = "Search",
                subtitle = "offline · 6,236 verses",
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
            OutlinedTextField(
                value = raw,
                onValueChange = { raw = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(50),
                placeholder = {
                    Text(
                        text = "Search text, surah — or 2:255 to jump…",
                        color = colors.muted,
                        fontSize = 14.sp,
                        fontFamily = fonts.serif
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = colors.muted
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedContainerColor = colors.card,
                    unfocusedContainerColor = colors.card,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.hairline,
                    cursorColor = colors.accent
                )
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            if (q.isNotEmpty()) {
                if (q.length < 2) {
                    HintText("Type at least 2 characters.")
                } else if (docs != null && directRef == null && matchedSurahs.isEmpty() && matchedVerses.isEmpty()) {
                    HintText("No results for “$q”.")
                } else if (matchedVerses.isNotEmpty()) {
                    val count = matchedVerses.size
                    val plus = if (count >= MAX_RESULTS) "+" else ""
                    val plural = if (count != 1) "s" else ""
                    HintText("$count$plus verse result$plural")
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    if (directRef != null) {
                        item(key = "jump-verse") {
                            JumpCard(
                                label = "Go to verse",
                                title = "${directRef.name} — verse ${directRef.v}",
                                onClick = {
                                    nav.navigate(Routes.reader(s = directRef.s, v = directRef.v))
                                }
                            )
                        }
                    }

                    items(
                        items = matchedSurahs,
                        key = { "surah-${it.n}" }
                    ) { surah ->
                        JumpCard(
                            label = "Go to surah",
                            title = "${surah.n}. ${surah.nameEn} — ${surah.nameMeaning}",
                            onClick = {
                                nav.navigate(Routes.reader(s = surah.n))
                            }
                        )
                    }

                    items(
                        items = matchedVerses,
                        key = { "v-${it.s}:${it.v}" }
                    ) { verse ->
                        val surah = vm.surah(verse.s)
                        val surahName = surah?.nameEn ?: "Surah ${verse.s}"
                        ResultCard(
                            verse = verse,
                            surahName = surahName,
                            onClick = {
                                nav.navigate(Routes.reader(s = verse.s, v = verse.v))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    val fonts = rememberAppFonts()
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        color = appColors.muted,
        fontSize = 12.sp,
        fontFamily = fonts.serif
    )
}

@Composable
private fun JumpCard(
    label: String,
    title: String,
    onClick: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = null
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = label.uppercase(Locale.ROOT),
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fonts.serif,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = colors.text,
                fontSize = 15.sp,
                fontFamily = fonts.serif
            )
        }
    }
}

@Composable
private fun ResultCard(
    verse: Verse,
    surahName: String,
    onClick: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = null
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "$surahName ${verse.s}:${verse.v}",
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fonts.serif,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = verse.displayArabic(),
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.text,
                    fontSize = 19.sp,
                    lineHeight = 32.sp,
                    fontFamily = fonts.arabic
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = verse.en,
                color = colors.english,
                fontSize = 14.sp,
                fontFamily = fonts.serif,
                maxLines = 3
            )
        }
    }
}
