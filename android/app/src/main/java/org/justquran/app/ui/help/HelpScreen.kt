package org.justquran.app.ui.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.justquran.app.AppContainer
import org.justquran.app.data.PAUSES_NOTE
import org.justquran.app.data.PauseSign
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.GoldDivider
import org.justquran.app.ui.Rtl
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

@Composable
fun HelpScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(title = "How to use", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .tilesBackground()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                CollapsibleCard(title = "Two ways to read", defaultOpen = true) {
                    Step(1, "Verse-by-verse (the default): every verse in its own card with translation and transliteration.")
                    Step(2, "Folio (mushaf style): tap the book icon in the top bar of any surah for continuous Arabic with tappable verse-number markers. Settings → Mushaf page style can page the mushaf — pages are laid out at your chosen font size, and the number of pages adapts so verses are never cut off.")
                    Step(3, "Tap the icon again to switch back — the app remembers your choice.")
                }

                CollapsibleCard(title = "Card View") {
                    Step(1, "In card view, chips at the top of any surah: Arabic · English · اردو · Translit. (Mushaf view has no chips — the top bar has ▶ play and the layout icon.)")
                    Step(2, "Tap a chip to show or hide that line while reading. The Arabic chip shows or hides the Arabic line on the verse cards.")
                    Step(3, "The Urdu translation is the classic Jalandhari translation (public domain). Turn all three translations off for Arabic-only reading.")
                    Step(4, "One line always stays visible — the chip of the last visible line is disabled, so a card never goes blank. (Mushaf view is always Arabic.)")
                }

                CollapsibleCard(title = "Text size & theme") {
                    Step(1, "Settings has sliders for Arabic, English, Urdu and transliteration text size.")
                    Step(2, "Theme can be light, dark, or follow your phone.")
                    Step(3, "Everything works fully offline; there are no ads, accounts or tracking.")
                }

                CollapsibleCard(title = "Audio") {
                    Step(1, "Tap ▶ Play surah — the chip at the top of any surah in card view, or the ▶ icon in the top bar in mushaf view — to listen from the beginning, starting with Bismillah (except surahs 1 and 9). The verse being recited is highlighted and kept on screen as it plays.")
                    Step(2, "Tap any verse to open its card, then press play to hear just that verse. Turn AUTO on to continue to the next verse automatically — and into the next surah when one ends.")
                    Step(3, "Recitation (Sheikh Mahmoud Khalil Al-Hussary) keeps playing with the screen off. Control it from the lock screen, the notification, or a Bluetooth headset.")
                    Step(4, "Download surahs for offline listening from the Recitation screen on the home page — downloaded surahs play without internet.")
                    Step(5, "Translation narration: Turn on Settings → Recitation → 'Follow each verse with translation narration' to hear each verse read aloud in English (Brian) and/or Urdu (Jalandhari) after the Arabic recitation (in card view; the paged mushaf stays Arabic-only).")
                }

                CollapsibleCard(title = "Volume buttons") {
                    Step(1, "Turn it on in Settings → Use volume buttons to navigate.")
                    Step(2, "While reading with audio paused: Volume Down = next verse, Volume Up = previous verse. In mushaf view they turn a whole screen (or a whole page in the paged mushaf style).")
                    Step(3, "While audio plays, the buttons still control the volume — the verses follow the recitation on their own.")
                }

                CollapsibleCard(title = "Search") {
                    Step(1, "Tap the magnifier at the top of the home page.")
                    Step(2, "Type in English, Arabic or Urdu letters — all three work.")
                    Step(3, "Find a surah by name (e.g. “Tawbah” or التوبہ).")
                    Step(4, "Jump straight to a verse by typing its number, e.g. 2:255.")
                }

                CollapsibleCard(title = "Bookmarks") {
                    Step(1, "Tap the bookmark button on any verse details card.")
                    Step(2, "Open Bookmarks from the home page to see saved verses; tap one to go back to it.")
                    Step(3, "Continue reading on the home page returns you to where you stopped.")
                }

                CollapsibleCard(title = "Verse details card") {
                    Step(1, "While reading, tap a verse card — or, in folio view, tap a verse or its small gold number badge.")
                    Step(2, "A card opens showing the Arabic, Urdu and English for that verse.")
                    Step(3, "Buttons: bookmark · copy or share · close.")
                }

                CollapsibleCard(title = "Translations reader") {
                    Step(1, "Home → Translations, then pick English or اردو.")
                    Step(2, "Translations flow like a book — swipe, tap the page edges, or use the arrows to turn a page.")
                    Step(3, "Tap a verse-number marker for its Arabic, audio, bookmark and share.")
                }

                CollapsibleCard(title = PAUSES_NOTE.headingEn) {
                    Text(
                        text = PAUSES_NOTE.en,
                        color = colors.text,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.height(8.dp))
                    Rtl {
                        Text(
                            text = PAUSES_NOTE.ur,
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.urdu,
                            fontSize = 13.sp,
                            lineHeight = 28.sp,
                            fontFamily = fonts.urdu
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    for (sign in PAUSES_NOTE.signs) {
                        PauseRow(sign)
                    }
                }

                CollapsibleCard(title = "Troubleshooting") {
                    Step(1, "If the app ever looks broken or gets stuck: open Settings → Diagnostics and tap Repair offline files & reload. The app clears downloaded audio and cached files, then restarts fresh — your bookmarks and settings are kept.")
                    Step(2, "If that does not fix it, uninstall the app and install it again — you will get the latest version.")
                    Step(3, "Still stuck? Note the version number shown in Settings and let us know via the project page (linked in About & credits).")
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    defaultOpen: Boolean = false,
    content: @Composable () -> Unit
) {
    var open by rememberSaveable { mutableStateOf(defaultOpen) }
    val fonts = rememberAppFonts()
    val colors = appColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = null
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = !open },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(Locale.ROOT),
                    modifier = Modifier.weight(1f),
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fonts.serif,
                    letterSpacing = 1.5.sp
                )
                Icon(
                    imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (open) "Collapse" else "Expand",
                    tint = colors.accent
                )
            }
            if (open) {
                GoldDivider(Modifier.padding(vertical = 10.dp))
                content()
            }
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(24.dp)
                .background(colors.hairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = n.toString(),
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fonts.serif
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = colors.text,
            fontSize = 14.sp,
            lineHeight = 23.sp,
            fontFamily = fonts.serif
        )
    }
}

@Composable
private fun PauseRow(sign: PauseSign) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Rtl {
            Text(
                text = sign.sign,
                modifier = Modifier.width(56.dp),
                color = colors.text,
                fontSize = 30.sp,
                fontFamily = fonts.arabic,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${sign.nameEn} — ${sign.en}",
                color = colors.text,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontFamily = fonts.serif
            )
            Rtl {
                Text(
                    text = sign.ur,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.urdu,
                    fontSize = 13.sp,
                    lineHeight = 26.sp,
                    fontFamily = fonts.urdu
                )
            }
        }
    }
    GoldDivider()
}
