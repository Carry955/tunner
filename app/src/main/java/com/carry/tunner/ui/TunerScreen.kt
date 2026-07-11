package com.carry.tunner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carry.tunner.model.Note
import com.carry.tunner.ui.theme.Green
import com.carry.tunner.ui.theme.Amber
import com.carry.tunner.ui.theme.Red
import com.carry.tunner.viewmodel.TunerViewModel
import kotlin.math.abs

/**
 * 音准器主界面
 */
@Composable
fun TunerScreen(
    viewModel: TunerViewModel,
    modifier: Modifier = Modifier
) {
    val isListening by viewModel.isListening.collectAsState()
    val currentNote by viewModel.currentNote.collectAsState()
    val pitchHistory by viewModel.pitchHistory.collectAsState()
    val noiseGate by viewModel.noiseGate.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部：当前音符显示（缩小高度）
        NoteDisplay(
            note = currentNote,
            isListening = isListening,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 中间：钢琴卷帘
        PianoRollView(
            pitchHistory = pitchHistory,
            currentNote = currentNote,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // 底部：控制栏
        ControlBar(
            isListening = isListening,
            isPaused = isPaused,
            noiseGate = noiseGate,
            onStartStop = {
                if (isListening) {
                    viewModel.stopListening()
                } else {
                    viewModel.startListening()
                }
            },
            onPauseResume = { viewModel.togglePause() },
            onNoiseGateChange = { viewModel.setNoiseGate(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/**
 * 音符显示区域（精简版）
 */
@Composable
private fun NoteDisplay(
    note: Note?,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isListening) {
            Text(
                text = "点击麦克风开始",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        } else if (note == null) {
            Text(
                text = "等待声音...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        } else {
            // 音符名称 + 八度（一行显示）
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = note.name,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${note.octave}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                )
            }

            // 音分偏差 + 频率（一行显示）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CentsIndicator(cents = note.cents)

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "%.1f Hz".format(note.frequency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * 音分偏差指示器（精简版）
 */
@Composable
private fun CentsIndicator(
    cents: Double,
    modifier: Modifier = Modifier
) {
    val color = when {
        abs(cents) < 5 -> Green
        abs(cents) < 15 -> Amber
        else -> Red
    }

    val text = when {
        cents > 0 -> "+${cents.toInt()}¢"
        cents < 0 -> "${cents.toInt()}¢"
        else -> "准确"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.SemiBold
    )
}

/**
 * 底部控制栏
 */
@Composable
private fun ControlBar(
    isListening: Boolean,
    isPaused: Boolean,
    noiseGate: Float,
    onStartStop: () -> Unit,
    onPauseResume: () -> Unit,
    onNoiseGateChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 噪声门滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "噪声门",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.width(56.dp)
            )

            Slider(
                value = noiseGate,
                onValueChange = onNoiseGateChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Text(
                text = "%.0f%%".format(noiseGate * 100),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 控制按钮 - 居中且平衡
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 开始/停止按钮（主按钮）
            IconButton(
                onClick = onStartStop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) Red else MaterialTheme.colorScheme.primary
                    )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "停止" else "开始",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // 暂停/继续按钮（次按钮）
            if (isListening) {
                IconButton(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "继续" else "暂停",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
