package dev.heliosares.auxprotect.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Provides a dedicated CoroutineDispatcher for database operations.
 * Configurable thread pool size with clean shutdown support.
 */
class DatabaseDispatcher(threadPoolSize: Int = 4) {

    private val executor = Executors.newFixedThreadPool(threadPoolSize) { runnable ->
        Thread(runnable, "AuxProtect-DB").apply {
            isDaemon = true
        }
    }

    /**
     * The coroutine dispatcher for database operations.
     */
    val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()

    /**
     * Shuts down the dispatcher with a timeout, waiting for pending operations to complete.
     *
     * @param timeoutMs maximum time to wait for pending operations in milliseconds
     */
    fun shutdown(timeoutMs: Long = 30_000) {
        executor.shutdown()
        if (!executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow()
        }
    }

    /**
     * Returns whether the dispatcher has been shut down.
     */
    fun isShutdown(): Boolean = executor.isShutdown
}
