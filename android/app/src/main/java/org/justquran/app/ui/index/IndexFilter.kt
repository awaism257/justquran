package org.justquran.app.ui.index

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.justquran.app.ui.theme.appColors
import org.justquran.app.ui.theme.rememberAppFonts
import java.text.Normalizer
import java.util.Locale

fun foldQuery(s: String): String {
    val norm = Normalizer.normalize(s, Normalizer.Form.NFD)
    return Regex("\\p{Mn}+").replace(norm, "").lowercase(Locale.ROOT)
}

@Composable
fun IndexFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val fonts = rememberAppFonts()
    val colors = appColors

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        textStyle = TextStyle(
            fontSize = 14.sp,
            fontFamily = fonts.serif,
            color = colors.text
        ),
        placeholder = {
            Text(
                text = placeholder,
                color = colors.muted,
                fontSize = 14.sp,
                fontFamily = fonts.serif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.muted
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedContainerColor = colors.card,
            unfocusedContainerColor = colors.card,
            cursorColor = colors.accent,
            focusedBorderColor = colors.hairline,
            unfocusedBorderColor = colors.hairline
        )
    )
}
