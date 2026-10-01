package org.justquran.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.justquran.app.AppContainer
import org.justquran.app.CrashReporter
import org.justquran.app.data.FontKey
import org.justquran.app.data.ThemeChoice
import org.justquran.app.ui.AppViewModel
import org.justquran.app.ui.BackBar
import org.justquran.app.ui.GoldDivider
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.appVersionLabel
import org.justquran.app.ui.theme.rememberAppFonts
import org.justquran.app.ui.tilesBackground

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onAbout: () -> Unit,
    onRecitation: () -> Unit
) {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val settings by appViewModel.settings.collectAsState()
    val fonts = rememberAppFonts()
    val colors = appColors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BackBar(
                title = "Settings",
                onBack = onBack
            )
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
            ) {
                // Section 1: Theme
                SectionCard {
                    SectionTitle("Theme")
                    Column(modifier = Modifier.selectableGroup()) {
                        ThemeRow(
                            label = "System default",
                            choice = ThemeChoice.SYSTEM,
                            selected = settings.theme,
                            onSelect = { appViewModel.setTheme(it) }
                        )
                        ThemeRow(
                            label = "Dark",
                            choice = ThemeChoice.DARK,
                            selected = settings.theme,
                            onSelect = { appViewModel.setTheme(it) }
                        )
                        ThemeRow(
                            label = "Light",
                            choice = ThemeChoice.LIGHT,
                            selected = settings.theme,
                            onSelect = { appViewModel.setTheme(it) }
                        )
                    }
                }

                // Section 2: Font sizes
                SectionCard {
                    SectionTitle("Font sizes")
                    FontSlider(
                        label = "Arabic",
                        value = settings.fontAr,
                        onValueChange = { appViewModel.setFontScale(FontKey.AR, it) }
                    )
                    FontSlider(
                        label = "Urdu",
                        value = settings.fontUr,
                        onValueChange = { appViewModel.setFontScale(FontKey.UR, it) }
                    )
                    FontSlider(
                        label = "English",
                        value = settings.fontEn,
                        onValueChange = { appViewModel.setFontScale(FontKey.EN, it) }
                    )
                    FontSlider(
                        label = "Transliteration",
                        value = settings.fontTr,
                        onValueChange = { appViewModel.setFontScale(FontKey.TR, it) }
                    )
                }

                // Section 3: Translations
                SectionCard {
                    SectionTitle("Translations")
                    ToggleRow(
                        label = "English translation",
                        checked = settings.showEn,
                        onCheckedChange = { appViewModel.setShowEn(it) }
                    )
                    ToggleRow(
                        label = "Urdu translation (اردو)",
                        checked = settings.showUr,
                        onCheckedChange = { appViewModel.setShowUr(it) }
                    )
                    ToggleRow(
                        label = "Transliteration",
                        checked = settings.showTr,
                        onCheckedChange = { appViewModel.setShowTr(it) }
                    )
                }

                // Section 4: Reading
                SectionCard {
                    SectionTitle("Reading")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mushaf page style",
                            modifier = Modifier.weight(1f),
                            color = colors.text,
                            fontSize = 15.sp,
                            fontFamily = fonts.serif
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PageStylePill(
                                label = "Vertical",
                                selected = !settings.mushafPaged,
                                onSelect = { appViewModel.setMushafPaged(false) }
                            )
                            PageStylePill(
                                label = "Paged",
                                selected = settings.mushafPaged,
                                onSelect = { appViewModel.setMushafPaged(true) }
                            )
                        }
                    }

                    val hasTranslation = settings.showEn || settings.showUr || settings.showTr
                    val canToggleAr = !settings.showAr || hasTranslation
                    ToggleRow(
                        label = "Arabic text in card view",
                        checked = settings.showAr,
                        enabled = canToggleAr,
                        onCheckedChange = { appViewModel.setShowAr(it) }
                    )
                    ToggleRow(
                        label = "Always cream page",
                        checked = settings.alwaysCreamPage,
                        onCheckedChange = { appViewModel.setAlwaysCreamPage(it) }
                    )
                    ToggleRow(
                        label = "Use volume buttons to navigate",
                        checked = settings.volumeKeysTurnPages,
                        onCheckedChange = { appViewModel.setVolumeKeysTurnPages(it) }
                    )
                }

                // Section 5: Recitation
                SectionCard {
                    SectionTitle("Recitation")
                    ToggleRow(
                        label = "Continue to next verse automatically",
                        checked = settings.audioAutoAdvance,
                        onCheckedChange = { appViewModel.setAudioAutoAdvance(it) }
                    )
                    ToggleRow(
                        label = "Follow each verse with translation narration",
                        checked = settings.followTranslation,
                        onCheckedChange = { appViewModel.setFollowTranslation(it) }
                    )
                    Text(
                        text = "After the Arabic, each verse is read aloud in English and/or Urdu (card view; the paged mushaf stays Arabic-only).",
                        color = colors.muted,
                        fontSize = 12.sp,
                        fontFamily = fonts.serif,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                    if (settings.followTranslation) {
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            ToggleRow(
                                label = "English narration · Brian",
                                checked = settings.followEn,
                                enabled = settings.followTranslation,
                                onCheckedChange = { appViewModel.setFollowEn(it) }
                            )
                            ToggleRow(
                                label = "Urdu narration · Jalandhari",
                                checked = settings.followUr,
                                enabled = settings.followTranslation,
                                onCheckedChange = { appViewModel.setFollowUr(it) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRecitation() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download audio for offline",
                            modifier = Modifier.weight(1f),
                            color = colors.text,
                            fontSize = 15.sp,
                            fontFamily = fonts.serif
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.muted
                        )
                    }
                }

                // Section 5: About & credits
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAbout() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "About & credits",
                            modifier = Modifier.weight(1f),
                            color = colors.text,
                            fontSize = 15.sp,
                            fontFamily = fonts.serif
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.muted
                        )
                    }
                }

                // Section 6: App info & Diagnostics
                SectionCard {
                    SectionTitle("App info")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Version",
                            modifier = Modifier.weight(1f),
                            color = colors.text,
                            fontSize = 15.sp,
                            fontFamily = fonts.serif
                        )
                        Text(
                            text = appVersionLabel(LocalContext.current),
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontFamily = fonts.serif
                        )
                    }
                    GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    DiagnosticsRow()
                    RepairRow(container)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        border = null
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    val fonts = rememberAppFonts()
    val colors = appColors
    Text(
        text = title.uppercase(Locale.ROOT),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = colors.accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = fonts.serif,
        letterSpacing = 1.5.sp
    )
    GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun ThemeRow(
    label: String,
    choice: ThemeChoice,
    selected: ThemeChoice,
    onSelect: (ThemeChoice) -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = (selected == choice),
                role = Role.RadioButton,
                onClick = { onSelect(choice) }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (selected == choice),
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.accent,
                unselectedColor = colors.muted
            )
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = colors.text,
            fontSize = 15.sp,
            fontFamily = fonts.serif
        )
    }
}

