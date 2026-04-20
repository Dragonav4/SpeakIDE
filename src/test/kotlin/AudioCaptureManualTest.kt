import com.danilian.speakide.AudioCapture


fun main() {
    val capture = AudioCapture { chunk ->
    }

    val monitor = Thread({
        while (!Thread.currentThread().isInterrupted) {
            val db = capture.currentDb()
            val bars = "█".repeat(((db + 100) / 2).toInt().coerceIn(0, 40))
            println("%6.1f dB | %s".format(db, bars))
            Thread.sleep(100)
        }
    }, "monitor").apply { isDaemon = true }

    println("Recording 5 seconds... Press Ctrl+C to stop. ")
    capture.start()
    monitor.start()

    Thread.sleep(5000)

    capture.stop()
    println("Ready")
}
