package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkSurfaceVariant
import com.example.ui.theme.LabDarkVoid
import java.util.Locale
import kotlin.math.log10

@Composable
fun GainControlCard(
    gain: Float,
    isMuted: Boolean,
    isLimiterEnabled: Boolean,
    isLimitingActive: Boolean,
    onGainChanged: (Float) -> Unit,
    onToggleLimiter: (Boolean) -> Unit,
    onEmergencyMute: () -> Unit,
    onUnmute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gainDb = if (gain > 0.001f) 20f * log10(gain) else -60f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(1.dp, LabDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("gain_control_card")
    ) {
        // Card Title & Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DSP GAIN & FEEDBACK LIMITER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Output Amplitude Scaling",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Gain readout badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isMuted) CrimsonAlert.copy(alpha = 0.15f) else CyanNeon.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isMuted) CrimsonAlert else CyanNeon.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isMuted) "MUTED" else String.format(Locale.US, "%.2fx (%+.1fdB)", gain, gainDb),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isMuted) CrimsonAlert else CyanNeon
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Gain Slider
        Slider(
            value = if (isMuted) 0f else gain,
            onValueChange = onGainChanged,
            valueRange = 0.0f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = CyanNeon,
                activeTrackColor = CyanNeon,
                inactiveTrackColor = LabDarkSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gain_slider")
        )

        // Limiter & Safety Status Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LabDarkVoid)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Limiter Icon",
                    tint = if (isLimiterEnabled) EmeraldActive else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Feedback Soft-Limiter (tanh)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isLimitingActive) {
                            "⚠️ Saturating runaway feedback loop!"
                        } else if (isLimiterEnabled) {
                            "Acoustic hearing & speaker driver protection active"
                        } else {
                            "Disabled (Caution: High risk of squealing!)"
                        },
                        fontSize = 10.sp,
                        color = if (isLimitingActive) CrimsonAlert else Color(0xFF94A3B8)
                    )
                }
            }

            Switch(
                checked = isLimiterEnabled,
                onCheckedChange = onToggleLimiter,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LabDarkVoid,
                    checkedTrackColor = EmeraldActive,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = LabDarkSurfaceVariant
                ),
                modifier = Modifier.testTag("limiter_switch")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Big Emergency Mute Button
        Button(
            onClick = {
                if (isMuted) {
                    onUnmute()
                } else {
                    onEmergencyMute()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMuted) Color(0xFF1E293B) else CrimsonAlert,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("emergency_mute_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                    contentDescription = if (isMuted) "Unmute" else "Emergency Mute",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isMuted) "RESTORE NOMINAL AUDIO (1.0x)" else "EMERGENCY MUTE (KILL FEEDBACK)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
