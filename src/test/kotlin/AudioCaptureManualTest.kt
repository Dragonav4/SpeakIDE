import com.danilian.speakide.AudioCapture


fun main() {
    val capture = AudioCapture()

    capture.start { chunk ->
        val rms = calculateRms(chunk)
        val bars = "█".repeat((rms / 100).toInt().coerceIn(0, 40))
        println(bars)
    }

    println("Recording 5sec...")
    Thread.sleep(5000)

    capture.stop()
    println("Ready")
}

fun calculateRms(pcm: ByteArray): Double {
    var sum = 0.0
    for (i in pcm.indices step 2) {
        val sample = (pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)
        sum += sample * sample
    }
    return Math.sqrt(sum / (pcm.size / 2))
}
