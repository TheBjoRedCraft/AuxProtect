package dev.heliosares.auxprotect.database.repository

import dev.heliosares.auxprotect.database.schema.WorldsTable
import org.jetbrains.exposed.sql.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for world name↔ID mappings.
 * Maintains an in-memory bidirectional cache.
 */
class WorldRepository(
    database: Database,
    private val worldsTable: WorldsTable
) : BaseRepository(database) {

    private val nameToId = ConcurrentHashMap<String, Int>()
    private val idToName = ConcurrentHashMap<Int, String>()
    private var nextWid = 0

    /**
     * Loads all existing world mappings into memory.
     */
    suspend fun initialize() {
        dbQuery {
            worldsTable.selectAll().forEach { row ->
                val name = row[worldsTable.name]
                val wid = row[worldsTable.wid].toInt()
                nameToId[name] = wid
                idToName[wid] = name
                if (wid >= nextWid) {
                    nextWid = wid + 1
                }
            }
        }
    }

    /**
     * Gets the world ID for the given world name, creating a new mapping if needed.
     *
     * @param worldName the world name
     * @param worldExists function to check if the world actually exists on the server
     * @return the world ID, or -1 if the world doesn't exist
     */
    suspend fun getOrCreateWorldId(worldName: String, worldExists: (String) -> Boolean): Int {
        nameToId[worldName]?.let { return it }

        if (!worldExists(worldName)) return -1

        return dbQuery {
            // Double-check in transaction
            nameToId[worldName]?.let { return@dbQuery it }

            val wid = nextWid++
            worldsTable.insert { stmt ->
                stmt[name] = worldName
                stmt[this.wid] = wid.toShort()
            }

            nameToId[worldName] = wid
            idToName[wid] = worldName
            wid
        }
    }

    /**
     * Gets the world name for the given world ID.
     */
    fun getWorldName(wid: Int): String? {
        return idToName[wid]
    }

    /**
     * Gets the world ID for the given world name from cache only.
     */
    fun getWorldId(worldName: String): Int? {
        return nameToId[worldName]
    }
}
