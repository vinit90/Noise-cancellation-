package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAntiPhase
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkVoid

@Composable
fun OscilloscopeView(
    inputWaveform: FloatArray,
    outputWaveform: FloatArray,
    isPhaseInverted: Boolean,
    isLimiting: Boolean,
    modifier: Modifier = Modifier
) {
    val gridColor = LabDarkBorder.copy(alpha = 0.4f)
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(1.dp, LabDarkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("oscilloscope_container")
    ) {
        // Header with legend and status badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "REAL-TIME OSCILLOSCOPE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Legend indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Input legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CyanNeon, CircleShape)
                    )
                    Text(
                        text = "Mic Input",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyanNeon
                    )
                }

                // Output legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AmberAntiPhase, CircleShape)
                    )
                    Text(
                        text = if (isPhaseInverted) "Anti-Phase Ø" else "Output",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AmberAntiPhase
                    )
                }
            }
        }

        // Oscilloscope Scope Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(140.dp)
                .background(LabDarkVoid, RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .testTag("oscilloscope_canvas")
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Draw background grid lines (horizontal divisions)
                val hSteps = 4
                for (i in 1..hSteps) {
                    val y = height * (i.toFloat() / (hSteps + 1))
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                // Vertical divisions
                val vSteps = 6
                for (i in 1..vSteps) {
                    val x = width * (i.toFloat() / (vSteps + 1))
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                // Draw bold center zero axis
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.5f
                )

                // Render Input Waveform (Cyan)
                if (inputWaveform.isNotEmpty()) {
                    val inPath = Path()
                    val stepX = width / (inputWaveform.size - 1)
                    var started = false

                    for (i in inputWaveform.indices) {
                        val sample = inputWaveform[i].coerceIn(-1.0f, 1.0f)
                        val x = i * stepX
                        val y = midY - (sample * (midY * 0.85f))

                        if (!started) {
                            inPath.moveTo(x, y)
                            started = true
                        } else {
                            inPath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = inPath,
                        color = CyanNeon.copy(alpha = 0.9f),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }

                // Render Output / Anti-Phase Waveform (Amber/Coral)
                if (outputWaveform.isNotEmpty()) {
                    val outPath = Path()
                    val stepX = width / (outputWaveform.size - 1)
                    var started = false

                    for (i in outputWaveform.indices) {
                        val sample = outputWaveform[i].coerceIn(-1.0f, 1.0f)
                        val x = i * stepX
                        val y = midY - (sample * (midY * 0.85f))

                        if (!started) {
                            outPath.moveTo(x, y)
                            started = true
                        } else {
                            outPath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = outPath,
                        color = AmberAntiPhase.copy(alpha = 0.95f),
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                    )
                }
            }

            // Overlay indicator in corner
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "48 kHz PCM",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
                if (isLimiting) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIMITER SATURATING",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}
