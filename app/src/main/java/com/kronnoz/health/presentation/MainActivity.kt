package com.kronnoz.health.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.kronnoz.health.presentation.theme.HealthTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = HeartRateRepository(applicationContext)

        setContent {
            val vm: HeartRateViewModel = viewModel(factory = HeartRateViewModel.Factory(repo))
            val bpm by vm.heartRate.collectAsState()
            val available by vm.available.collectAsState()
            val permission = rememberPermissionState(android.Manifest.permission.BODY_SENSORS)
            var isLoading by remember { mutableStateOf(false) }

            // Esperar hasta que bpm > 0
            LaunchedEffect(isLoading, bpm) {
                if (isLoading && bpm > 0) {
                    isLoading = false
                }
            }

            WearApp(
                bpm = bpm,
                available = available,
                isLoading = isLoading,
                onClick = {
                    if (permission.status.isGranted) {
                        isLoading = true
                        vm.toggle()
                    } else {
                        permission.launchPermissionRequest()
                    }
                }
            )
        }
    }
}

@Composable
fun WearApp(
    bpm: Double,
    available: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (available) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeat"
    )

    val gradientColors = listOf(
        Color(0xFF121212),
        Color(0xFF1E1E1E)
    )

    HealthTheme {
        Scaffold(
            timeText = {
                TimeText(
                    modifier = Modifier.padding(top = 4.dp),
                    timeTextStyle = MaterialTheme.typography.caption2.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp
                    )
                )
            },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = {
                if (available) {
                    PositionIndicator(scalingLazyListState = ScalingLazyListState())
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(gradientColors))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = ScalingLazyListState()
                ) {
                    item {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart icon",
                            tint = if (available) Color(0xFFE91E63) else Color(0xFFB0BEC5),
                            modifier = Modifier
                                .size(48.dp)
                                .scale(scale)
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    item {
                        Text(
                            text = if (available) "${bpm.toInt()} BPM" else "Tap to Start",
                            style = MaterialTheme.typography.title1.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    item {
                        Text(
                            text = if (available) "Heart Rate" else "Awaiting...",
                            style = MaterialTheme.typography.caption2.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    item {
                        if (isLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(4.dp),
                                    indicatorColor = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Obteniendo datos...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Chip(
                                onClick = onClick,
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = if (available) Color(0xFFE91E63) else Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(36.dp),
                                label = {
                                    Text(
                                        text = if (available) "Stop" else "Start",
                                        style = MaterialTheme.typography.button.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    showBackground = true
)
@Composable
fun WearAppPreview() {
    WearApp(
        bpm = 76.0,
        available = true,
        isLoading = false,
        onClick = {}
    )
}

