package com.danilian.speakide

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import java.util.concurrent.atomic.AtomicBoolean

class AudioCapture {

    private val format = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        16_000f,
        16,
        1,
        2,
        16_000f,
        false
    )

    private var line: TargetDataLine? = null
    private val running = AtomicBoolean(false)

    fun start(onData: (ByteArray) -> Unit) {
        val info = DataLine.Info(TargetDataLine::class.java, format)

        check(AudioSystem.isLineSupported(info)) {
            "Microphone is unavailable or not supported: $info"
        }

        val dataLine = (AudioSystem.getLine(info) as TargetDataLine).also {
            it.open(format)
            it.start()
            line = it
        }

        running.set(true)

        Thread({
            val buffer = ByteArray(4096)
            try {
                while (running.get()) {
                    val bytesRead = dataLine.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        onData(buffer.copyOf(bytesRead))
                    }
                }
            } finally {
                dataLine.stop()
                dataLine.close()
            }
        }, "speakide-audio-capture").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        line?.stop()
    }
}
