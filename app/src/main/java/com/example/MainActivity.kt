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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.DelayAlignmentCard
import com.example.ui.components.EngineStatusBar
import com.example.ui.components.GainControlCard
import com.example.ui.components.OscilloscopeView
import com.example.ui.components.PhaseControlCard
import com.example.ui.components.TestToneCard
import com.example.ui.components.VuMeterView
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LabDarkBorder
import com.example.ui.theme.LabDarkCard
import com.example.ui.theme.LabDarkSurface
import com.example.ui.theme.LabDarkVoid
import com.example.ui.theme.MyApplicationTheme

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
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isTestToneEnabled by viewModel.isTestToneEnabled.collectAsStateWithLifecycle()
    val testToneFreq by viewModel.testToneFrequency.collectAsStateWithLifecycle()

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

    // Observe error state
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
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Acoustic Phase Inversion & ANC Lab",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Big Start / Stop Engine Button
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

            // 2. Hardware stream telemetry bar
            item {
                EngineStatusBar(
                    activeSource = telemetry.activeSource,
                    bufferSizeFrames = telemetry.bufferSizeFrames,
                    estimatedLatencyMs = telemetry.estimatedHardwareLatencyMs,
                    isRunning = isRunning
                )
            }

            // 3. Real-Time Oscilloscope Waveform Canvas
            item {
                OscilloscopeView(
                    inputWaveform = telemetry.inputWaveform,
                    outputWaveform = telemetry.outputWaveform,
                    isPhaseInverted = isPhaseInverted,
                    isLimiting = telemetry.isLimitingActive
                )
            }

            // 4. Live VU Level Meters
            item {
                VuMeterView(
                    inputDbfs = telemetry.inputDbfs,
                    outputDbfs = telemetry.outputDbfs,
                    inputRms = telemetry.inputRms,
                    outputRms = telemetry.outputRms
                )
            }

            // 5. Phase Inversion Polarity Switch Card
            item {
                PhaseControlCard(
                    isPhaseInverted = isPhaseInverted,
                    onTogglePhase = { viewModel.setPhaseInverted(it) }
                )
            }

            // 6. Delay Line / Phase Alignment Ring Buffer Card
            item {
                DelayAlignmentCard(
                    delayMs = delayMs,
                    onDelayChanged = { viewModel.setDelayMs(it) }
                )
            }

            // 7. Gain & Feedback Soft-Limiter Card (with Emergency Mute)
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

            // 8. Calibration Sine Tone Generator Card
            item {
                TestToneCard(
                    isTestToneEnabled = isTestToneEnabled,
                    testToneFreq = testToneFreq,
                    onToggleTestTone = { viewModel.toggleTestTone() },
                    onSelectFreq = { viewModel.setTestToneFrequency(it) }
                )
            }

            // Bottom safety footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LabDarkCard)
                        .border(1.dp, LabDarkBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🔬 Acoustic Lab Tip: For clearest cancellation observation, start with Gain at 0.5x, enable 440 Hz Test Tone or play a constant tone near the microphone, and slowly increment Delay in 1ms steps to find spatial null interference nodes.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showInfoDialog) {
        AcousticLabInfoDialog(onDismiss = { showInfoDialog = false })
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
        targetValue = 1.03f,
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
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(if (isRunning) 1.0f else pulseScale)
            .testTag("engine_toggle_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Stop Engine" else "Start Engine",
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (isRunning) "HALT AUDIO DSP ENGINE" else "ENGAGE AUDIO DSP ENGINE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
