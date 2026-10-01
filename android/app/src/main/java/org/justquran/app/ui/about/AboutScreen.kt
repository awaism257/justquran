package org.justquran.app.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.justquran.app.AppContainer
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.GoldDivider
import org.justquran.app.ui.appBuildLabel
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

@Composable
fun AboutScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(title = "About & credits", onBack = onBack)
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

                AboutCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "JustQuran",
                            color = colors.text,
                            fontSize = 24.sp,
                            fontFamily = fonts.serif
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Free · No ads · No sign-in · No tracking · Offline",
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontFamily = fonts.serif,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AboutCard {
                    CardTitle("Texts & licences")
                    CreditRow("Arabic", "Digital Khatt Indopak script — QUL / Tarteel Inc (qul.tarteel.ai)")
                    CreditRow("English", "Talal Itani, ClearQuran.com — CC BY-ND 4.0")
                    CreditRow("Urdu", "Jalandhari (public domain) — digitised by the JustQuran project from the original printed edition")
                    CreditRow("Transliteration", "Tanzil Project (tanzil.net)")
                    CreditRow("Recitation", "Sheikh Mahmoud Khalil Al-Hussary (Murattal)")
                    CreditRow("Source code", "Open source — MIT License (GitHub)")
                }

                AboutCard {
                    CardTitle("Fonts")
                    Text(
                        text = "DigitalKhatt IndoPak (Amine Anane / Tarteel Inc) · Amiri Quran · Noto Nastaliq Urdu — SIL Open Font License",
                        color = colors.text,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontFamily = fonts.serif
                    )
                }

                AboutCard {
                    CardTitle("App")
                    val context = LocalContext.current
                    Text(
                        text = appBuildLabel(context),
                        color = colors.text,
                        fontSize = 14.sp,
                        fontFamily = fonts.serif
                    )
                }

                AboutCard {
                    CardTitle("Project page")
                    val context = LocalContext.current
                    Text(
                        text = "News, source code and ways to support the project:",
                        color = colors.text,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontFamily = fonts.serif
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://justquran.app")
                                    )
                                )
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "justquran.app",
                            modifier = Modifier.weight(1f),
                            color = colors.accent,
                            fontSize = 14.sp,
                            fontFamily = fonts.serif
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.muted
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Compiled by Awais Mahmood with the help of Kimi K3 AI.",
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontFamily = fonts.serif,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Distributed free of charge.",
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontFamily = fonts.serif,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
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

@Composable
private fun CardTitle(text: String) {
    val fonts = rememberAppFonts()
    Text(
        text = text.uppercase(Locale.ROOT),
        color = appColors.accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = fonts.serif,
        letterSpacing = 1.5.sp
    )
    GoldDivider(Modifier.padding(vertical = 8.dp))
}

@Composable
private fun CreditRow(label: String, value: String) {
    val fonts = rememberAppFonts()
    val colors = appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(108.dp),
            color = colors.muted,
            fontSize = 14.sp,
            fontFamily = fonts.serif
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = colors.text,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontFamily = fonts.serif
        )
    }
}
