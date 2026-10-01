package org.justquran.app.ui.khatm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.justquran.app.AppContainer
import org.justquran.app.data.CLOSING_HADITH
import org.justquran.app.data.KHATM_INTRO
import org.justquran.app.data.KHATM_INTRO_ATTRIBUTION
import org.justquran.app.data.SHORT_DUA
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.GoldDivider
import org.justquran.app.ui.Rtl
import org.justquran.app.ui.fatihaBackground
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.theme.withHonorifics

@Composable
fun KhatmScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(title = "Completing the Quran", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fatihaBackground()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Completing the Quran",
                        color = colors.text,
                        fontSize = 22.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.height(6.dp))
                    Rtl {
                        Text(
                            text = "دعائے ختمِ قرآن",
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.accent,
                            fontSize = 19.sp,
                            lineHeight = 34.sp,
                            fontFamily = fonts.urdu,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextCard {
                    Text(
                        text = withHonorifics(KHATM_INTRO.en, fonts.naskh),
                        color = colors.text,
                        fontSize = 15.sp,
                        lineHeight = 26.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.height(10.dp))
                    Rtl {
                        Text(
                            text = withHonorifics(KHATM_INTRO.ur, fonts.naskh),
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.urdu,
                            fontSize = 13.sp,
                            lineHeight = 30.sp,
                            fontFamily = fonts.urdu
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = withHonorifics(KHATM_INTRO_ATTRIBUTION.en, fonts.naskh),
                        color = colors.text,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 24.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.height(6.dp))
                    Rtl {
                        Text(
                            text = withHonorifics(KHATM_INTRO_ATTRIBUTION.ur, fonts.naskh),
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.text,
                            fontSize = 13.sp,
                            lineHeight = 30.sp,
                            fontFamily = fonts.urdu
                        )
                    }
                }

                TextCard {
                    SHORT_DUA.forEachIndexed { index, duaSegment ->
                        if (index > 0) {
                            GoldDivider(Modifier.padding(vertical = 16.dp))
                        }
                        Rtl {
                            Text(
                                text = withHonorifics(duaSegment.ar, fonts.naskh),
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.text,
                                fontSize = 24.sp,
                                lineHeight = 44.sp,
                                fontFamily = fonts.arabic,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Rtl {
                            Text(
                                text = withHonorifics(duaSegment.ur, fonts.naskh),
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.urdu,
                                fontSize = 13.sp,
                                lineHeight = 30.sp,
                                fontFamily = fonts.urdu
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = withHonorifics(duaSegment.en, fonts.naskh),
                            color = colors.english,
                            fontSize = 15.sp,
                            lineHeight = 26.sp,
                            fontFamily = fonts.serif
                        )
                    }
                }

                TextCard {
                    Text(
                        text = withHonorifics(CLOSING_HADITH.en, fonts.naskh),
                        color = colors.text,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 27.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.height(10.dp))
                    Rtl {
                        Text(
                            text = withHonorifics(CLOSING_HADITH.ur, fonts.naskh),
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.urdu,
                            fontSize = 13.sp,
                            lineHeight = 30.sp,
                            fontFamily = fonts.urdu
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = withHonorifics(CLOSING_HADITH.noteEn, fonts.naskh),
                        color = colors.text,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 24.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.height(6.dp))
                    Rtl {
                        Text(
                            text = withHonorifics(CLOSING_HADITH.noteUr, fonts.naskh),
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.text,
                            fontSize = 13.sp,
                            lineHeight = 30.sp,
                            fontFamily = fonts.urdu
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun TextCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        border = null
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}
