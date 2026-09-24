package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkVoid
import java.util.Locale

@Composable
fun EngineStatusBar(
    activeSource: String,
    bufferSizeFrames: Int,
    estimatedLatencyMs: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LabDarkVoid)
            .border(1.dp, LabDarkBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("engine_status_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isRunning) EmeraldActive else Color(0xFF64748B), CircleShape)
            )
            Text(
                text = if (isRunning) "ENGINE RUNNING" else "ENGINE STANDBY",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (isRunning) EmeraldActive else Color(0xFF94A3B8)
            )
        }

        Text(
            text = "SRC: $activeSource",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = CyanNeon
        )

        Text(
            text = "${bufferSizeFrames}f | 48kHz",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
        )

        Text(
            text = String.format(Locale.US, "~%.1fms", estimatedLatencyMs),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E1)
        )
    }
}
