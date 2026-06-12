package dev.heliosares.auxprotect.database.repository

import dev.heliosares.auxprotect.database.schema.UidsTable
import dev.heliosares.auxprotect.database.schema.EnumIdsTable
import org.jetbrains.exposed.sql.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for string↔int ID mappings.
 * Replaces StringIDManager with Exposed-based operations.
 * Maintains bidirectional in-memory caches for O(1) lookups.
 */
class StringIdRepository(
    database: Database,
    private val uidsTable: UidsTable,
    private val enumIdsTable: EnumIdsTable
) : BaseRepository(database) {

    // UID caches
    private val uidValueToId = ConcurrentHashMap<String, Int>()
    private val uidIdToValue = ConcurrentHashMap<Int, String>()

    // Enum ID caches
    private val enumValueToId = ConcurrentHashMap<String, Int>()
    private val enumIdToValue = ConcurrentHashMap<Int, String>()

    /**
     * Initializes the repository by loading all existing mappings into memory.
     */
    suspend fun initialize() {
        loadCache(uidsTable, uidValueToId, uidIdToValue)
        loadCache(enumIdsTable, enumValueToId, enumIdToValue)
    }

    /**
     * Gets the UID ID for the given value, inserting if it doesn't exist.
     */
    suspend fun getOrInsertUid(value: String): Int {
        return getOrInsert(uidsTable, uidValueToId, uidIdToValue, value)
    }

    /**
     * Gets the UID ID for the given value, or null if not found.
     */
    fun getUidId(value: String): Int? {
        return uidValueToId[value.lowercase()]
    }

    /**
     * Gets the UID value for the given ID, or null if not found.
     */
    fun getUidValue(id: Int): String? {
        return uidIdToValue[id]
    }

    /**
     * Gets or inserts all values, returning a map of value to ID.
     */
    suspend fun getOrInsertAllUids(values: Collection<String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for (value in values) {
            result[value] = getOrInsertUid(value)
        }
        return result
    }

    /**
     * Gets the enum ID for the given value, inserting if it doesn't exist.
     */
    suspend fun getOrInsertEnumId(value: String): Int {
        return getOrInsert(enumIdsTable, enumValueToId, enumIdToValue, value)
    }

    /**
     * Gets the enum ID for the given value, or null if not found.
     */
    fun getEnumId(value: String): Int? {
        return enumValueToId[value.lowercase()]
    }

    /**
     * Gets the enum value for the given ID, or null if not found.
     */
    fun getEnumValue(id: Int): String? {
        return enumIdToValue[id]
    }

    /**
     * Clears all caches.
     */
    fun clearCaches() {
        uidValueToId.clear()
        uidIdToValue.clear()
        enumValueToId.clear()
        enumIdToValue.clear()
    }

    private suspend fun loadCache(
        table: Table,
        valueToId: ConcurrentHashMap<String, Int>,
        idToValue: ConcurrentHashMap<Int, String>
    ) {
        dbQuery {
            val idCol = table.columns.first { it.name == "id" }
            val valueCol = table.columns.first { it.name == "value" }

            @Suppress("UNCHECKED_CAST")
            table.select(idCol, valueCol).forEach { row ->
                val id = row[idCol as Column<Int>]
                val value = row[valueCol as Column<String>]
                idToValue[id] = value
                valueToId[value.lowercase()] = id
            }
        }
    }

    private suspend fun getOrInsert(
        table: Table,
        valueToId: ConcurrentHashMap<String, Int>,
        idToValue: ConcurrentHashMap<Int, String>,
        value: String
    ): Int {
        val key = value.lowercase()
        valueToId[key]?.let { return it }

        return dbQuery {
            val idCol = table.columns.first { it.name == "id" }
            val valueCol = table.columns.first { it.name == "value" }

            // Double-check in transaction
            valueToId[key]?.let { return@dbQuery it }

            @Suppress("UNCHECKED_CAST")
            val generatedId = table.insert { stmt ->
                stmt[valueCol as Column<String>] = value
            }[idCol as Column<Int>]

            valueToId[key] = generatedId
            idToValue[generatedId] = value
            generatedId
        }
    }
}
