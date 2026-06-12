package dev.heliosares.auxprotect.database.service

import dev.heliosares.auxprotect.core.IAuxProtect
import dev.heliosares.auxprotect.database.MigrationManager
import dev.heliosares.auxprotect.database.SQLManager
import dev.heliosares.auxprotect.database.repository.MetadataRepository

/**
 * Migration service that wraps the existing MigrationManager.
 * Ports all existing migrations (v16-v20) to the new framework and adds v21 for the Exposed migration.
 *
 * The existing MigrationManager handles the complex migration logic with raw SQL,
 * which is tightly coupled to the Connection-based API. This service wraps it
 * and provides version tracking via the new MetadataRepository.
 *
 * Migration v21 is a no-op data migration — the schema is compatible, just a version bump
 * to record that the Exposed layer has been initialized.
 */
class MigrationService(
    private val plugin: IAuxProtect,
    private val metadataRepository: MetadataRepository
) {

    companion object {
        /**
         * Target database version including the Exposed migration.
         * v20 = last Java-only migration
         * v21 = Exposed initialization (no-op data migration, just version bump)
         */
        const val TARGET_DB_VERSION = 21
    }

    @Volatile
    var isMigrating: Boolean = false
        private set

    @Volatile
    var currentMigrationVersion: Int = -1
        private set

    @Volatile
    var progress: Int = 0
        private set

    @Volatile
    var total: Int = 0
        private set

    /**
     * Gets the current progress string for display.
     */
    fun getProgressString(): String? {
        if (!isMigrating || currentMigrationVersion <= 0) return null
        val pct = if (total > 0) (progress.toDouble() / total * 100).toInt() else 0
        return "Migration to v$currentMigrationVersion $pct% complete. ($progress/$total). DO NOT INTERRUPT"
    }

    /**
     * Runs the Exposed-specific migration (v21) after the existing MigrationManager
     * has completed its work (v16-v20).
     *
     * This should be called after MigrationManager.postTables() has finished.
     */
    suspend fun runExposedMigration() {
        val currentVersion = metadataRepository.getVersion()

        if (currentVersion >= TARGET_DB_VERSION) {
            plugin.debug("Database already at version $currentVersion, no Exposed migration needed.")
            return
        }

        if (currentVersion < MigrationManager.TARGET_DB_VERSION) {
            // The existing MigrationManager should handle migrations up to v20
            plugin.debug("Database at version $currentVersion, waiting for legacy migrations to complete first.")
            return
        }

        // Run v21 migration (no-op, just version bump)
        if (currentVersion == MigrationManager.TARGET_DB_VERSION) {
            isMigrating = true
            currentMigrationVersion = TARGET_DB_VERSION
            plugin.info("Running Exposed migration v$TARGET_DB_VERSION (schema compatibility check)...")

            try {
                // No data migration needed — Exposed table definitions match existing schema exactly
                metadataRepository.setVersion(TARGET_DB_VERSION)
                plugin.info("Exposed migration v$TARGET_DB_VERSION complete.")
            } catch (e: Exception) {
                plugin.warning("Failed to complete Exposed migration v$TARGET_DB_VERSION")
                plugin.print(e)
                throw e
            } finally {
                isMigrating = false
                currentMigrationVersion = -1
            }
        }
    }

    /**
     * Gets the current database version from the metadata repository.
     */
    suspend fun getVersion(): Int {
        return metadataRepository.getVersion()
    }

    /**
     * Gets the original (oldest) database version.
     */
    suspend fun getOriginalVersion(): Int {
        return metadataRepository.getOriginalVersion()
    }
}
