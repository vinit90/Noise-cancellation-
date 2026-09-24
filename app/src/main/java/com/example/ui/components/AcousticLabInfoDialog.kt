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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AmberAntiPhase
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkVoid

@Composable
fun AcousticLabInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LabDarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LabDarkBorder, RoundedCornerShape(20.dp))
                .testTag("acoustic_lab_info_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
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
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "ACOUSTIC LAB MANUAL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Principle 1: Phase Inversion & Destructive Interference
                SectionHeader("1. Destructive Wave Interference")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LabDarkVoid)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "A·sin(ωt) + A·sin(ωt + π) = 0\n\nsample_out = -1.0 × sample_in",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAntiPhase
                    )
                }
                Text(
                    text = "When an anti-phase sound wave (shifted by 180° / inverted polarity) collides with the incident acoustic wave, the sound pressure variations cancel each other out, reducing perceived sound pressure level.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Principle 2: Acoustic Path Propagation Delay
                SectionHeader("2. Propagation Delay & Distance")
                Text(
                    text = "Sound travels through air at approx. 343 m/s (34.3 cm per millisecond at 20°C). Because the mic and speaker are physically displaced, the inverted wave must be timed to coincide with the arriving wave front. Use the Ring Buffer slider to align phase delays.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Principle 3: Open Air vs Headphones
                SectionHeader("3. Open Air vs Closed Headphones")
                Text(
                    text = "Commercial ANC (e.g. Sony, Bose, Apple) operates inside an acoustically sealed ear cup where distance is under 1 cm and ambient noise is 1-dimensional. On an open phone, sound radiates omnidirectionally in 3D space, meaning cancellation occurs at localized spatial null nodes.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Principle 4: Safety & Feedback
                SectionHeader("4. Feedback Squeal Protection")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CrimsonAlert.copy(alpha = 0.15f))
                        .border(1.dp, CrimsonAlert.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = CrimsonAlert,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Because the phone's speaker is next to its microphone, positive acoustic feedback (howling squeal) can occur if gain exceeds loop attenuation. The built-in tanh soft-limiter clamps runaway oscillations. Use the Emergency Mute button immediately if squealing begins.",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = CyanNeon,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
