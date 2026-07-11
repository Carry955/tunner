package com.carry.tunner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carry.tunner.audio.AudioCapture
import com.carry.tunner.audio.PitchDetector
import com.carry.tunner.model.Note
import com.carry.tunner.model.PitchData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 音准器 ViewModel
 * 管理音频采集、音高检测和状态
 */
class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioCapture = AudioCapture(application)
    private val pitchDetector = PitchDetector()

    // 是否正在监听
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // 当前音高数据
    private val _currentPitch = MutableStateFlow<PitchData?>(null)
    val currentPitch: StateFlow<PitchData?> = _currentPitch.asStateFlow()

    // 当前音符
    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    // 音高历史记录（用于钢琴卷帘显示）
    private val _pitchHistory = MutableStateFlow<List<PitchData>>(emptyList())
    val pitchHistory: StateFlow<List<PitchData>> = _pitchHistory.asStateFlow()

    // 噪声门阈值 (0.0 - 1.0)
    private val _noiseGate = MutableStateFlow(0.02f)
    val noiseGate: StateFlow<Float> = _noiseGate.asStateFlow()

    // 录音起始时间（用于相对时间戳）
    private var audioStartTime = 0L

    // 是否暂停显示（音频继续采集，但不更新 UI）
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // 权限错误事件
    private val _permissionError = MutableStateFlow(false)
    val permissionError: StateFlow<Boolean> = _permissionError.asStateFlow()

    // 最大历史记录数（约 30 秒，每帧约 16ms）
    private val maxHistorySize = 30 * 60

    /**
     * 开始监听麦克风
     */
    fun startListening() {
        if (_isListening.value) return

        audioStartTime = System.currentTimeMillis()

        audioCapture.start(
            onAudioData = { audioData ->
                processAudioData(audioData)
            },
            onPermissionError = {
                _permissionError.value = true
            }
        )

        _isListening.value = audioCapture.isRecording
    }

    /**
     * 停止监听
     */
    fun stopListening() {
        audioCapture.stop()
        _isListening.value = false
        _currentPitch.value = null
        _currentNote.value = null
        _pitchHistory.value = emptyList()
    }

    /**
     * 设置噪声门阈值
     */
    fun setNoiseGate(value: Float) {
        _noiseGate.value = value.coerceIn(0f, 1f)
    }

    /**
     * 切换暂停状态
     */
    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    /**
     * 清除权限错误
     */
    fun clearPermissionError() {
        _permissionError.value = false
    }

    private fun processAudioData(audioData: FloatArray) {
        // 计算振幅（RMS）
        val rms = calculateRMS(audioData)

        // 相对时间戳（从录音开始算起）
        val elapsed = System.currentTimeMillis() - audioStartTime

        // 噪声门过滤
        if (rms < _noiseGate.value) {
            if (!_isPaused.value) {
                _currentPitch.value = PitchData(
                    frequency = 0.0,
                    timestamp = elapsed,
                    amplitude = rms.toDouble()
                )
                _currentNote.value = null
            }
            return
        }

        // 检测音高
        val frequency = pitchDetector.detect(audioData) ?: return

        val note = Note.fromFrequency(frequency)
        val pitchData = PitchData(
            frequency = frequency,
            timestamp = elapsed,
            amplitude = rms.toDouble(),
            note = note
        )

        if (!_isPaused.value) {
            _currentPitch.value = pitchData
            _currentNote.value = note

            // 更新历史记录
            val history = _pitchHistory.value.toMutableList()
            history.add(pitchData)
            if (history.size > maxHistorySize) {
                history.removeAt(0)
            }
            _pitchHistory.value = history
        }
    }

    /**
     * 计算 RMS 振幅
     */
    private fun calculateRMS(audioData: FloatArray): Float {
        var sum = 0.0f
        for (sample in audioData) {
            sum += sample * sample
        }
        return kotlin.math.sqrt(sum / audioData.size)
    }

    override fun onCleared() {
        super.onCleared()
        audioCapture.stop()
    }
}
