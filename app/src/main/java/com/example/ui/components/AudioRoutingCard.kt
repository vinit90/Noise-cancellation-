package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioDeviceItem
import com.example.ui.theme.AmberAntiPhase
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkSurfaceVariant
import com.example.ui.theme.LabDarkVoid
import com.example.ui.theme.VioletMod

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioRoutingCard(
    availableInputs: List<AudioDeviceItem>,
    availableOutputs: List<AudioDeviceItem>,
    selectedInputId: Int?,
    selectedOutputId: Int?,
    isBluetoothConnected: Boolean,
    isFeedbackRiskHigh: Boolean,
    isFeedbackShieldEnabled: Boolean,
    onSelectInput: (Int?) -> Unit,
    onSelectOutput: (Int?) -> Unit,
    onToggleFeedbackShield: (Boolean) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefreshDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LabDarkCard, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isFeedbackRiskHigh) AmberAntiPhase.copy(alpha = 0.5f) else EmeraldActive.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("audio_routing_card")
    ) {
        // --- 1. Card Header with Title and Bluetooth Settings Action ---
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
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isBluetoothConnected) CyanNeon else VioletMod,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "HARDWARE TRANSDUCER ROUTING",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Mic Sensing vs Speaker Projection",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefreshDevices,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("refresh_audio_devices_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh devices",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onOpenBluetoothSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon.copy(alpha = 0.15f),
                        contentColor = CyanNeon
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("open_bluetooth_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PAIR BLE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. Acoustic Feedback / Transducer Physical Isolation Banner ---
        if (isFeedbackRiskHigh) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmberAntiPhase.copy(alpha = 0.12f))
                    .border(1.dp, AmberAntiPhase.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Acoustic feedback warning",
                        tint = AmberAntiPhase,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Column {
                        Text(
                            text = "Acoustic Feedback Risk (Co-Located Mic & Speaker)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AmberAntiPhase
                        )
                        Text(
                            text = "Phone mic is only ~4cm from phone speaker. Speaker sound instantly leaks into mic, causing howling. Switch output to a separate Bluetooth Speaker or plug in headphones for real ANC separation!",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldActive.copy(alpha = 0.12f))
                    .border(1.dp, EmeraldActive.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Transducer isolation active",
                        tint = EmeraldActive,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Transducer Physical Isolation Active",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = EmeraldActive
                        )
                        Text(
                            text = "Phone microphone listens to incoming noise while external speaker projects anti-phase waves without acoustic leakback.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. Output Destination (Where anti-phase audio plays) ---
        Text(
            text = "SPEAKER OUTPUT (CANCELLATION WAVE)",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option for System Default
            DeviceOptionChip(
                label = "System Default",
                icon = Icons.Default.VolumeUp,
                isSelected = selectedOutputId == null,
                isRecommended = false,
                onClick = { onSelectOutput(null) },
                testTag = "output_default_chip"
            )

            // Dynamic output devices found
            availableOutputs.forEach { device ->
                val isSelected = selectedOutputId == device.id
                DeviceOptionChip(
                    label = device.name,
                    icon = if (device.isBluetooth) Icons.Default.BluetoothConnected else if (device.type == 3 || device.type == 4) Icons.Default.Headphones else Icons.Default.Speaker,
                    isSelected = isSelected,
                    isRecommended = device.isBluetooth,
                    badgeText = if (device.isBluetooth) "RECOMMENDED" else null,
                    onClick = { onSelectOutput(device.id) },
                    testTag = "output_device_${device.id}_chip"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. Input Source (Where microphone audio is captured) ---
        Text(
            text = "MICROPHONE INPUT (INCIDENT NOISE SENSING)",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = AmberAntiPhase
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option for System Default
            DeviceOptionChip(
                label = "Phone Built-in Mic",
                icon = Icons.Default.Mic,
                isSelected = selectedInputId == null,
                isRecommended = true,
                badgeText = "SENSE NOISE",
                onClick = { onSelectInput(null) },
                testTag = "input_default_chip"
            )

            availableInputs.filter { it.id != 0 }.forEach { device ->
                val isSelected = selectedInputId == device.id
                DeviceOptionChip(
                    label = device.name,
                    icon = Icons.Default.Mic,
                    isSelected = isSelected,
                    isRecommended = false,
                    onClick = { onSelectInput(device.id) },
                    testTag = "input_device_${device.id}_chip"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 5. Acoustic Feedback Shield Switch ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LabDarkVoid)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isFeedbackShieldEnabled) EmeraldActive else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Acoustic Feedback Shield",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Clamps gain when using phone speaker to prevent ear-piercing squeal",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 13.sp
                    )
                }
            }

            Switch(
                checked = isFeedbackShieldEnabled,
                onCheckedChange = onToggleFeedbackShield,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EmeraldActive,
                    checkedTrackColor = EmeraldActive.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color(0xFF64748B),
                    uncheckedTrackColor = LabDarkSurfaceVariant
                ),
                modifier = Modifier.testTag("feedback_shield_switch")
            )
        }
    }
}

@Composable
private fun DeviceOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isRecommended: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val activeBorder by animateColorAsState(
        targetValue = if (isSelected) CyanNeon else Color(0xFF334155),
        animationSpec = tween(durationMillis = 150),
        label = "chip_border"
    )
    val activeBg by animateColorAsState(
        targetValue = if (isSelected) CyanNeon.copy(alpha = 0.18f) else LabDarkVoid,
        animationSpec = tween(durationMillis = 150),
        label = "chip_bg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(activeBg)
            .border(1.dp, activeBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyanNeon else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
            )
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isRecommended) EmeraldActive.copy(alpha = 0.25f) else VioletMod.copy(alpha = 0.25f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecommended) EmeraldActive else VioletMod
                    )
                }
            }
        }
    }
}
