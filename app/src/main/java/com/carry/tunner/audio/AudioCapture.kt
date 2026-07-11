package com.carry.tunner.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 麦克风音频采集
 * 使用 AudioRecord 以 44100Hz 单声道 16bit 采集 PCM 数据
 */
class AudioCapture(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val FRAME_SIZE = 2048 // 每次读取的样本数（减小帧以提升灵敏度）
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /** 是否正在录音 */
    @Volatile
    var isRecording = false
        private set

    /**
     * 检查是否有录音权限
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 开始采集音频数据
     * @param onAudioData 采集到音频数据时的回调，在 IO 线程调用
     * @param onPermissionError 没有权限时的回调
     */
    fun start(
        onAudioData: (FloatArray) -> Unit,
        onPermissionError: () -> Unit = {}
    ) {
        if (isRecording) return

        if (!hasPermission()) {
            onPermissionError()
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                maxOf(bufferSize, FRAME_SIZE * 2)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            captureJob = scope.launch {
                val shortBuffer = ShortArray(FRAME_SIZE)
                val floatBuffer = FloatArray(FRAME_SIZE)

                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(shortBuffer, 0, FRAME_SIZE) ?: -1
                    if (readCount > 0) {
                        // Short 转 Float (-1.0 to 1.0)
                        for (i in 0 until readCount) {
                            floatBuffer[i] = shortBuffer[i] / 32768.0f
                        }
                        val data = if (readCount == FRAME_SIZE) {
                            floatBuffer.copyOf()
                        } else {
                            floatBuffer.copyOf(readCount)
                        }
                        onAudioData(data)
                    }
                }
            }
        } catch (e: SecurityException) {
            onPermissionError()
        }
    }

    /**
     * 停止采集
     */
    fun stop() {
        isRecording = false
        captureJob?.cancel()
        captureJob = null
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
            // 可能已经停止
        }
        audioRecord?.release()
        audioRecord = null
    }
}
