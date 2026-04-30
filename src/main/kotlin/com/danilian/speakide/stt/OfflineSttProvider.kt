package com.danilian.speakide.stt

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val LOG = logger<OfflineSttProvider<*>>()

/**
 * Abstract base for offline STT providers that need to load a model from disk on first use.
 *
 * ### Lazy loading
 * [getOrLoad] loads [R] once and caches it. Subsequent calls return the cached instance
 * immediately without taking the mutex, so the hot path is lock-free.
 *
 * ### Thread safety
 * [dispose] acquires the same mutex as [getOrLoad], so the resource is never freed while
 * a load is in progress (and vice versa). Subclasses must not touch the resource directly —
 * they only implement [loadResource] and [releaseResource].
 *
 * ### Adding a new provider
 * 1. Extend this class with `R` = the native model type.
 * 2. Implement [validatePath], [loadResource], and [releaseResource].
 * 3. Call [getOrLoad] inside your [transcribe] override.
 */
abstract class OfflineSttProvider<R : Any>(
    protected val modelPath: String
) : SttProvider, Disposable {

    private val mutex = Mutex()
    private var resource: R? = null

    @Volatile
    private var disposed = false

    protected suspend fun getOrLoad(): R {
        resource?.let { return it }

        return mutex.withLock {
            resource?.let { return@withLock it }

            if (disposed) error("$displayName is already disposed")

            val path = modelPath.ifBlank {
                throw IllegalArgumentException(
                    "$displayName model path is not set. " +
                            "Set it in Settings → Tools → SpeakIDE."
                )
            }

            validatePath(path)

            LOG.info("$displayName: loading model from $path")
            val loaded = withContext(Dispatchers.IO) { loadResource(path) }
            LOG.info("$displayName: model loaded successfully")

            resource = loaded
            loaded
        }
    }

    protected abstract fun validatePath(path: String)

    protected abstract suspend fun loadResource(path: String): R

    protected abstract fun releaseResource(resource: R)

    override fun dispose() {
        disposed = true
        val toRelease = runBlocking {
            mutex.withLock {
                val r = resource
                resource = null
                r
            }
        }
        toRelease?.let {
            runCatching { releaseResource(it) }
                .onFailure { ex -> LOG.warn("$displayName: error during dispose", ex) }
        }
    }
}
