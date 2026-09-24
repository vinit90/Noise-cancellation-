package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Speed
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

@Composable
fun DelayAlignmentCard(
    delayMs: Int,
    onDelayChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 343 m/s = 34.3 cm/ms = 0.343 m/ms
    val distanceMeters = delayMs * 0.343f
    val distanceInches = delayMs * 13.504f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(1.dp, LabDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("delay_control_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACOUSTIC DELAY LINE (RING BUFFER)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Acoustic Path Alignment",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Big Live Delay Readout Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(VioletMod.copy(alpha = 0.15f))
                    .border(1.dp, VioletMod.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${delayMs} ms",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = VioletMod
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Equivalent Acoustic Distance Banner
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
                text = String.format(Locale.US, "Air Path: %.2f m (%.1f in)", distanceMeters, distanceInches),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "${delayMs * 48} samples @ 48kHz",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Slider with - and + step buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }

            Slider(
                value = delayMs.toFloat(),
                onValueChange = { onDelayChanged(it.toInt()) },
                valueRange = 0f..100f,
                steps = 99,
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
                onClick = { onDelayChanged((delayMs + 1).coerceAtMost(100)) },
                modifier = Modifier
                    .size(36.dp)
                    .background(LabDarkSurfaceVariant, CircleShape)
                    .testTag("delay_increment_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase delay 1ms",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Quick Preset Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 3, 10, 25, 50).forEach { presetMs ->
                val isSelected = delayMs == presetMs
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) VioletMod.copy(alpha = 0.25f) else LabDarkVoid)
                        .border(
                            1.dp,
                            if (isSelected) VioletMod else Color(0xFF1E293B),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onDelayChanged(presetMs) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${presetMs}ms",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) VioletMod else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
