package dev.heliosares.auxprotect.database.repository

import dev.heliosares.auxprotect.database.schema.InvBlobTable
import dev.heliosares.auxprotect.database.schema.TransactionsBlobTable
import org.jetbrains.exposed.sql.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for blob storage with hash-based deduplication.
 * Replaces BlobManager with Exposed-based operations.
 * Maintains an in-memory cache with TTL for recently used blobs.
 */
class BlobRepository(
    database: Database,
    private val invBlobTable: InvBlobTable,
    private val transactionsBlobTable: TransactionsBlobTable
) : BaseRepository(database) {

    companion object {
        /** Cache expiry time in milliseconds (10 minutes). */
        const val CACHE_EXPIRY_MS = 600_000L

        /** Minimum interval between cache cleanups in milliseconds. */
        const val CLEANUP_INTERVAL_MS = 30_000L
    }

    private data class BlobCacheEntry(
        val blobId: Long,
        val data: ByteArray,
        val hash: Int,
        var lastUsed: Long = System.currentTimeMillis()
    ) {
        fun touch() {
            lastUsed = System.currentTimeMillis()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BlobCacheEntry) return false
            if (hash != other.hash) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = hash
    }

    // Hash → BlobCacheEntry
    private val invBlobCache = ConcurrentHashMap<Int, BlobCacheEntry>()
    private val transactionBlobCache = ConcurrentHashMap<Int, BlobCacheEntry>()
    private var lastCleanup = 0L

    /**
     * Gets or creates a blob ID for the given data in the inventory blob table.
     *
     * @param blob the blob data
     * @param snowflake the snowflake to use as the blob ID if inserting
     * @return the blob ID
     */
    suspend fun getOrCreateInvBlobId(blob: ByteArray?, snowflake: Long): Long {
        return getOrCreateBlobId(invBlobTable, invBlobCache, blob, snowflake)
    }

    /**
     * Gets or creates a blob ID for the given data in the transaction blob table.
     *
     * @param blob the blob data
     * @param snowflake the snowflake to use as the blob ID if inserting
     * @return the blob ID
     */
    suspend fun getOrCreateTransactionBlobId(blob: ByteArray?, snowflake: Long): Long {
        return getOrCreateBlobId(transactionsBlobTable, transactionBlobCache, blob, snowflake)
    }

    /**
     * Retrieves a blob by its ID from the inventory blob table.
     */
    suspend fun getInvBlob(blobId: Long): ByteArray? {
        return getBlob(invBlobTable, blobId)
    }

    /**
     * Retrieves a blob by its ID from the transaction blob table.
     */
    suspend fun getTransactionBlob(blobId: Long): ByteArray? {
        return getBlob(transactionsBlobTable, blobId)
    }

    /**
     * Cleans up expired cache entries.
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        if (now - lastCleanup < CLEANUP_INTERVAL_MS) return
        lastCleanup = now

        cleanupCache(invBlobCache)
        cleanupCache(transactionBlobCache)
    }

    private fun cleanupCache(cache: ConcurrentHashMap<Int, BlobCacheEntry>) {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { now - it.value.lastUsed > CACHE_EXPIRY_MS }
    }

    private suspend fun getOrCreateBlobId(
        table: Table,
        cache: ConcurrentHashMap<Int, BlobCacheEntry>,
        blob: ByteArray?,
        snowflake: Long
    ): Long {
        if (blob == null) return -1L

        val hash = blob.contentHashCode()

        // Check cache first
        cache[hash]?.let { cached ->
            if (cached.data.contentEquals(blob)) {
                cached.touch()
                return cached.blobId
            }
        }

        // Check database
        val existingId = dbQuery {
            val blobidCol = table.columns.first { it.name == "blobid" }
            val hashCol = table.columns.first { it.name == "hash" }
            val ablobCol = table.columns.first { it.name == "ablob" }

            @Suppress("UNCHECKED_CAST")
            table.select(blobidCol, ablobCol)
                .where { (hashCol as Column<Int>) eq hash }
                .orderBy(blobidCol as Column<Long> to SortOrder.DESC)
                .forEach { row ->
                    val otherBytes = row[ablobCol as Column<ExposedBlob>].bytes
                    if (blob.contentEquals(otherBytes)) {
                        return@dbQuery row[blobidCol].toLong()
                    }
                }
            -1L
        }

        val blobId: Long
        if (existingId >= 0) {
            blobId = existingId
        } else {
            blobId = snowflake
            dbQuery {
                val blobidCol = table.columns.first { it.name == "blobid" }
                val ablobCol = table.columns.first { it.name == "ablob" }
                val hashCol = table.columns.first { it.name == "hash" }

                table.insert { stmt ->
                    @Suppress("UNCHECKED_CAST")
                    stmt[blobidCol as Column<Long>] = blobId
                    stmt[ablobCol as Column<ExposedBlob>] = ExposedBlob(blob)
                    stmt[hashCol as Column<Int>] = hash
                }
            }
        }

        if (blobId > 0) {
            cache[hash] = BlobCacheEntry(blobId, blob, hash)
        }

        return blobId
    }

    private suspend fun getBlob(table: Table, blobId: Long): ByteArray? {
        if (blobId <= 0) return null
        return dbQuery {
            val blobidCol = table.columns.first { it.name == "blobid" }
            val ablobCol = table.columns.first { it.name == "ablob" }

            @Suppress("UNCHECKED_CAST")
            table.select(ablobCol)
                .where { (blobidCol as Column<Long>) eq blobId }
                .firstOrNull()
                ?.let { it[ablobCol as Column<ExposedBlob>].bytes }
        }
    }
}
