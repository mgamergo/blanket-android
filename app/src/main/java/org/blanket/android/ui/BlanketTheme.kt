package org.blanket.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.blanket.android.R
import org.blanket.android.data.ThemeMode

data class BlanketColors(val dark: Boolean) {
    val background = Color(if (dark) 0xff242424 else 0xfffafafa)
    val bar = Color(if (dark) 0xff303030 else 0xffffffff)
    val foreground = Color(if (dark) 0xfffafafa else 0xff333338)
    val muted = Color(if (dark) 0xffb8b8b8 else 0xff65656a)
    val separator = Color(if (dark) 0xff454545 else 0xffdedede)
    val accent = Color(if (dark) 0xff78aeed else 0xff1c71d8)
    val accentBackground = Color(if (dark) 0xff28394f else 0xffd2e3f9)
    val blue = Color(0xff3584e4)
    val track = Color(if (dark) 0xff363636 else 0xffebebed)
    val disabledFill = Color(if (dark) 0xff4c4c4c else 0xffcfcfd2)
}

val LocalBlanketColors = staticCompositionLocalOf { BlanketColors(false) }
val BlanketFont =
    FontFamily(Font(R.font.cantarell_regular), Font(R.font.cantarell_bold, FontWeight.Bold))

@Composable
fun BlanketTheme(theme: ThemeMode, content: @Composable () -> Unit) {
    val dark =
        when (theme) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    CompositionLocalProvider(LocalBlanketColors provides BlanketColors(dark), content = content)
}

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 15,
    bold: Boolean = false,
    color: Color = LocalBlanketColors.current.foreground,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text,
        modifier,
        style =
            TextStyle(
                color = color,
                fontFamily = BlanketFont,
                fontSize = size.sp,
                lineHeight = (size + 5).sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = align,
            ),
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}
