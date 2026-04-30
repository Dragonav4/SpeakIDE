package com.danilian.speakide.audio

import be.tarsos.dsp.resample.Resampler

object AudioResampler {

    private const val TARGET_RATE = 16_000
    private const val SOURCE_RATE = 44_100

    fun resampleTo16kFloat(pcmBytes: ByteArray, sourceRate: Int = SOURCE_RATE): FloatArray {
        if (pcmBytes.isEmpty()) return FloatArray(0)

        val factor = TARGET_RATE.toDouble() / sourceRate

        val numSamples = pcmBytes.size / 2
        val sourceFloats = FloatArray(numSamples) { i ->
            val lo = pcmBytes[i * 2].toInt() and 0xFF
            val hi = pcmBytes[i * 2 + 1].toInt() shl 8
            (lo or hi).toShort() / 32768.0f
        }

        val outputSize = (numSamples * factor).toInt() + 2
        val outputFloats = FloatArray(outputSize)

        val resampler = Resampler(true, factor, factor)
        val result = resampler.process(
            factor,
            sourceFloats, 0, numSamples,
            true,
            outputFloats, 0, outputSize
        )

        return outputFloats.copyOf(result.outputSamplesGenerated)
    }
}
