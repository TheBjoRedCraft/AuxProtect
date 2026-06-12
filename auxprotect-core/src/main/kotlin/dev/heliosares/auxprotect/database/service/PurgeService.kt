package dev.heliosares.auxprotect.database.service

import dev.heliosares.auxprotect.core.IAuxProtect
import dev.heliosares.auxprotect.database.Table
import dev.heliosares.auxprotect.database.repository.EntryRepository
import dev.heliosares.auxprotect.database.repository.MetadataRepository
import dev.heliosares.auxprotect.database.SQLManager
import kotlinx.coroutines.runBlocking

/**
 * Service for purging old entries from the database.
 * Supports auto-purge scheduling and per-table configurable retention.
 */
class PurgeService(
    private val plugin: IAuxProtect,
    private val entryRepository: EntryRepository,
    private val metadataRepository: MetadataRepository,
    private val isMySQL: Boolean
) {

    /**
     * Purges entries older than the specified retention period from a single table.
     *
     * @param table the table to purge
     * @param retentionMs retention period in milliseconds
     * @return the number of deleted rows
     */
    suspend fun purge(table: Table, retentionMs: Long): Int {
        if (!table.canPurge()) return 0
        return entryRepository.purge(table, retentionMs)
    }

    /**
     * Purges all purgeable tables with their configured auto-purge intervals.
     *
     * @return total number of deleted rows
     */
    suspend fun autoPurge(): Int {
        var totalCount = 0
        for (table in Table.values()) {
            if (!table.canPurge()) continue
            if (!table.exists(plugin)) continue
            if (table.autoPurgeInterval < Table.MIN_PURGE_INTERVAL) continue

            plugin.info("Purging ${table}...")
            try {
                totalCount += purge(table, table.autoPurgeInterval)
            } catch (e: Exception) {
                plugin.warning("Error purging $table")
                plugin.print(e)
            }
        }
        return totalCount
    }

    /**
     * Runs vacuum on SQLite databases to reclaim space.
     * No-op for MySQL/MariaDB.
     */
    fun vacuum() {
        if (isMySQL) return
        // Vacuum is handled by the existing SQLManager since it requires
        // raw connection access outside of Exposed's transaction model
        try {
            plugin.sqlManager?.let { sql ->
                runBlocking {
                    sql.execute({ connection ->
                        sql.vacuum(connection)
                    }, 0L)
                }
            }
        } catch (e: Exception) {
            plugin.print(e)
        }
    }
}
