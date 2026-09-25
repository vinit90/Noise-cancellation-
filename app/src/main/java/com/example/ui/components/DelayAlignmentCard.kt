package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkSurfaceVariant
import com.example.ui.theme.LabDarkVoid
import com.example.ui.theme.VioletMod
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DelayAlignmentCard(
    delayMs: Int,
    onDelayChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Speed of sound = 343 m/s = 0.343 m/ms = 34.3 cm/ms
    val distanceMeters = delayMs * 0.343f
    val distanceFeet = distanceMeters * 3.28084f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(1.dp, LabDarkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("delay_control_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACOUSTIC & BT DELAY LINE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Acoustic Path & BLE Alignment",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Delay Readout Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(VioletMod.copy(alpha = 0.15f))
                    .border(1.dp, VioletMod.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${delayMs} ms",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = VioletMod
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Distance / Bluetooth Latency indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LabDarkVoid)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "Acoustic Flight: %.2f m (%.1f ft)", distanceMeters, distanceFeet),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = CyanNeon
            )
            Text(
                text = if (delayMs >= 40) "BLE Buffer Sync" else "Direct Air Path",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (delayMs >= 40) VioletMod else Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slider with - and + step buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { onDelayChanged((delayMs - 1).coerceAtLeast(0)) },
                modifier = Modifier
                    .size(36.dp)
                    .background(LabDarkSurfaceVariant, CircleShape)
                    .testTag("delay_decrement_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease delay 1ms",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Slider(
                value = delayMs.toFloat(),
                onValueChange = { onDelayChanged(it.toInt()) },
                valueRange = 0f..200f,
                steps = 199,
                colors = SliderDefaults.colors(
                    thumbColor = VioletMod,
                    activeTrackColor = VioletMod,
                    inactiveTrackColor = LabDarkSurfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("delay_offset_slider")
            )

            IconButton(
                onClick = { onDelayChanged((delayMs + 1).coerceAtMost(200)) },
                modifier = Modifier
                    .size(36.dp)
                    .background(LabDarkSurfaceVariant, CircleShape)
                    .testTag("delay_increment_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase delay 1ms",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Quick Preset Chips (Air path & Bluetooth latency modes)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                0 to "0ms (Direct)",
                5 to "5ms (1.7m)",
                15 to "15ms (5.1m)",
                50 to "50ms (BLE Low)",
                120 to "120ms (BT A2DP)"
            )

            presets.forEach { (presetMs, label) ->
                val isSelected = delayMs == presetMs
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) VioletMod.copy(alpha = 0.25f) else LabDarkVoid)
                        .border(
                            1.dp,
                            if (isSelected) VioletMod else Color(0xFF1E293B),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onDelayChanged(presetMs) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) VioletMod else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
