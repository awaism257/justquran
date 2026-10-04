package org.justquran.app.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts

enum class PagedMode {
    ARABIC,
    ENGLISH,
    URDU
}

@Composable
fun PagedModePillsRow(
    currentMode: PagedMode,
    onSelectArabic: () -> Unit,
    onSelectEnglish: () -> Unit,
    onSelectUrdu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PagedModePill(
            label = "Arabic",
            selected = (currentMode == PagedMode.ARABIC),
            onClick = onSelectArabic
        )
        PagedModePill(
            label = "English",
            selected = (currentMode == PagedMode.ENGLISH),
            onClick = onSelectEnglish
        )
        PagedModePill(
            label = "اردو",
            selected = (currentMode == PagedMode.URDU),
            isUrdu = true,
            onClick = onSelectUrdu
        )
    }
}

@Composable
private fun PagedModePill(
    label: String,
    selected: Boolean,
    isUrdu: Boolean = false,
    onClick: () -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 72.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else Color.Transparent)
            .border(1.dp, if (selected) colors.accent else colors.hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF101913) else colors.muted,
            fontSize = 12.5.sp,
            fontFamily = if (isUrdu) fonts.urdu else fonts.serif,
            textAlign = TextAlign.Center
        )
    }
}
