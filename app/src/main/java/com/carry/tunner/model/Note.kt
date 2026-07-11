package com.carry.tunner.model

/**
 * 音符数据模型
 * @param name 音符名称 (C, D, E, F, G, A, B)
 * @param octave 八度 (0-8)
 * @param frequency 频率 (Hz)
 * @param midiNote MIDI 音符号 (0-127)
 * @param cents 偏离目标音符的音分值 (-50 to +50)
 */
data class Note(
    val name: String,
    val octave: Int,
    val frequency: Double,
    val midiNote: Int,
    val cents: Double
) {
    /** 完整音符名称，如 "C4", "A#3" */
    val fullName: String get() = "$name$octave"

    companion object {
        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        /**
         * 从频率计算音符
         * @param frequency 检测到的频率 (Hz)
         * @return 对应的 Note 对象，如果频率无效返回 null
         */
        fun fromFrequency(frequency: Double): Note? {
            if (frequency <= 0) return null

            // A4 = 440Hz, MIDI note 69
            val midiNote = 12 * Math.log(frequency / 440.0) / Math.log(2.0) + 69
            val roundedMidi = Math.round(midiNote).toInt()

            val cents = (midiNote - roundedMidi) * 100.0

            val noteIndex = ((roundedMidi % 12) + 12) % 12
            val octave = (roundedMidi / 12) - 1

            return Note(
                name = NOTE_NAMES[noteIndex],
                octave = octave,
                frequency = frequency,
                midiNote = roundedMidi,
                cents = cents
            )
        }

        /**
         * 获取所有音符名称（一个八度）
         */
        fun noteNames(): Array<String> = NOTE_NAMES
    }
}
