package com.danilian.speakide.audio

import java.util.concurrent.ConcurrentLinkedQueue


class PcmBuffer {

    private val chunks = ConcurrentLinkedQueue<ByteArray>()

    fun add(chunk: ByteArray) {
        chunks.add(chunk)
    }
    fun drain(): ByteArray {
        val snapshot = generateSequence { chunks.poll() }.toList()
        if (snapshot.isEmpty()) return ByteArray(0)
        val total = snapshot.sumOf { it.size }
        val result = ByteArray(total)
        var pos = 0
        for (chunk in snapshot) {
            chunk.copyInto(result, pos)
            pos += chunk.size
        }
        return result
    }

    fun isEmpty(): Boolean = chunks.isEmpty()
}
