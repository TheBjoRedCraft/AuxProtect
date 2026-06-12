package dev.heliosares.auxprotect.database.repository

import dev.heliosares.auxprotect.database.schema.VersionTable
import dev.heliosares.auxprotect.database.schema.LastsTable
import dev.heliosares.auxprotect.database.schema.MigrationTasksTable
import org.jetbrains.exposed.sql.*

/**
 * Repository for version tracking, last timestamps, and migration task tracking.
 */
class MetadataRepository(
    database: Database,
    private val versionTable: VersionTable,
    private val lastsTable: LastsTable,
    private val migrationTasksTable: MigrationTasksTable
) : BaseRepository(database) {

    /**
     * Gets the current database version (most recent entry).
     */
    suspend fun getVersion(): Int {
        return dbQuery {
            versionTable
                .select(versionTable.version)
                .orderBy(versionTable.time to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.get(versionTable.version) ?: 0
        }
    }

    /**
     * Gets the original database version (oldest entry).
     */
    suspend fun getOriginalVersion(): Int {
        return dbQuery {
            versionTable
                .select(versionTable.version)
                .orderBy(versionTable.time to SortOrder.ASC)
                .limit(1)
                .firstOrNull()
                ?.get(versionTable.version) ?: 0
        }
    }

    /**
     * Sets the database version by inserting a new version record.
     */
    suspend fun setVersion(version: Int) {
        dbQuery {
            versionTable.insert { stmt ->
                stmt[time] = System.currentTimeMillis()
                stmt[this.version] = version
            }
        }
    }

    /**
     * Gets a "last" value by key ID.
     */
    suspend fun getLast(keyId: Short): Long {
        return dbQuery {
            lastsTable
                .select(lastsTable.value)
                .where { lastsTable.name eq keyId }
                .firstOrNull()
                ?.get(lastsTable.value) ?: -1L
        }
    }

    /**
     * Sets a "last" value by key ID.
     */
    suspend fun setLast(keyId: Short, value: Long) {
        dbQuery {
            lastsTable.update({ lastsTable.name eq keyId }) { stmt ->
                stmt[this.value] = value
            }
        }
    }

    /**
     * Ensures a "last" key exists (inserts if missing).
     */
    suspend fun ensureLastKey(keyId: Short) {
        dbQuery {
            val exists = lastsTable
                .select(lastsTable.name)
                .where { lastsTable.name eq keyId }
                .firstOrNull() != null

            if (!exists) {
                try {
                    lastsTable.insert { stmt ->
                        stmt[name] = keyId
                    }
                } catch (_: java.sql.SQLIntegrityConstraintViolationException) {
                    // Already exists (race condition with concurrent insert)
                } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                    // SQLite uses a different exception hierarchy for constraint violations
                }
            }
        }
    }

    /**
     * Checks if a migration task has been completed.
     */
    suspend fun isMigrationTaskComplete(taskId: Int): Boolean {
        return dbQuery {
            migrationTasksTable
                .select(migrationTasksTable.id)
                .where { migrationTasksTable.id eq taskId }
                .firstOrNull() != null
        }
    }

    /**
     * Marks a migration task as completed.
     */
    suspend fun completeMigrationTask(taskId: Int) {
        dbQuery {
            migrationTasksTable.insert { stmt ->
                stmt[id] = taskId
            }
        }
    }
}
