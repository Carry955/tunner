package com.carry.tunner.audio

import kotlin.math.abs
import kotlin.math.log
import kotlin.math.round

/**
 * YIN 音高检测算法
 * 基于自相关的音高检测方法，适合人声基频检测
 */
class PitchDetector(
    private val sampleRate: Int = AudioCapture.SAMPLE_RATE,
    private val threshold: Double = 0.15
) {

    /**
     * 检测音频数据的基频
     * @param audioData PCM 音频数据 (float, -1.0 to 1.0)
     * @return 检测到的频率 (Hz)，如果未检测到返回 null
     */
    fun detect(audioData: FloatArray): Double? {
        if (audioData.size < 2) return null

        // 1. 计算差分函数
        val diff = differenceFunction(audioData)

        // 2. 累积均值归一化
        val cmndf = cumulativeMeanNormalizedDifference(diff)

        // 3. 绝对阈值法找第一个谷值
        val tau = absoluteThreshold(cmndf)

        if (tau == -1) return null

        // 4. 抛物线插值提高精度
        val betterTau = parabolicInterpolation(cmndf, tau)

        // 5. 转换为频率
        val frequency = sampleRate / betterTau

        // 过滤不合理的人声频率 (80Hz - 1100Hz)
        return if (frequency in 80.0..1100.0) frequency else null
    }

    /**
     * 计算差分函数
     * d(j) = sum((x[i] - x[i+j])^2) for i = 0..N/2-1
     */
    private fun differenceFunction(audioData: FloatArray): DoubleArray {
        val halfSize = audioData.size / 2
        val diff = DoubleArray(halfSize)

        for (j in 1 until halfSize) {
            var sum = 0.0
            for (i in 0 until halfSize) {
                val delta = (audioData[i] - audioData[i + j]).toDouble()
                sum += delta * delta
            }
            diff[j] = sum
        }
        diff[0] = 1.0 // 避免除零

        return diff
    }

    /**
     * 累积均值归一化差分函数
     * cmndf(j) = d(j) / ((1/j) * sum(d[k], k=1..j))
     */
    private fun cumulativeMeanNormalizedDifference(diff: DoubleArray): DoubleArray {
        val cmndf = DoubleArray(diff.size)
        cmndf[0] = 1.0

        var runningSum = 0.0
        for (j in 1 until diff.size) {
            runningSum += diff[j]
            cmndf[j] = if (runningSum == 0.0) 1.0 else diff[j] * j / runningSum
        }

        return cmndf
    }

    /**
     * 绝对阈值法
     * 找到第一个低于阈值的谷值位置
     */
    private fun absoluteThreshold(cmndf: DoubleArray): Int {
        // 跳过前2个样本（对应太高的频率）
        for (j in 2 until cmndf.size) {
            if (cmndf[j] < threshold) {
                // 找到谷值，继续找最低点
                var minJ = j
                var k = j
                while (k + 1 < cmndf.size && cmndf[k + 1] < cmndf[k]) {
                    k++
                    minJ = k
                }
                return minJ
            }
        }
        return -1
    }

    /**
     * 抛物线插值
     * 使用相邻三个点拟合抛物线，找到更精确的谷值位置
     */
    private fun parabolicInterpolation(cmndf: DoubleArray, tau: Int): Double {
        if (tau < 1 || tau >= cmndf.size - 1) return tau.toDouble()

        val s0 = cmndf[tau - 1]
        val s1 = cmndf[tau]
        val s2 = cmndf[tau + 1]

        val adjustment = (s0 - s2) / (2.0 * (s0 - 2.0 * s1 + s2))

        return tau + adjustment
    }
}
