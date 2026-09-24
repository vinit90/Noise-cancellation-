package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkVoid
import com.example.ui.theme.MeterGreen
import com.example.ui.theme.MeterOrange
import com.example.ui.theme.MeterRed
import com.example.ui.theme.MeterYellow
import java.util.Locale

@Composable
fun VuMeterView(
    inputDbfs: Float,
    outputDbfs: Float,
    inputRms: Float,
    outputRms: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(1.dp, LabDarkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("vu_meter_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE AUDIO LEVEL METERS (dBFS)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = String.format(Locale.US, "IN: %+.1f dB", inputDbfs),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (inputDbfs > -6f) MeterRed else if (inputDbfs > -18f) MeterYellow else CyanNeon
            )
        }

        // dBFS Axis Ticks (-60, -36, -24, -12, -6, 0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp, start = 72.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("-60", "-36", "-24", "-12", "-6", "0").forEach { tick ->
                Text(
                    text = tick,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (tick == "0") MeterRed else Color(0xFF64748B)
                )
            }
        }

        // Mic Input Bar
        VuBarRow(
            label = "MIC IN",
            dbfs = inputDbfs,
            tag = "vu_mic_in_bar"
        )

        // Speaker Output Bar
        VuBarRow(
            label = "SPK OUT",
            dbfs = outputDbfs,
            tag = "vu_spk_out_bar"
        )
    }
}

@Composable
private fun VuBarRow(
    label: String,
    dbfs: Float,
    tag: String
) {
    // Map -60dBFS to 0.0, and 0dBFS to 1.0
    val normalizedFraction = ((dbfs + 60f) / 60f).coerceIn(0.02f, 1.0f)
    val animatedWidth by animateFloatAsState(
        targetValue = normalizedFraction,
        animationSpec = tween(durationMillis = 60),
        label = "vu_bar"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            modifier = Modifier.width(68.dp)
        )

        // Meter Track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LabDarkVoid)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .testTag(tag)
        ) {
            // Gradient fill corresponding to amplitude
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedWidth)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MeterGreen,
                                MeterYellow,
                                MeterOrange,
                                MeterRed
                            )
                        )
                    )
            )
        }
    }
}
