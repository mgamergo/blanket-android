package org.blanket.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.caverock.androidsvg.SVG

@Composable
fun SoundIcon(id: String, name: String, tint: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap =
        remember(id) {
            val svg =
                context.assets.open("icons/com.rafaelmardojai.Blanket-$id-symbolic.svg").use {
                    SVG.getFromInputStream(it)
                }
            createBitmap(192, 192)
                .apply {
                    val canvas = android.graphics.Canvas(this)
                    canvas.scale(3f, 3f)
                    svg.renderToCanvas(canvas)
                }
                .asImageBitmap()
        }
    Image(
        bitmap,
        contentDescription = name.takeIf { it.isNotEmpty() },
        modifier = modifier.size(64.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun GlyphButton(
    glyph: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    blue: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalBlanketColors.current
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (blue) colors.blue else Color.Transparent)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val tint =
            if (blue) Color.White else colors.foreground.copy(alpha = if (enabled) 1f else 0.35f)
        if (glyph in listOf("▶", "Ⅱ", "☰", "⋮", "◖", "‹", "×")) {
            Canvas(Modifier.size(24.dp)) {
                val w = size.width
                val h = size.height
                fun path(
                    vararg points: Pair<Float, Float>,
                    close: Boolean = false,
                ): androidx.compose.ui.graphics.Path =
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(points[0].first * w, points[0].second * h)
                        points.drop(1).forEach { lineTo(it.first * w, it.second * h) }
                        if (close) close()
                    }
                when (glyph) {
                    "▶" ->
                        drawPath(
                            path(0.3f to 0.18f, 0.8f to 0.5f, 0.3f to 0.82f, close = true),
                            tint,
                        )
                    "Ⅱ" -> {
                        drawRect(
                            tint,
                            androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.18f),
                            androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.64f),
                        )
                        drawRect(
                            tint,
                            androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.18f),
                            androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.64f),
                        )
                    }
                    "☰" ->
                        listOf(0.25f, 0.5f, 0.75f).forEach { y ->
                            drawLine(
                                tint,
                                androidx.compose.ui.geometry.Offset(w * 0.18f, h * y),
                                androidx.compose.ui.geometry.Offset(w * 0.82f, h * y),
                                2.dp.toPx(),
                            )
                        }
                    "⋮" ->
                        listOf(0.25f, 0.5f, 0.75f).forEach { y ->
                            drawCircle(
                                tint,
                                1.5.dp.toPx(),
                                androidx.compose.ui.geometry.Offset(w * 0.5f, h * y),
                            )
                        }
                    "◖" -> {
                        drawPath(
                            path(
                                0.1f to 0.38f,
                                0.3f to 0.38f,
                                0.55f to 0.16f,
                                0.55f to 0.84f,
                                0.3f to 0.62f,
                                0.1f to 0.62f,
                                close = true,
                            ),
                            tint,
                        )
                        drawPath(
                            path(0.7f to 0.32f, 0.82f to 0.5f, 0.7f to 0.68f),
                            tint,
                            style =
                                androidx.compose.ui.graphics.drawscope.Stroke(
                                    2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )
                    }
                    "‹" ->
                        drawPath(
                            path(0.65f to 0.2f, 0.35f to 0.5f, 0.65f to 0.8f),
                            tint,
                            style =
                                androidx.compose.ui.graphics.drawscope.Stroke(
                                    2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )
                    "×" -> {
                        drawLine(
                            tint,
                            androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.3f),
                            androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.7f),
                            2.dp.toPx(),
                            StrokeCap.Round,
                        )
                        drawLine(
                            tint,
                            androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.3f),
                            androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.7f),
                            2.dp.toPx(),
                            StrokeCap.Round,
                        )
                    }
                }
            }
        } else Label(glyph, size = 23, color = tint)
    }
}

@Composable
fun BlanketSlider(
    value: Float,
    description: String,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val colors = LocalBlanketColors.current
    val changeValue by rememberUpdatedState(onChange)
    Canvas(
        modifier
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                setProgress {
                    changeValue(it.coerceIn(0f, 1f))
                    true
                }
            }
            .onKeyEvent {
                if (
                    it.type == KeyEventType.KeyDown &&
                        (it.key == Key.DirectionLeft || it.key == Key.DirectionRight)
                ) {
                    changeValue(
                        (value + if (it.key == Key.DirectionRight) 0.05f else -0.05f).coerceIn(
                            0f,
                            1f,
                        )
                    )
                    true
                } else false
            }
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures { changeValue((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { changeValue((it.x / size.width).coerceIn(0f, 1f)) }
                ) { change, _ ->
                    change.consume()
                    changeValue((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val inset = 4.dp.toPx()
        val y = size.height / 2
        val start = androidx.compose.ui.geometry.Offset(inset, y)
        val end = androidx.compose.ui.geometry.Offset(size.width - inset, y)
        drawLine(colors.track, start, end, 8.dp.toPx(), StrokeCap.Round)
        if (value > 0f)
            drawLine(
                if (active) colors.blue else colors.disabledFill,
                start,
                androidx.compose.ui.geometry.Offset(inset + (size.width - inset * 2) * value, y),
                8.dp.toPx(),
                StrokeCap.Round,
            )
    }
}
