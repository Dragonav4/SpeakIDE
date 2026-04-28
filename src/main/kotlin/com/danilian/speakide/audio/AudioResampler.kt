package com.danilian.speakide.audio

object AudioResampler {
    /**
     * Resamples 16-bit little-endian PCM bytes to a FloatArray normalized between -1.0 and 1.0,
     * while changing the sample rate.
     */
    fun resampleTo16kFloat(pcmBytes: ByteArray, sourceRate: Int = 44100): FloatArray {
        val targetRate = 16000
        val numSamples = pcmBytes.size / 2
        val duration = numSamples.toDouble() / sourceRate
        val targetSamples = (duration * targetRate).toInt()
        val result = FloatArray(targetSamples)
        
        val sourceFloats = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            val low = pcmBytes[i * 2].toInt() and 0xFF
            val high = pcmBytes[i * 2 + 1].toInt() shl 8
            val sampleInt = (low or high).toShort()
            sourceFloats[i] = sampleInt / 32768.0f
        }
        
        for (i in 0 until targetSamples) {
            val sourceIndex = i.toDouble() * sourceRate / targetRate
            val index1 = sourceIndex.toInt()
            val index2 = (index1 + 1).coerceAtMost(numSamples - 1)
            val fraction = (sourceIndex - index1).toFloat()
            
            result[i] = sourceFloats[index1] * (1.0f - fraction) + sourceFloats[index2] * fraction
        }
        
        return result
    }
}
