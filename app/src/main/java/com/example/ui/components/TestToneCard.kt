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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

@Composable
fun TestToneCard(
    isTestToneEnabled: Boolean,
    testToneFreq: Float,
    onToggleTestTone: () -> Unit,
    onSelectFreq: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(1.dp, LabDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("test_tone_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Test Tone",
                    tint = if (isTestToneEnabled) CyanNeon else Color(0xFF64748B)
                )
                Column {
                    Text(
                        text = "CALIBRATION SINE GENERATOR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isTestToneEnabled) "Synthesizing ${testToneFreq.toInt()} Hz Reference" else "Disabled (Listening to Mic)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTestToneEnabled) CyanNeon else Color(0xFF94A3B8)
                    )
                }
            }

            Switch(
                checked = isTestToneEnabled,
                onCheckedChange = { onToggleTestTone() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LabDarkVoid,
                    checkedTrackColor = CyanNeon,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = LabDarkSurfaceVariant
                ),
                modifier = Modifier.testTag("test_tone_switch")
            )
        }

        if (isTestToneEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(250f, 440f, 1000f, 2000f).forEach { freq ->
                    val isSelected = testToneFreq == freq
                    val label = if (freq >= 1000f) "${(freq / 1000).toInt()} kHz" else "${freq.toInt()} Hz"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else LabDarkVoid)
                            .border(
                                1.dp,
                                if (isSelected) CyanNeon else Color(0xFF1E293B),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectFreq(freq) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CyanNeon else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
