package org.justquran.app.ui.reader

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder

class FolioBuild(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>,
    val placeholders: List<AnnotatedString.Range<Placeholder>>
)
