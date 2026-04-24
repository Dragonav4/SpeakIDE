import com.danilian.speakide.AudioCapture


fun main() {
    var capture: AudioCapture? = null
    capture = AudioCapture(
        silenceThresholdDb = -50.0,
        silenceDurationMs = 2000L,
        onSilenceTimeout = {
            Thread { capture?.stop() }.start()
        },
        onData = { },
        onError = { e ->
            println("ERROR: ${e.message}")
            capture?.stop()
        }
    )

    val monitor = Thread({
        while (!Thread.currentThread().isInterrupted) {
            val db = capture.currentDb()
            val bars = "█".repeat(((db + 100) / 2).toInt().coerceIn(0, 40))
            println("%6.1f dB | %s".format(db, bars))
            Thread.sleep(100)
        }
    }, "monitor").apply { isDaemon = true }


    println("Recording...")
    capture.start()
    monitor.start()

    Thread.sleep(15_000)

    capture.stop()
    println("Ready")
}