@Composable
private fun FontSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = colors.text,
                fontSize = 14.sp,
                fontFamily = fonts.serif
            )
            Text(
                text = "$value%",
                color = colors.muted,
                fontSize = 13.sp,
                fontFamily = fonts.serif
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 70f..160f,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.hairline
            )
        )
    }
}

@Composable
private fun DiagnosticsRow() {
    val colors = appColors
    val fonts = rememberAppFonts()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var report by remember { mutableStateOf(CrashReporter.readReport(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (report == null) {
            Text(
                text = "No crash reports on this device.",
                color = colors.muted,
                fontSize = 13.sp,
                fontFamily = fonts.serif
            )
        } else {
            Text(
                text = "JustQuran saved a crash report on this device.",
                color = colors.text,
                fontSize = 14.sp,
                fontFamily = fonts.serif
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showDialog = true }) {
                    Text(
                        text = "View",
                        color = colors.accent,
                        fontFamily = fonts.serif
                    )
                }
                TextButton(
                    onClick = {
                        val currentReport = report ?: return@TextButton
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "JustQuran crash report")
                            putExtra(Intent.EXTRA_TEXT, currentReport)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Share crash report"))
                        } catch (_: Throwable) {}
                    }
                ) {
                    Text(
                        text = "Share",
                        color = colors.accent,
                        fontFamily = fonts.serif
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        CrashReporter.clear(context)
                        report = null
                    }
                ) {
                    Text(
                        text = "Clear",
                        color = colors.muted,
                        fontFamily = fonts.serif
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                copied = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(report ?: ""))
                        copied = true
                    }
                ) {
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        fontFamily = fonts.serif
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        copied = false
                    }
                ) {
                    Text(
                        text = "Close",
                        fontFamily = fonts.serif
                    )
                }
            },
            title = {
                Text(
                    text = "Crash report",
                    fontWeight = FontWeight.Bold,
                    fontFamily = fonts.serif
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = report ?: "",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun RepairRow(appContainer: AppContainer) {
    val colors = appColors
    val fonts = rememberAppFonts()
    val context = LocalContext.current
    var isRepairing by remember { mutableStateOf(false) }
    var confirmRepair by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedButton(
            onClick = { confirmRepair = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRepairing
        ) {
            Text(
                text = if (isRepairing) "Repairing…" else "Repair offline files & reload",
                fontSize = 14.sp,
                fontFamily = fonts.serif
            )
            if (isRepairing) {
                Spacer(modifier = Modifier.size(10.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp
                )
            }
        }
    }

    if (confirmRepair) {
        AlertDialog(
            onDismissRequest = { confirmRepair = false },
            title = {
                Text(
                    text = "Repair offline files & reload?",
                    fontFamily = fonts.serif
                )
            },
            text = {
                Text(
                    text = "Downloaded audio and cached files will be cleared, then the app restarts fresh. Your bookmarks and settings are kept.",
                    fontSize = 14.sp,
                    fontFamily = fonts.serif,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRepair = false
                        isRepairing = true
                        CoroutineScope(Dispatchers.IO).launch {
                            appContainer.audioDownloadRepository.cancelDownload()
                            File(context.applicationContext.filesDir, "audio").deleteRecursively()
                            context.applicationContext.cacheDir.deleteRecursively()
                            withContext(Dispatchers.Main) {
                                (context as? Activity)?.recreate()
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Repair",
                        color = colors.accent,
                        fontFamily = fonts.serif
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRepair = false }) {
                    Text(
                        text = "Cancel",
                        color = colors.muted,
                        fontFamily = fonts.serif
                    )
                }
            }
        )
    }
}

@Composable
private fun PageStylePill(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) colors.accent else Color.Transparent)
            .border(1.dp, if (selected) colors.accent else colors.hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF101913) else colors.muted,
            fontSize = 13.sp,
            fontFamily = fonts.serif
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = appColors
    val fonts = rememberAppFonts()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (enabled) colors.text else colors.muted,
            fontSize = 15.sp,
            fontFamily = fonts.serif
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.bg,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.muted,
                uncheckedTrackColor = colors.hairline
            )
        )
    }
}
