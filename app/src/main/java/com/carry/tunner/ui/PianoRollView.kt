package com.carry.tunner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.carry.tunner.model.Note
import com.carry.tunner.model.PitchData
import com.carry.tunner.ui.theme.PianoKeyActive
import com.carry.tunner.ui.theme.PianoKeyBorder
import com.carry.tunner.ui.theme.PianoBlackKey
import com.carry.tunner.ui.theme.PianoWhiteKey
import com.carry.tunner.ui.theme.PitchDot
import com.carry.tunner.ui.theme.PitchDotActive
import com.carry.tunner.ui.theme.PitchLine

/**
 * 钢琴卷帘视图
 * 纵轴为音高（对应钢琴键），横轴为时间，实时滚动显示
 */
@Composable
fun PianoRollView(
    pitchHistory: List<PitchData>,
    currentNote: Note?,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    // 缩放比例
    var scaleY by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .background(surfaceColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scaleY = (scaleY * zoom).coerceIn(0.5f, 3f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 钢琴键宽度 - 更窄更精致
            val keyWidth = 48.dp.toPx()

            // 卷帘区域
            val rollWidth = canvasWidth - keyWidth
            val rollLeft = keyWidth

            // 音高范围 (C2 - C6, MIDI 36-84)
            val minMidi = 36
            val maxMidi = 84
            val midiRange = maxMidi - minMidi

            // 每个半音的高度
            val baseNoteHeight = canvasHeight / midiRange
            val noteHeight = baseNoteHeight * scaleY

            // 绘制钢琴键盘
            drawPianoKeys(
                keyWidth = keyWidth,
                canvasHeight = canvasHeight,
                minMidi = minMidi,
                maxMidi = maxMidi,
                noteHeight = noteHeight,
                currentMidi = currentNote?.midiNote,
                onSurfaceColor = onSurfaceColor
            )

            // 绘制网格线
            drawGridLines(
                left = rollLeft,
                width = rollWidth,
                canvasHeight = canvasHeight,
                minMidi = minMidi,
                maxMidi = maxMidi,
                noteHeight = noteHeight,
                outlineColor = outlineColor
            )

            // 绘制音高历史
            drawPitchHistory(
                pitchHistory = pitchHistory,
                left = rollLeft,
                width = rollWidth,
                canvasHeight = canvasHeight,
                minMidi = minMidi,
                noteHeight = noteHeight,
                currentMidi = currentNote?.midiNote
            )
        }
    }
}

/**
 * 绘制钢琴键盘
 */
private fun DrawScope.drawPianoKeys(
    keyWidth: Float,
    canvasHeight: Float,
    minMidi: Int,
    maxMidi: Int,
    noteHeight: Float,
    currentMidi: Int?,
    onSurfaceColor: Color
) {
    val noteNames = Note.noteNames()

    for (midi in minMidi..maxMidi) {
        val noteIndex = ((midi % 12) + 12) % 12
        val octave = (midi / 12) - 1
        val noteName = noteNames[noteIndex]

        // 计算 Y 位置（从底部开始，高音在上）
        val yFromBottom = (midi - minMidi) * noteHeight
        val y = canvasHeight - yFromBottom - noteHeight

        // 判断是否为黑键
        val isBlackKey = noteName.contains("#")
        val isActive = midi == currentMidi

        // 绘制键盘背景
        val keyColor = when {
            isActive -> PianoKeyActive
            isBlackKey -> PianoBlackKey
            else -> PianoWhiteKey
        }

        // 绘制圆角矩形键盘
        drawRoundRect(
            color = keyColor,
            topLeft = Offset(1.dp.toPx(), y + 0.5.dp.toPx()),
            size = Size(keyWidth - 2.dp.toPx(), noteHeight - 1.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // 绘制键盘边框
        drawRoundRect(
            color = PianoKeyBorder,
            topLeft = Offset(1.dp.toPx(), y + 0.5.dp.toPx()),
            size = Size(keyWidth - 2.dp.toPx(), noteHeight - 1.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5.dp.toPx())
        )

        // 绘制音符名称（白键显示全部自然音名）
        if (!isBlackKey) {
            val textColor = if (isActive) Color.White else onSurfaceColor.copy(alpha = 0.7f)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = noteName == "C"
                }
                drawText(
                    "$noteName$octave",
                    keyWidth / 2,
                    y + noteHeight / 2 + 3.dp.toPx(),
                    paint
                )
            }
        }
    }
}

/**
 * 绘制网格线
 */
private fun DrawScope.drawGridLines(
    left: Float,
    width: Float,
    canvasHeight: Float,
    minMidi: Int,
    maxMidi: Int,
    noteHeight: Float,
    outlineColor: Color
) {
    for (midi in minMidi..maxMidi) {
        val yFromBottom = (midi - minMidi) * noteHeight
        val y = canvasHeight - yFromBottom

        val noteIndex = ((midi % 12) + 12) % 12
        val isC = noteIndex == 0

        // C 音的线更粗更明显
        val strokeWidth = if (isC) 1.dp.toPx() else 0.5.dp.toPx()
        val color = if (isC) outlineColor.copy(alpha = 0.4f) else outlineColor

        drawLine(
            color = color,
            start = Offset(left, y),
            end = Offset(left + width, y),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * 绘制音高历史
 */
private fun DrawScope.drawPitchHistory(
    pitchHistory: List<PitchData>,
    left: Float,
    width: Float,
    canvasHeight: Float,
    minMidi: Int,
    noteHeight: Float,
    currentMidi: Int?
) {
    if (pitchHistory.isEmpty()) return

    // 时间戳现在是相对时间（从录音开始算起的毫秒数）
    // 取最后一个点的时间作为"当前时间"
    val now = pitchHistory.last().timestamp
    val timeWindow = 5000L // 5 秒窗口
    val startTime = now - timeWindow

    // 过滤时间窗口内的数据
    val visiblePitches = pitchHistory.filter { it.timestamp >= startTime }
    if (visiblePitches.isEmpty()) return

    // 计算每个像素对应的时间
    val timePerPixel = timeWindow / width

    // 绘制音高块
    var prevX = -1f
    var prevY = -1f

    for (pitch in visiblePitches) {
        val note = pitch.note ?: continue

        // 计算 X 位置
        val timeOffset = now - pitch.timestamp
        val x = left + width - (timeOffset / timePerPixel)

        // 计算 Y 位置
        val midiNote = note.midiNote + note.cents / 100.0
        val yFromBottom = (midiNote - minMidi) * noteHeight
        val y = canvasHeight - yFromBottom.toFloat() - noteHeight / 2

        // 判断是否为当前音符
        val isActive = note.midiNote == currentMidi

        // 绘制连接线 - 更细更精致
        if (prevX >= left && prevY >= 0) {
            drawLine(
                color = PitchLine.copy(alpha = if (isActive) 0.8f else 0.4f),
                start = Offset(prevX, prevY),
                end = Offset(x, y),
                strokeWidth = if (isActive) 2.dp.toPx() else 1.5.dp.toPx()
            )
        }

        // 绘制圆点 - 更大更醒目
        val dotColor = if (isActive) PitchDotActive else PitchDot
        val dotRadius = if (isActive) 5.dp.toPx() else 4.dp.toPx()

        // 绘制光晕效果（当前音符）
        if (isActive) {
            drawCircle(
                color = dotColor.copy(alpha = 0.2f),
                radius = dotRadius * 2,
                center = Offset(x, y)
            )
        }

        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(x, y)
        )

        prevX = x
        prevY = y
    }
}
