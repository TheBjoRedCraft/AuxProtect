package dev.heliosares.auxprotect.database.query

import dev.heliosares.auxprotect.core.IAuxProtect
import dev.heliosares.auxprotect.core.Parameters
import dev.heliosares.auxprotect.database.*
import java.sql.ResultSet

/**
 * Translates Parameters into SQL queries and parses results.
 * This bridges the existing Parameters.toSQL() mechanism with the new service layer.
 *
 * The query builder delegates to the existing Parameters.toSQL() method for SQL generation
 * to maintain exact backward compatibility, while providing a cleaner API for the service layer.
 */
class QueryBuilder(
    private val plugin: IAuxProtect,
    private val sql: SQLManager
) {

    companion object {
        /** Maximum number of results per lookup. */
        const val MAX_LOOKUP_SIZE = 500_000
    }

    /**
     * Builds a complete SELECT statement from Parameters.
     *
     * @param params the search parameters
     * @return a QueryPlan containing the SQL statement and bind parameters
     */
    fun buildLookupQuery(params: Parameters): QueryPlan {
        val sqlStmts = params.toSQL(plugin)
        val bindParams = sqlStmts.drop(1).toList()

        val table = params.table
        val sb = StringBuilder("SELECT * FROM $table")

        // Add index hints if enabled
        if (plugin.apConfig.indexing) {
            val index = when {
                params.users.isNotEmpty() -> Table.Index.UID
                params.actions.isNotEmpty() &&
                        table.hasLocation(plugin.platform) &&
                        params.radius.isNotEmpty() &&
                        params.worldID >= 0 -> Table.Index.XZ

                else -> null
            }
            if (index != null) {
                sb.append(" ").append(sql.indexedBy(index.getName(table)))
            }
        }

        // Add WHERE clause
        if (sqlStmts[0].length > 1) {
            sb.append(" WHERE ").append(sqlStmts[0])
        }

        sb.append(" ORDER BY time DESC LIMIT ").append(MAX_LOOKUP_SIZE + 1).append(";")

        return QueryPlan(sb.toString(), bindParams, table)
    }

    /**
     * Builds a COUNT query from Parameters.
     */
    fun buildCountQuery(params: Parameters): QueryPlan {
        val sqlStmts = params.toSQL(plugin)
        val bindParams = sqlStmts.drop(1).toList()

        val sb = StringBuilder(sql.countStmt).append(params.table)
        if (sqlStmts[0].length > 1) {
            sb.append(" WHERE ").append(sqlStmts[0])
        }

        return QueryPlan(sb.toString(), bindParams, params.table)
    }

    /**
     * Parses a ResultSet row into a DbEntry using registered loaders.
     *
     * @param rs the result set positioned at the current row
     * @param table the table being queried
     * @param loaders registered entry loaders for custom entry types
     * @return the parsed DbEntry, or null if the action is unknown
     */
    fun parseRow(
        rs: ResultSet,
        table: Table,
        loaders: List<EntryLoader>
    ): DbEntry? {
        val hasLocation = table.hasLocation(plugin.platform)
        val hasData = table.hasData()
        val hasAction = table.hasActionId()
        val hasLook = table.hasLook()

        val snowflake = rs.getLong("time")
        val uid = rs.getInt("uid")

        val actionId = when {
            hasAction -> rs.getInt("action_id")
            table == Table.AUXPROTECT_COMMANDS -> EntryAction.COMMAND.id
            table == Table.AUXPROTECT_CHAT -> EntryAction.CHAT.id
            table == Table.AUXPROTECT_XRAY -> EntryAction.VEIN.id
            else -> -1
        }

        var world: String? = null
        var x = 0;
        var y = 0;
        var z = 0
        if (hasLocation) {
            world = sql.getWorld(rs.getInt("world_id"))
            x = rs.getInt("x")
            y = rs.getInt("y")
            z = rs.getInt("z")
        }

        var pitch = 0;
        var yaw = 180
        if (hasLook) {
            pitch = rs.getInt("pitch")
            yaw = rs.getInt("yaw")
        }

        val data = if (hasData) rs.getString("data") else null

        val entryAction = EntryAction.getAction(table, actionId) ?: return null
        val state = entryAction.hasDual && entryAction.id != actionId

        val target: String?
        val targetId: Int
        if (table.hasStringTarget()) {
            target = rs.getString("target")
            targetId = -1
        } else {
            target = null
            targetId = rs.getInt("target_id")
        }

        val entryData = EntryData(
            table,
            snowflake,
            uid,
            entryAction,
            state,
            world,
            x,
            y,
            z,
            pitch,
            yaw,
            target,
            targetId,
            data,
            rs
        )

        // Try custom loaders first
        for (loader in loaders) {
            if (!loader.applies().test(entryData)) continue
            val entry = loader.loader().load(entryData)
            if (entry != null) {
                if (table.hasBlobID()) {
                    val blobid = rs.getLong("blobid")
                    entry.setBlobID(if (rs.wasNull()) -1 else blobid)
                }
                return entry
            }
        }

        // Default entry creation
        val entry = DbEntry(
            snowflake,
            uid,
            entryAction,
            state,
            world,
            x,
            y,
            z,
            pitch,
            yaw,
            target,
            targetId,
            data,
            sql
        )
        if (table.hasBlobID()) {
            val blobid = rs.getLong("blobid")
            entry.setBlobID(if (rs.wasNull()) -1 else blobid)
        }
        return entry
    }

    /**
     * Represents a query plan with SQL statement and bind parameters.
     */
    data class QueryPlan(
        val sql: String,
        val bindParams: List<String>,
        val table: Table
    )
}
