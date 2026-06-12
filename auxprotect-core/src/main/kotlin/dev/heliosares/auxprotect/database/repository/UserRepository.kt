package dev.heliosares.auxprotect.database.repository

import dev.heliosares.auxprotect.database.EntryAction
import dev.heliosares.auxprotect.database.Snowflake
import dev.heliosares.auxprotect.database.schema.AuxProtectLongtermTable
import dev.heliosares.auxprotect.database.schema.UidsTable
import dev.heliosares.auxprotect.database.schema.UserDataPendInvTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for user UUID/username/IP management.
 * Replaces SQLUserManager with Exposed-based operations.
 * Maintains a cache for username lookups with lazy loading.
 */
class UserRepository(
    database: Database,
    private val longtermTable: AuxProtectLongtermTable,
    private val uidsTable: UidsTable,
    private val userDataPendInvTable: UserDataPendInvTable,
    private val stringIdRepo: StringIdRepository
) : BaseRepository(database) {

    companion object {
        /** Cache expiry time in milliseconds (5 minutes). */
        const val USERNAME_CACHE_EXPIRY_MS = 300_000L
    }

    private data class CachedUsername(
        val username: String,
        val cachedAt: Long = System.currentTimeMillis()
    )

    private val usernameCache = ConcurrentHashMap<Int, CachedUsername>()

    /**
     * Gets the UID for a value, optionally inserting if not found.
     */
    suspend fun getUID(value: String?, insert: Boolean): Int {
        if (value == null || value.equals("#null", ignoreCase = true)) return -1
        if (value.isBlank()) return 0

        return if (insert) {
            stringIdRepo.getOrInsertUid(value)
        } else {
            stringIdRepo.getUidId(value) ?: -1
        }
    }

    /**
     * Resolves a UID to the most recent username.
     */
    suspend fun getUsernameFromUID(uid: Int): String? {
        if (uid < 0) return null
        if (uid == 0) return ""

        // Check cache
        usernameCache[uid]?.let { cached ->
            if (System.currentTimeMillis() - cached.cachedAt < USERNAME_CACHE_EXPIRY_MS) {
                return cached.username
            }
        }

        // Query database
        val username = dbQuery {
            longtermTable
                .join(uidsTable, JoinType.LEFT, longtermTable.targetId, uidsTable.id)
                .select(uidsTable.value)
                .where {
                    (longtermTable.actionId eq EntryAction.USERNAME.id.toShort()) and
                            (longtermTable.uid eq uid)
                }
                .orderBy(longtermTable.time to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.get(uidsTable.value)
        }

        if (username != null) {
            usernameCache[uid] = CachedUsername(username)
        }

        return username
    }

    /**
     * Gets the UUID string from a UID.
     */
    fun getUUIDFromUID(uid: Int): String? {
        if (uid < 0) return "#null"
        if (uid == 0) return ""
        return stringIdRepo.getUidValue(uid)
    }

    /**
     * Gets the earliest join time for a UID.
     */
    suspend fun getJoinTime(uid: Int): Long {
        return dbQuery {
            longtermTable
                .select(longtermTable.time.min())
                .where { longtermTable.uid eq uid }
                .firstOrNull()
                ?.get(longtermTable.time.min()) ?: 0L
        } / Snowflake.COUNTER_FACTOR
    }

    /**
     * Gets the UID from a username ID.
     */
    suspend fun getUIDFromUsernameID(nameID: Int): Int {
        if (nameID <= 0) return -1
        return dbQuery {
            longtermTable
                .select(longtermTable.uid)
                .where {
                    (longtermTable.targetId eq nameID) and
                            (longtermTable.actionId eq EntryAction.USERNAME.id.toShort())
                }
                .orderBy(longtermTable.time to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.get(longtermTable.uid) ?: -1
        }
    }

    /**
     * Gets pending inventory data for a user.
     */
    suspend fun getPendingInventory(uid: Int): ByteArray? {
        if (uid <= 0) return null
        return dbQuery {
            userDataPendInvTable
                .select(userDataPendInvTable.pending)
                .where { userDataPendInvTable.uid eq uid }
                .firstOrNull()
                ?.get(userDataPendInvTable.pending)
                ?.bytes
        }
    }

    /**
     * Sets or removes pending inventory data for a user.
     */
    suspend fun setPendingInventory(uid: Int, blob: ByteArray?) {
        if (uid <= 0) throw IllegalArgumentException("Invalid UID")

        dbQuery {
            if (blob == null) {
                userDataPendInvTable.deleteWhere { userDataPendInvTable.uid eq uid }
            } else {
                val time = System.currentTimeMillis()
                val updated =
                    userDataPendInvTable.update({ userDataPendInvTable.uid eq uid }) { stmt ->
                        stmt[userDataPendInvTable.time] = time
                        stmt[userDataPendInvTable.pending] = ExposedBlob(blob)
                    }
                if (updated == 0) {
                    userDataPendInvTable.insert { stmt ->
                        stmt[userDataPendInvTable.time] = time
                        stmt[userDataPendInvTable.uid] = uid
                        stmt[userDataPendInvTable.pending] = ExposedBlob(blob)
                    }
                }
            }
        }
    }

    /**
     * Cleans up expired cache entries.
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        usernameCache.entries.removeIf { now - it.value.cachedAt > USERNAME_CACHE_EXPIRY_MS }
    }
}
