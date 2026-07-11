package com.carry.tunner.model

/**
 * 音高数据点
 * @param frequency 检测到的频率 (Hz)
 * @param timestamp 时间戳 (ms)
 * @param amplitude 振幅 (0.0 - 1.0)
 * @param note 对应的音符（如果检测到有效音高）
 */
data class PitchData(
    val frequency: Double,
    val timestamp: Long,
    val amplitude: Double,
    val note: Note? = null
)
