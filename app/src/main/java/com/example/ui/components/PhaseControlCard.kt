package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAntiPhase
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkSurfaceVariant
import com.example.ui.theme.LabDarkVoid

@Composable
fun PhaseControlCard(
    isPhaseInverted: Boolean,
    onTogglePhase: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor by animateColorAsState(
        targetValue = if (isPhaseInverted) AmberAntiPhase else CyanNeon,
        animationSpec = tween(durationMillis = 200),
        label = "phase_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isPhaseInverted) AmberAntiPhase.copy(alpha = 0.4f) else LabDarkBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .testTag("phase_control_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACOUSTIC PHASE POLARITY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isPhaseInverted) "180° Anti-Phase (Inverted Polarity)" else "0° Passthrough (In-Phase)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = activeColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // M3 Switch
            Switch(
                checked = isPhaseInverted,
                onCheckedChange = onTogglePhase,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LabDarkVoid,
                    checkedTrackColor = AmberAntiPhase,
                    uncheckedThumbColor = CyanNeon,
                    uncheckedTrackColor = LabDarkSurfaceVariant
                ),
                modifier = Modifier.testTag("invert_phase_switch")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Polarity explanation pill / schematic indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LabDarkVoid)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPhaseInverted) "Ø" else "+",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = activeColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPhaseInverted) {
                        "DSP: sample = -sample (Multiplied by -1.0x)"
                    } else {
                        "DSP: sample = sample (Passthrough with 0° shift)"
                    },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPhaseInverted) {
                        "Causes destructive interference when colliding with original acoustic sound wave."
                    } else {
                        "Produces constructive interference (adds to ambient sound pressure level)."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
