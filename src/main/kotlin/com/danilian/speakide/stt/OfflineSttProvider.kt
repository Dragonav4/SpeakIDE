package com.danilian.speakide.stt

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val LOG = logger<OfflineSttProvider<*>>()


abstract class OfflineSttProvider<R : Any>(
    protected val modelPath: String
) : SttProvider, Disposable {

    private val mutex = Mutex()
    private var resource: R? = null

    @Volatile
    protected var disposed = false

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
    protected fun getLoadedResource(): R? = resource

    protected fun clearResource() {
        resource = null
    }

    protected abstract fun validatePath(path: String)

    protected abstract suspend fun loadResource(path: String): R

    protected abstract fun releaseResource(resource: R)

    override fun dispose() {
        disposed = true
        resource?.let { releaseResource(it) }
        resource = null
    }
}
