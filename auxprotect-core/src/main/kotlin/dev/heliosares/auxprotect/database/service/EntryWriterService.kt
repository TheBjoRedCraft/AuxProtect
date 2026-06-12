package dev.heliosares.auxprotect.database.service

import dev.heliosares.auxprotect.core.IAuxProtect
import dev.heliosares.auxprotect.database.DbEntry
import dev.heliosares.auxprotect.database.Table
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

/**
 * Coroutine-based queue processing service for writing entries to the database.
 * Replaces DatabaseRunnable with a non-blocking, coroutine-driven approach.
 *
 * Features:
 * - Configurable flush interval and batch size
 * - Listener support for entry events
 * - Graceful shutdown with queue drain
 */
class EntryWriterService(
    private val plugin: IAuxProtect,
    private val databaseService: DatabaseService,
    private val flushIntervalMs: Long = 1000L,
    private val batchSize: Int = 128
) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("AuxProtect-EntryWriter")
    )

    private val listeners = mutableSetOf<Consumer<DbEntry>>()
    private val listenerLock = Any()

    @Volatile
    private var running = false

    private var writerJob: Job? = null

    /**
     * Starts the entry writer coroutine that periodically flushes queued entries.
     */
    fun start() {
        if (running) return
        running = true

        writerJob = scope.launch {
            while (isActive && running) {
                try {
                    flushAll(false)
                } catch (e: Exception) {
                    plugin.print(e)
                }
                delay(flushIntervalMs)
            }
        }
    }

    /**
     * Adds an entry to the queue for the entry's action table.
     */
    fun add(entry: DbEntry) {
        if (!entry.action.isEnabled) return
        val table = entry.action.table ?: return

        table.queue.add(entry)

        synchronized(listenerLock) {
            listeners.forEach { it.accept(entry) }
        }
    }

    /**
     * Returns the total number of entries queued across all tables.
     */
    fun queueSize(): Int {
        return Table.values().sumOf { it.queue.size }
    }

    /**
     * Flushes all queued entries to the database.
     *
     * @param force if true, forces immediate flush regardless of timing
     */
    suspend fun flushAll(force: Boolean) {
        for (table in Table.values()) {
            if (!table.hasAPEntries()) continue
            if (!table.exists(plugin)) continue

            val entries = mutableListOf<DbEntry>()
            var entry: DbEntry? = table.queue.poll()
            while (entry != null && entries.size < batchSize) {
                entries.add(entry)
                entry = table.queue.poll()
            }

            if (entries.isNotEmpty()) {
                try {
                    databaseService.entryRepository.batchInsert(
                        table = table,
                        entries = entries,
                        worldIdResolver = { world ->
                            if (world != null) {
                                runBlocking {
                                    databaseService.worldRepository.getOrCreateWorldId(world) { plugin.doesWorldExist(it) }
                                }
                            } else -1
                        },
                        uidResolver = { e ->
                            runBlocking {
                                databaseService.userRepository.getUID(e.getUserUUID(), true)
                            }
                        },
                        targetIdResolver = { e ->
                            runBlocking {
                                databaseService.userRepository.getUID(e.getTargetUUID(), true)
                            }
                        }
                    )
                } catch (e: Exception) {
                    plugin.print(e)
                    // Re-queue failed entries
                    entries.forEach { table.queue.add(it) }
                }
            }
        }
    }

    /**
     * Adds or removes an entry listener.
     */
    fun addRemoveEntryListener(consumer: Consumer<DbEntry>, add: Boolean) {
        synchronized(listenerLock) {
            if (add) listeners.add(consumer)
            else listeners.remove(consumer)
        }
    }

    /**
     * Shuts down the writer service, flushing any remaining entries.
     */
    fun shutdown() {
        running = false
        writerJob?.cancel()

        // Final flush
        runBlocking {
            try {
                flushAll(true)
            } catch (e: Exception) {
                plugin.print(e)
            }
        }

        scope.cancel()
    }
}
