package com.example.pomodoro

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.ceil

enum class Mode(val label: String, val minutes: Int) {
    Focus("Focus", 25),
    Short("Short break", 5),
    Long("Long break", 15)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PomodoroApp()
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Suppress("DEPRECATION")
private fun buzz(context: Context) {
    (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(600L)
}

@Composable
fun PomodoroApp() {
    val context = LocalContext.current
    val view = LocalView.current

    var mode by remember { mutableStateOf(Mode.Focus) }
    var totalMs by remember { mutableLongStateOf(Mode.Focus.minutes * 60_000L) }
    var remainingMs by remember { mutableLongStateOf(Mode.Focus.minutes * 60_000L) }
    var running by remember { mutableStateOf(false) }
    var sessions by remember { mutableIntStateOf(0) }

    fun switchMode(m: Mode) {
        mode = m
        totalMs = m.minutes * 60_000L
        remainingMs = totalMs
        running = false
    }

    fun nextMode() {
        if (mode == Mode.Focus) {
            sessions += 1
            switchMode(if (sessions % 4 == 0) Mode.Long else Mode.Short)
        } else {
            switchMode(Mode.Focus)
        }
    }

    LaunchedEffect(running, mode) {
        if (running) {
            val end = System.currentTimeMillis() + remainingMs
            while (running) {
                delay(100)
                remainingMs = (end - System.currentTimeMillis()).coerceAtLeast(0L)
                if (remainingMs == 0L) {
                    buzz(context)
                    nextMode()
                    break
                }
            }
        }
    }

    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    val progress = 1f - remainingMs.toFloat() / totalMs.toFloat()
    val secs = ceil(remainingMs / 1000.0).toLong()
    val timeText = "%02d:%02d".format(secs / 60, secs % 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { switchMode(m) },
                    label = { Text(m.label) }
                )
            }
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(290.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 14.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeText,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { switchMode(mode) },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset", modifier = Modifier.size(28.dp))
            }
            FilledIconButton(
                onClick = { running = !running },
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(32.dp)
            ) {
                Icon(
                    imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (running) "Pause" else "Start",
                    modifier = Modifier.size(44.dp)
                )
            }
            FilledTonalIconButton(
                onClick = { nextMode() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Skip", modifier = Modifier.size(28.dp))
            }
        }

        Text(
            text = "Sessions completed: $sessions",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
