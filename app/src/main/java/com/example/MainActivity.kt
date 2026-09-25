package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioViewModel
import com.example.audio.EngineStatus
import com.example.ui.components.AcousticLabInfoDialog
import com.example.ui.components.AudioRoutingCard
import com.example.ui.components.DelayAlignmentCard
import com.example.ui.components.EngineStatusBar
import com.example.ui.components.GainControlCard
import com.example.ui.components.OscilloscopeView
import com.example.ui.components.PhaseControlCard
import com.example.ui.components.TestToneCard
import com.example.ui.components.VuMeterView
import com.example.ui.theme.AmberAntiPhase
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkSurface
import com.example.ui.theme.LabDarkVoid
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VioletMod

class MainActivity : ComponentActivity() {

    private val viewModel: AudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PhaseLabScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Ensure microphone and audio track stop when app is minimized or backgrounded
        viewModel.onAppBackgrounded()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseLabScreen(viewModel: AudioViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val gain by viewModel.gain.collectAsStateWithLifecycle()
    val isPhaseInverted by viewModel.isPhaseInverted.collectAsStateWithLifecycle()
    val delayMs by viewModel.delayMs.collectAsStateWithLifecycle()
    val isLimiterEnabled by viewModel.isLimiterEnabled.collectAsStateWithLifecycle()
    val isFeedbackShieldEnabled by viewModel.isFeedbackShieldEnabled.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isTestToneEnabled by viewModel.isTestToneEnabled.collectAsStateWithLifecycle()
    val testToneFreq by viewModel.testToneFrequency.collectAsStateWithLifecycle()

    // Hardware Audio Routing & BLE states
    val availableInputs by viewModel.availableInputs.collectAsStateWithLifecycle()
    val availableOutputs by viewModel.availableOutputs.collectAsStateWithLifecycle()
    val selectedInputId by viewModel.selectedInputId.collectAsStateWithLifecycle()
    val selectedOutputId by viewModel.selectedOutputId.collectAsStateWithLifecycle()
    val isBluetoothConnected by viewModel.isBluetoothOutputConnected.collectAsStateWithLifecycle()
    val isFeedbackRiskHigh by viewModel.isFeedbackRiskHigh.collectAsStateWithLifecycle()

    // Active Tab (0: Scope & Meters, 1: BLE & Routing, 2: Phase & DSP, 3: All Controls)
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Automatic shutdown when app is minimized / lifecycle backgrounded
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startEngine()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is strictly required for real-time acoustic phase inversion.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(engineStatus) {
        if (engineStatus is EngineStatus.Error) {
            snackbarHostState.showSnackbar((engineStatus as EngineStatus.Error).message)
        }
    }

    val isRunning = engineStatus is EngineStatus.Running

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LabDarkVoid,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isRunning) EmeraldActive else Color(0xFF64748B), CircleShape)
                        )
                        Column {
                            Text(
                                text = "PHASELAB // DSP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Acoustic Phase Inversion & ANC Lab",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.5.sp,
                                color = CyanNeon
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("info_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Acoustic Lab Info",
                            tint = CyanNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LabDarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Top Navigation Tabs (Fits neatly on any screen size) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LabDarkSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabPill(
                    title = "SCOPE",
                    icon = Icons.Default.GraphicEq,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                    testTag = "tab_scope"
                )
                TabPill(
                    title = "BLE & ROUTE",
                    icon = Icons.Default.Bluetooth,
                    isSelected = selectedTab == 1,
                    hasAlertBadge = isFeedbackRiskHigh,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                    testTag = "tab_routing"
                )
                TabPill(
                    title = "PHASE/DSP",
                    icon = Icons.Default.Tune,
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1f),
                    testTag = "tab_dsp"
                )
                TabPill(
                    title = "ALL",
                    icon = Icons.Default.ViewAgenda,
                    isSelected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    modifier = Modifier.weight(0.8f),
                    testTag = "tab_all"
                )
            }

            // --- Real-time Audio Routing Summary Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LabDarkCard)
                    .border(0.5.dp, LabDarkBorder)
                    .clickable { selectedTab = 1 }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = if (telemetry.isBluetoothOutput) Icons.Default.BluetoothConnected else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (telemetry.isBluetoothOutput) CyanNeon else AmberAntiPhase,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "MIC: ${telemetry.selectedInputName} ➔ OUT: ${telemetry.selectedOutputName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1
                    )
                }

                if (isFeedbackRiskHigh) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AmberAntiPhase.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CO-LOCATED (TAP TO ROUTE)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = AmberAntiPhase
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldActive.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ISOLATED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = EmeraldActive
                        )
                    }
                }
            }

            // --- Scrollable Card Content (Centered on wide displays) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 620.dp)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Always show Big Engine Toggle at the top for easy 1-tap engagement
                    item {
                        BigEngineActionButton(
                            isRunning = isRunning,
                            onToggle = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (isRunning) {
                                    viewModel.stopEngine()
                                } else {
                                    if (hasPermission) {
                                        viewModel.startEngine()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        )
                    }

                    // TAB 0: SCOPE & METERS
                    if (selectedTab == 0 || selectedTab == 3) {
                        item {
                            OscilloscopeView(
                                inputWaveform = telemetry.inputWaveform,
                                outputWaveform = telemetry.outputWaveform,
                                isPhaseInverted = isPhaseInverted,
                                isLimiting = telemetry.isLimitingActive,
                                peakToPeak = telemetry.peakToPeak,
                                estimatedFreqHz = telemetry.estimatedFreqHz
                            )
                        }

                        item {
                            VuMeterView(
                                inputDbfs = telemetry.inputDbfs,
                                outputDbfs = telemetry.outputDbfs,
                                inputRms = telemetry.inputRms,
                                outputRms = telemetry.outputRms
                            )
                        }

                        item {
                            EngineStatusBar(
                                activeSource = telemetry.activeSource,
                                bufferSizeFrames = telemetry.bufferSizeFrames,
                                estimatedLatencyMs = telemetry.estimatedHardwareLatencyMs,
                                isRunning = isRunning
                            )
                        }
                    }

                    // TAB 1: BLE SPEAKER & TRANSDUCER ROUTING
                    if (selectedTab == 1 || selectedTab == 3) {
                        item {
                            AudioRoutingCard(
                                availableInputs = availableInputs,
                                availableOutputs = availableOutputs,
                                selectedInputId = selectedInputId,
                                selectedOutputId = selectedOutputId,
                                isBluetoothConnected = isBluetoothConnected,
                                isFeedbackRiskHigh = isFeedbackRiskHigh,
                                isFeedbackShieldEnabled = isFeedbackShieldEnabled,
                                onSelectInput = { viewModel.selectInputDevice(it) },
                                onSelectOutput = { viewModel.selectOutputDevice(it) },
                                onToggleFeedbackShield = { viewModel.setFeedbackShieldEnabled(it) },
                                onOpenBluetoothSettings = { viewModel.openBluetoothSettings() },
                                onRefreshDevices = { viewModel.refreshAudioDevices() }
                            )
                        }
                    }

                    // TAB 2: PHASE & DSP ALIGNMENT
                    if (selectedTab == 2 || selectedTab == 3) {
                        item {
                            PhaseControlCard(
                                isPhaseInverted = isPhaseInverted,
                                onTogglePhase = { viewModel.setPhaseInverted(it) }
                            )
                        }

                        item {
                            DelayAlignmentCard(
                                delayMs = delayMs,
                                onDelayChanged = { viewModel.setDelayMs(it) }
                            )
                        }

                        item {
                            GainControlCard(
                                gain = gain,
                                isMuted = isMuted,
                                isLimiterEnabled = isLimiterEnabled,
                                isLimitingActive = telemetry.isLimitingActive,
                                onGainChanged = { viewModel.setGain(it) },
                                onToggleLimiter = { viewModel.setLimiterEnabled(it) },
                                onEmergencyMute = { viewModel.emergencyMute() },
                                onUnmute = { viewModel.unmute() }
                            )
                        }

                        item {
                            TestToneCard(
                                isTestToneEnabled = isTestToneEnabled,
                                testToneFreq = testToneFreq,
                                onToggleTestTone = { viewModel.toggleTestTone() },
                                onSelectFreq = { viewModel.setTestToneFrequency(it) }
                            )
                        }
                    }

                    // Lab Tips Footer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LabDarkCard)
                                .border(1.dp, LabDarkBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🔬 PhaseLab Tip: Keeping the microphone on your phone while sending the anti-phase output to an external Bluetooth speaker physically breaks the co-located acoustic feedback loop for clean spatial active noise cancellation.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AcousticLabInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

@Composable
private fun TabPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    hasAlertBadge: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val activeBg by animateColorAsState(
        targetValue = if (isSelected) CyanNeon.copy(alpha = 0.2f) else LabDarkVoid,
        animationSpec = tween(durationMillis = 150),
        label = "tab_bg"
    )
    val activeBorder by animateColorAsState(
        targetValue = if (isSelected) CyanNeon else Color(0xFF334155),
        animationSpec = tween(durationMillis = 150),
        label = "tab_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(activeBg)
            .border(1.dp, activeBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyanNeon else Color(0xFF94A3B8),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF94A3B8)
            )
            if (hasAlertBadge && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AmberAntiPhase, CircleShape)
                )
            }
        }
    }
}

@Composable
fun BigEngineActionButton(
    isRunning: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isRunning) CrimsonAlert else EmeraldActive,
        animationSpec = tween(durationMillis = 200),
        label = "btn_color"
    )

    Button(
        onClick = onToggle,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(if (isRunning) 1.0f else pulseScale)
            .testTag("engine_toggle_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Stop Engine" else "Start Engine",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isRunning) "HALT DSP ENGINE" else "ENGAGE DSP ENGINE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
