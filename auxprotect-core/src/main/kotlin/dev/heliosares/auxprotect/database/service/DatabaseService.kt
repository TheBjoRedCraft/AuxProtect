package dev.heliosares.auxprotect.database.service

import dev.heliosares.auxprotect.core.IAuxProtect
import dev.heliosares.auxprotect.database.DatabaseDispatcher
import dev.heliosares.auxprotect.database.SQLManager
import dev.heliosares.auxprotect.database.config.DatabaseConfig
import dev.heliosares.auxprotect.database.config.DatabaseFactory
import dev.heliosares.auxprotect.database.query.QueryBuilder
import dev.heliosares.auxprotect.database.repository.*
import dev.heliosares.auxprotect.database.schema.TableRegistry
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Main entry point for the new database service layer.
 * Replaces SQLManager as the primary public API for database operations.
 *
 * This service coordinates all repositories, manages lifecycle, and provides
 * the bridge between the old Java API and the new Kotlin/Exposed layer.
 *
 * Error handling:
 * - Exposed's transaction {} blocks with automatic rollback on exception
 * - Configurable retry with exponential backoff for transient failures (in BaseRepository)
 * - Structured logging for all DB operations
 *
 * Shutdown:
 * - Coroutine cancellation with timeout
 * - Connection pool graceful drain via HikariCP
 */
class DatabaseService(
    private val plugin: IAuxProtect,
    private val config: DatabaseConfig
) {

    private lateinit var database: Database
    private lateinit var dispatcher: DatabaseDispatcher

    // Schema
    lateinit var registry: TableRegistry
        private set

    // Repositories
    lateinit var entryRepository: EntryRepository
        private set
    lateinit var blobRepository: BlobRepository
        private set
    lateinit var stringIdRepository: StringIdRepository
        private set
    lateinit var userRepository: UserRepository
        private set
    lateinit var worldRepository: WorldRepository
        private set
    lateinit var metadataRepository: MetadataRepository
        private set

    // Services
    lateinit var lookupService: LookupService
        private set
    lateinit var purgeService: PurgeService
        private set
    lateinit var migrationService: MigrationService
        private set

    // Query builder
    lateinit var queryBuilder: QueryBuilder
        private set

    private var initialized = false

    /**
     * Initializes the database service: connects, creates schema, and initializes all repositories.
     * This should be called once during plugin startup.
     *
     * Uses Exposed's SchemaUtils.createMissingTablesAndColumns() for automatic schema creation,
     * which handles CREATE TABLE IF NOT EXISTS and ADD COLUMN for new columns.
     */
    fun initialize() {
        plugin.info("Initializing Exposed database service...")
        val startTime = System.currentTimeMillis()

        // Create dispatcher with backend-specific thread pool
        dispatcher = DatabaseDispatcher(if (config.isMySQL) 4 else 1)

        // Connect to database via HikariCP
        database = DatabaseFactory.create(config)
        plugin.debug("HikariCP connection pool established (${if (config.isMySQL) "MySQL" else "SQLite"})")

        // Set up schema registry with prefix
        registry = TableRegistry(config.tablePrefix)

        // Create missing tables and columns via Exposed
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(*registry.getAllTables().toTypedArray())
        }
        plugin.debug("Schema verification complete")

        // Initialize repositories
        entryRepository = EntryRepository(database, registry)
        blobRepository = BlobRepository(database, registry.invBlobTable, registry.transactionsBlobTable)
        stringIdRepository = StringIdRepository(database, registry.uidsTable, registry.enumIdsTable)
        userRepository = UserRepository(database, registry.longtermTable, registry.uidsTable, registry.userDataPendInvTable, stringIdRepository)
        worldRepository = WorldRepository(database, registry.worldsTable)
        metadataRepository = MetadataRepository(database, registry.versionTable, registry.lastsTable, registry.migrationTasksTable)

        // Load caches from database
        runBlocking {
            stringIdRepository.initialize()
            worldRepository.initialize()
        }
        plugin.debug("Repository caches loaded")

        // Initialize services
        lookupService = LookupService(plugin, database)
        purgeService = PurgeService(plugin, entryRepository, metadataRepository, config.isMySQL)
        migrationService = MigrationService(plugin, metadataRepository)

        // Run Exposed migration (v21 no-op version bump)
        runBlocking {
            migrationService.runExposedMigration()
        }

        val elapsed = System.currentTimeMillis() - startTime
        initialized = true
        plugin.info("Exposed database service initialized in ${elapsed}ms.")
    }

    /**
     * Sets up the query builder. Must be called after SQLManager is available.
     */
    fun initializeQueryBuilder(sqlManager: SQLManager) {
        queryBuilder = QueryBuilder(plugin, sqlManager)
    }

    /**
     * Shuts down the database service, flushing pending operations and closing connections.
     * Waits for pending operations to complete with a timeout.
     */
    fun shutdown() {
        if (!initialized) return

        plugin.info("Shutting down Exposed database service...")

        // Shutdown dispatcher (waits for pending operations with 30s timeout)
        try {
            dispatcher.shutdown(30_000)
            plugin.debug("Database dispatcher shut down")
        } catch (e: Exception) {
            plugin.warning("Error shutting down database dispatcher")
            plugin.print(e)
        }

        // Close HikariCP connection pool (graceful drain)
        try {
            DatabaseFactory.close()
            plugin.debug("HikariCP connection pool closed")
        } catch (e: Exception) {
            plugin.warning("Error closing HikariCP pool")
            plugin.print(e)
        }

        initialized = false
        plugin.info("Exposed database service shut down.")
    }

    /**
     * Returns whether the service is initialized and connected.
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Returns the Exposed Database instance.
     */
    fun getDatabase(): Database = database

    /**
     * Returns the coroutine dispatcher for DB operations.
     */
    fun getDispatcher(): DatabaseDispatcher = dispatcher

    /**
     * Cleans up caches across all repositories.
     * Should be called periodically (e.g., from tick()).
     */
    fun cleanup() {
        blobRepository.cleanup()
        userRepository.cleanup()
    }

    /**
     * Gets the migration status string, or null if not migrating.
     */
    fun getMigrationStatus(): String? {
        return migrationService.getProgressString()
    }
}
