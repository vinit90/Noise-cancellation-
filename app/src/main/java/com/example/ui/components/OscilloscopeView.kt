package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAntiPhase
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkVoid
import java.util.Locale

enum class ScopeDisplayMode {
    DUAL,         // Channel 1 (Input) & Channel 2 (Anti-Phase DSP)
    SUM_RESIDUAL, // Channel 1 + Channel 2 (Shows active cancellation)
    XY_LISSAJOUS  // Phase plot: X=Input vs Y=Output
}

@Composable
fun OscilloscopeView(
    inputWaveform: FloatArray,
    outputWaveform: FloatArray,
    isPhaseInverted: Boolean,
    isLimiting: Boolean,
    peakToPeak: Float = 0f,
    estimatedFreqHz: Float = 0f,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(ScopeDisplayMode.DUAL) }
    var verticalZoom by remember { mutableFloatStateOf(1.0f) }
    var horizontalZoom by remember { mutableIntStateOf(1) }
    var isFrozen by remember { mutableStateOf(false) }

    var frozenInput by remember { mutableStateOf(FloatArray(256)) }
    var frozenOutput by remember { mutableStateOf(FloatArray(256)) }

    val activeInput = if (isFrozen) frozenInput else inputWaveform
    val activeOutput = if (isFrozen) frozenOutput else outputWaveform

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(1.dp, LabDarkBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("oscilloscope_container")
    ) {
        // --- 1. Header Toolbar with Title, Telemetry Readouts & Freeze Button ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "DIGITAL OSCILLOSCOPE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (estimatedFreqHz > 10f) {
                                String.format(Locale.US, "%.0f Hz", estimatedFreqHz)
                            } else {
                                "-- Hz"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                        Text(text = "•", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            text = String.format(Locale.US, "Vpp: %.2f", peakToPeak),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = AmberAntiPhase
                        )
                    }
                }
            }

            // Freeze / Run Trigger Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isFrozen) Color(0xFFEF4444).copy(alpha = 0.2f) else LabDarkVoid)
                    .border(
                        1.dp,
                        if (isFrozen) Color(0xFFEF4444) else Color(0xFF334155),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        if (!isFrozen) {
                            frozenInput = inputWaveform.copyOf()
                            frozenOutput = outputWaveform.copyOf()
                        }
                        isFrozen = !isFrozen
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("scope_freeze_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isFrozen) "Resume Scope" else "Hold Frame",
                        tint = if (isFrozen) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isFrozen) "HOLD" else "RUN",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isFrozen) Color(0xFFEF4444) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. Mode Selection Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScopeModeChip(
                label = "DUAL TRACE",
                isSelected = displayMode == ScopeDisplayMode.DUAL,
                selectedColor = CyanNeon,
                onClick = { displayMode = ScopeDisplayMode.DUAL },
                modifier = Modifier.weight(1f),
                testTag = "scope_mode_dual"
            )

            ScopeModeChip(
                label = "∑ CANCEL",
                isSelected = displayMode == ScopeDisplayMode.SUM_RESIDUAL,
                selectedColor = EmeraldActive,
                onClick = { displayMode = ScopeDisplayMode.SUM_RESIDUAL },
                modifier = Modifier.weight(1f),
                testTag = "scope_mode_sum"
            )

            ScopeModeChip(
                label = "X-Y PHASE",
                isSelected = displayMode == ScopeDisplayMode.XY_LISSAJOUS,
                selectedColor = AmberAntiPhase,
                onClick = { displayMode = ScopeDisplayMode.XY_LISSAJOUS },
                modifier = Modifier.weight(1f),
                testTag = "scope_mode_xy"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 3. Main Oscilloscope Screen (Responsive Height) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LabDarkVoid)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .testTag("oscilloscope_screen")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midX = w / 2f
                val midY = h / 2f

                drawGraticule(
                    width = w,
                    height = h,
                    gridColor = Color(0xFF1E293B),
                    axisColor = Color(0xFF334155),
                    dashEffect = dashEffect,
                    isLissajous = displayMode == ScopeDisplayMode.XY_LISSAJOUS
                )

                when (displayMode) {
                    ScopeDisplayMode.DUAL -> {
                        drawWaveform(
                            waveform = activeInput,
                            color = CyanNeon,
                            glowColor = CyanNeon.copy(alpha = 0.25f),
                            verticalZoom = verticalZoom,
                            horizontalZoom = horizontalZoom,
                            width = w,
                            height = h,
                            midY = midY
                        )
                        drawWaveform(
                            waveform = activeOutput,
                            color = AmberAntiPhase,
                            glowColor = AmberAntiPhase.copy(alpha = 0.25f),
                            verticalZoom = verticalZoom,
                            horizontalZoom = horizontalZoom,
                            width = w,
                            height = h,
                            midY = midY
                        )
                    }

                    ScopeDisplayMode.SUM_RESIDUAL -> {
                        val residual = FloatArray(activeInput.size) { i ->
                            val sIn = activeInput.getOrElse(i) { 0f }
                            val sOut = activeOutput.getOrElse(i) { 0f }
                            sIn + sOut
                        }
                        drawResidualWaveform(
                            waveform = residual,
                            primaryColor = EmeraldActive,
                            glowColor = EmeraldActive.copy(alpha = 0.3f),
                            fillColor = EmeraldActive.copy(alpha = 0.12f),
                            verticalZoom = verticalZoom,
                            horizontalZoom = horizontalZoom,
                            width = w,
                            height = h,
                            midY = midY
                        )
                    }

                    ScopeDisplayMode.XY_LISSAJOUS -> {
                        drawLissajous(
                            inputWaveform = activeInput,
                            outputWaveform = activeOutput,
                            color = if (isPhaseInverted) AmberAntiPhase else CyanNeon,
                            glowColor = if (isPhaseInverted) AmberAntiPhase.copy(alpha = 0.2f) else CyanNeon.copy(alpha = 0.2f),
                            zoom = verticalZoom,
                            width = w,
                            height = h,
                            midX = midX,
                            midY = midY
                        )
                    }
                }
            }

            // Screen On-Screen Display (OSD) Overlay Badges
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (displayMode) {
                            ScopeDisplayMode.DUAL -> "CH1(IN) + CH2(OUT)"
                            ScopeDisplayMode.SUM_RESIDUAL -> "INTERFERENCE (CH1+CH2)"
                            ScopeDisplayMode.XY_LISSAJOUS -> "X-Y PHASE PLOT"
                        },
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = when (displayMode) {
                            ScopeDisplayMode.DUAL -> CyanNeon
                            ScopeDisplayMode.SUM_RESIDUAL -> EmeraldActive
                            ScopeDisplayMode.XY_LISSAJOUS -> AmberAntiPhase
                        }
                    )
                }

                if (isLimiting) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIMITER",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Bottom Right Scale Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${verticalZoom.toInt()}x V • ${horizontalZoom}x T",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 4. Scale & Zoom Tool Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Amplitude / Voltage Scale Selector (1x, 2x, 5x)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "GAIN:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    color = Color(0xFF94A3B8)
                )
                listOf(1f, 2f, 5f).forEach { zoom ->
                    val isSelected = verticalZoom == zoom
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else LabDarkVoid)
                            .border(0.5.dp, if (isSelected) CyanNeon else Color(0xFF334155), RoundedCornerShape(4.dp))
                            .clickable { verticalZoom = zoom }
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                            .testTag("vertical_zoom_${zoom.toInt()}x"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${zoom.toInt()}x",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CyanNeon else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Timebase / Horizontal Scale Selector (1x, 2x, 4x)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "TIME:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    color = Color(0xFF94A3B8)
                )
                listOf(1, 2, 4).forEach { zoom ->
                    val isSelected = horizontalZoom == zoom
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) AmberAntiPhase.copy(alpha = 0.2f) else LabDarkVoid)
                            .border(0.5.dp, if (isSelected) AmberAntiPhase else Color(0xFF334155), RoundedCornerShape(4.dp))
                            .clickable { horizontalZoom = zoom }
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                            .testTag("horizontal_zoom_${zoom}x"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${zoom}x",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AmberAntiPhase else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGraticule(
    width: Float,
    height: Float,
    gridColor: Color,
    axisColor: Color,
    dashEffect: PathEffect,
    isLissajous: Boolean
) {
    val midX = width / 2f
    val midY = height / 2f

    val hDivisions = 8
    for (i in 1 until hDivisions) {
        val y = height * (i.toFloat() / hDivisions)
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.8f,
            pathEffect = dashEffect
        )
    }

    val vDivisions = 10
    for (i in 1 until vDivisions) {
        val x = width * (i.toFloat() / vDivisions)
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 0.8f,
            pathEffect = dashEffect
        )
    }

    // Center Axes
    drawLine(color = axisColor, start = Offset(0f, midY), end = Offset(width, midY), strokeWidth = 1.2f)
    drawLine(color = axisColor, start = Offset(midX, 0f), end = Offset(midX, height), strokeWidth = 1.2f)

    if (isLissajous) {
        val radiusMax = minOf(midX, midY) * 0.85f
        drawCircle(color = gridColor, radius = radiusMax, center = Offset(midX, midY), style = Stroke(width = 0.8f, pathEffect = dashEffect))
    }
}

