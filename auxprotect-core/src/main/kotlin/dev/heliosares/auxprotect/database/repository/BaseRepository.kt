package dev.heliosares.auxprotect.database.repository

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.Database

/**
 * Base repository providing common database operation patterns.
 * All repositories extend this to get transaction support, batch operations, and retry logic.
 */
abstract class BaseRepository(protected val database: Database) {

    companion object {
        /** Default batch size for bulk operations, matching existing behavior. */
        const val DEFAULT_BATCH_SIZE = 128

        /** Maximum number of retries for transient failures. */
        const val MAX_RETRIES = 3

        /** Base delay for exponential backoff in milliseconds. */
        const val BASE_RETRY_DELAY_MS = 100L
    }

    /**
     * Executes a database operation within a suspended transaction.
     */
    protected suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            block()
        }
    }

    /**
     * Executes a database operation with retry logic for transient failures.
     *
     * @param maxRetries maximum number of retry attempts
     * @param block the database operation to execute
     * @return the result of the operation
     */
    protected suspend fun <T> dbQueryWithRetry(
        maxRetries: Int = MAX_RETRIES,
        block: suspend Transaction.() -> T
    ): T {
        var lastException: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                return dbQuery(block)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries && isTransientError(e)) {
                    val delay = BASE_RETRY_DELAY_MS * (1L shl attempt)
                    kotlinx.coroutines.delay(delay)
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("Retry loop completed without result")
    }

    /**
     * Processes items in batches.
     *
     * @param items the items to process
     * @param batchSize the size of each batch
     * @param processor the function to process each batch
     */
    protected suspend fun <T> processBatched(
        items: List<T>,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        processor: suspend Transaction.(List<T>) -> Unit
    ) {
        items.chunked(batchSize).forEach { batch ->
            dbQuery {
                processor(batch)
            }
        }
    }

    /**
     * Determines if an exception represents a transient error that may succeed on retry.
     */
    private fun isTransientError(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: return false
        return message.contains("locked") ||
                message.contains("busy") ||
                message.contains("timeout") ||
                message.contains("deadlock") ||
                message.contains("connection")
    }
}
