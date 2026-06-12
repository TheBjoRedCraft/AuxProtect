package dev.heliosares.auxprotect.database.repository

import dev.heliosares.auxprotect.database.DbEntry
import dev.heliosares.auxprotect.database.EntryAction
import dev.heliosares.auxprotect.database.Snowflake
import dev.heliosares.auxprotect.database.Table
import dev.heliosares.auxprotect.database.schema.TableRegistry
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.statements.InsertStatement

/**
 * Repository handling all AP_ENTRIES tables (insert, lookup, count, purge).
 * Supports batch insert with multi-row VALUES matching current put() behavior.
 */
class EntryRepository(
    database: Database,
    private val registry: TableRegistry
) : BaseRepository(database) {

    /**
     * Batch inserts entries into the specified table.
     *
     * @param table the target table enum
     * @param entries the entries to insert
     * @param worldIdResolver resolves world name to world ID
     * @param uidResolver resolves user label to UID
     * @param targetIdResolver resolves target label to target ID
     * @param blobIdResolver resolves blob bytes to blob ID
     */
    suspend fun batchInsert(
        table: Table,
        entries: List<DbEntry>,
        worldIdResolver: (String?) -> Int,
        uidResolver: (DbEntry) -> Int,
        targetIdResolver: (DbEntry) -> Int,
        blobIdResolver: ((ByteArray?, Long) -> Long)? = null
    ) {
        if (entries.isEmpty()) return

        val exposedTable = registry.getExposedTable(table) ?: return

        processBatched(entries) { batch ->
            exposedTable.batchInsert(batch, shouldReturnGeneratedValues = false) { entry ->
                populateInsertStatement(this, table, entry, worldIdResolver, uidResolver, targetIdResolver, blobIdResolver)
            }
        }
    }

    /**
     * Counts entries matching the given WHERE clause in the specified table.
     *
     * @param table the table to count in
     * @param whereClause optional SQL WHERE clause
     * @return the count of matching entries
     */
    suspend fun count(table: Table): Long {
        val exposedTable = registry.getExposedTable(table) ?: return 0L
        return dbQuery {
            exposedTable.selectAll().count()
        }
    }

    /**
     * Purges entries older than the specified time threshold.
     *
     * @param table the table to purge from
     * @param retentionMs retention period in milliseconds
     * @return the number of deleted rows
     */
    suspend fun purge(table: Table, retentionMs: Long): Int {
        if (!table.canPurge()) return 0
        if (retentionMs < Table.MIN_PURGE_INTERVAL) return 0

        val exposedTable = registry.getExposedTable(table) ?: return 0
        val snowflakeThreshold = (System.currentTimeMillis() - retentionMs) * Snowflake.COUNTER_FACTOR

        return dbQuery {
            // All entry tables have a 'time' column as the first column
            val timeColumn = exposedTable.columns.first { it.name == "time" }
            @Suppress("UNCHECKED_CAST")
            exposedTable.deleteWhere {
                (timeColumn as Column<Long>) less snowflakeThreshold
            }
        }
    }

    /**
     * Populates an insert statement for a DbEntry based on the table's characteristics.
     */
    private fun populateInsertStatement(
        stmt: InsertStatement<*>,
        table: Table,
        entry: DbEntry,
        worldIdResolver: (String?) -> Int,
        uidResolver: (DbEntry) -> Int,
        targetIdResolver: (DbEntry) -> Int,
        blobIdResolver: ((ByteArray?, Long) -> Long)?
    ) {
        val exposedTable = registry.getExposedTable(table) ?: return
        val columns = exposedTable.columns.associateBy { it.name }

        columns["time"]?.let { stmt[it as Column<Long>] = entry.snowflake }
        columns["uid"]?.let { stmt[it as Column<Int>] = uidResolver(entry) }

        if (table.hasActionId()) {
            columns["action_id"]?.let {
                val actionId = if (entry.state) entry.action.idPos else entry.action.id
                stmt[it as Column<Short>] = actionId.toShort()
            }
        }

        if (table.hasLocation(null)) {
            columns["world_id"]?.let { stmt[it as Column<Short>] = worldIdResolver(entry.world).toShort() }
            columns["x"]?.let { stmt[it as Column<Int>] = entry.x }
            columns["y"]?.let { stmt[it as Column<Short>] = entry.y.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() }
            columns["z"]?.let { stmt[it as Column<Int>] = entry.z }
        }

        if (table.hasStringTarget()) {
            columns["target"]?.let { stmt[it as Column<String?>] = entry.getTarget(false) }
        } else {
            columns["target_id"]?.let { stmt[it as Column<Int>] = targetIdResolver(entry) }
        }

        if (table.hasData()) {
            columns["data"]?.let { stmt[it as Column<String?>] = entry.data }
        }

        if (table.hasBlobID() && blobIdResolver != null) {
            columns["blobid"]?.let {
                val blob = try { entry.blob } catch (_: Exception) { null }
                if (blob != null) {
                    stmt[it as Column<Long?>] = blobIdResolver(blob, entry.snowflake)
                }
            }
        }
    }
}