private fun DrawScope.drawWaveform(
    waveform: FloatArray,
    color: Color,
    glowColor: Color,
    verticalZoom: Float,
    horizontalZoom: Int,
    width: Float,
    height: Float,
    midY: Float
) {
    if (waveform.size < 2) return

    val visibleCount = (waveform.size / horizontalZoom).coerceIn(16, waveform.size)
    val stepX = width / (visibleCount - 1)
    val path = Path()
    var started = false
    val maxAmplitudePixels = midY * 0.88f

    for (i in 0 until visibleCount) {
        val sample = (waveform[i] * verticalZoom).coerceIn(-1.2f, 1.2f)
        val x = i * stepX
        val y = midY - (sample * maxAmplitudePixels)

        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(path = path, color = glowColor, style = Stroke(width = 4.0f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = path, color = color, style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawResidualWaveform(
    waveform: FloatArray,
    primaryColor: Color,
    glowColor: Color,
    fillColor: Color,
    verticalZoom: Float,
    horizontalZoom: Int,
    width: Float,
    height: Float,
    midY: Float
) {
    if (waveform.size < 2) return

    val visibleCount = (waveform.size / horizontalZoom).coerceIn(16, waveform.size)
    val stepX = width / (visibleCount - 1)
    val linePath = Path()
    val fillPath = Path()
    var started = false
    val maxAmplitudePixels = midY * 0.88f

    fillPath.moveTo(0f, midY)

    for (i in 0 until visibleCount) {
        val sample = (waveform[i] * verticalZoom).coerceIn(-1.2f, 1.2f)
        val x = i * stepX
        val y = midY - (sample * maxAmplitudePixels)

        if (!started) {
            linePath.moveTo(x, y)
            fillPath.lineTo(x, y)
            started = true
        } else {
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
    }

    fillPath.lineTo(width, midY)
    fillPath.close()

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(fillColor, Color.Transparent),
            startY = midY - 50f,
            endY = midY + 50f
        )
    )

    drawPath(path = linePath, color = glowColor, style = Stroke(width = 4.0f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = linePath, color = primaryColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawLissajous(
    inputWaveform: FloatArray,
    outputWaveform: FloatArray,
    color: Color,
    glowColor: Color,
    zoom: Float,
    width: Float,
    height: Float,
    midX: Float,
    midY: Float
) {
    val count = minOf(inputWaveform.size, outputWaveform.size)
    if (count < 2) return

    val maxScale = minOf(midX, midY) * 0.85f * zoom
    val path = Path()
    var started = false

    for (i in 0 until count) {
        val xVal = (inputWaveform[i]).coerceIn(-1.2f, 1.2f)
        val yVal = (outputWaveform[i]).coerceIn(-1.2f, 1.2f)

        val px = midX + (xVal * maxScale)
        val py = midY - (yVal * maxScale)

        if (!started) {
            path.moveTo(px, py)
            started = true
        } else {
            path.lineTo(px, py)
        }
    }

    drawPath(path = path, color = glowColor, style = Stroke(width = 4.0f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path = path, color = color, style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun ScopeModeChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val activeBg by animateColorAsState(
        targetValue = if (isSelected) selectedColor.copy(alpha = 0.18f) else LabDarkVoid,
        animationSpec = tween(durationMillis = 150),
        label = "mode_chip_bg"
    )
    val activeBorder by animateColorAsState(
        targetValue = if (isSelected) selectedColor else Color(0xFF334155),
        animationSpec = tween(durationMillis = 150),
        label = "mode_chip_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(activeBg)
            .border(1.dp, activeBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) selectedColor else Color(0xFF94A3B8)
        )
    }
}
