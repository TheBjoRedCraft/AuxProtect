package dev.heliosares.auxprotect.database.service

import dev.heliosares.auxprotect.core.IAuxProtect
import dev.heliosares.auxprotect.core.Language
import dev.heliosares.auxprotect.core.Parameters
import dev.heliosares.auxprotect.database.DbEntry
import dev.heliosares.auxprotect.database.DbEntryGroup
import dev.heliosares.auxprotect.database.EntryLoader
import dev.heliosares.auxprotect.database.LookupManager
import dev.heliosares.auxprotect.database.SQLManager
import dev.heliosares.auxprotect.database.Table
import dev.heliosares.auxprotect.database.query.QueryBuilder
import dev.heliosares.auxprotect.exceptions.LookupException
import dev.kshl.kshlib.exceptions.BusyException
import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Service for executing lookup queries against the database.
 * Replaces LookupManager with a service-oriented approach.
 *
 * Note: This service currently delegates to the existing LookupManager
 * for query execution to maintain full backward compatibility with
 * the complex result parsing logic. Future phases can migrate the
 * actual execution to pure Exposed queries.
 */
class LookupService(
    private val plugin: IAuxProtect,
    private val database: Database
) {

    private val loaders = mutableListOf<EntryLoader>()
    private val groupParameterCache = java.util.concurrent.ConcurrentHashMap<Long, Parameters>()

    companion object {
        /** Maximum number of results per lookup. */
        const val MAX_LOOKUP_SIZE = 500_000

        /** Group parameter cache TTL in milliseconds (3 hours). */
        const val GROUP_CACHE_TTL_MS = 3 * 3600_000L
    }

    /**
     * Performs a lookup using the provided Parameters.
     * Delegates to the existing SQLManager/LookupManager for actual execution.
     *
     * @param params the search parameters
     * @return list of matching entries
     * @throws LookupException if the lookup fails
     */
    @Throws(LookupException::class)
    fun lookup(params: Parameters): List<DbEntry> {
        // Delegate to existing LookupManager for now
        return plugin.sqlManager.lookupManager.lookup(params)
    }

    /**
     * Counts entries matching the given parameters.
     *
     * @param params one or more parameter sets to count
     * @return total count across all parameter sets
     * @throws LookupException if the count fails
     */
    @Throws(LookupException::class)
    fun count(vararg params: Parameters): Int {
        return plugin.sqlManager.lookupManager.count(*params)
    }

    /**
     * Registers a custom entry loader for specialized DbEntry types.
     */
    fun addLoader(loader: EntryLoader) {
        loaders.add(loader)
    }

    /**
     * Gets cached parameters for a group hash.
     */
    fun getParametersForGroup(groupHash: Long): Parameters? {
        return groupParameterCache[groupHash] ?: LookupManager.getParametersForGroup(groupHash)
    }
}
