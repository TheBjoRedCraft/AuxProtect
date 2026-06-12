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

    // Query builder
    lateinit var queryBuilder: QueryBuilder
        private set

    private var initialized = false

    /**
     * Initializes the database service: connects, creates schema, and initializes all repositories.
     * This should be called once during plugin startup.
     */
    fun initialize() {
        plugin.info("Initializing Exposed database service...")

        // Create dispatcher
        dispatcher = DatabaseDispatcher(if (config.isMySQL) 4 else 1)

        // Connect to database
        database = DatabaseFactory.create(config)

        // Set up schema registry with prefix
        registry = TableRegistry(config.tablePrefix)

        // Create missing tables and columns via Exposed
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(*registry.getAllTables().toTypedArray())
        }

        // Initialize repositories
        entryRepository = EntryRepository(database, registry)
        blobRepository = BlobRepository(database, registry.invBlobTable, registry.transactionsBlobTable)
        stringIdRepository = StringIdRepository(database, registry.uidsTable, registry.enumIdsTable)
        userRepository = UserRepository(database, registry.longtermTable, registry.uidsTable, registry.userDataPendInvTable, stringIdRepository)
        worldRepository = WorldRepository(database, registry.worldsTable)
        metadataRepository = MetadataRepository(database, registry.versionTable, registry.lastsTable, registry.migrationTasksTable)

        // Load caches
        runBlocking {
            stringIdRepository.initialize()
            worldRepository.initialize()
        }

        // Initialize services
        lookupService = LookupService(plugin, database)
        purgeService = PurgeService(plugin, entryRepository, metadataRepository, config.isMySQL)

        initialized = true
        plugin.info("Exposed database service initialized.")
    }

    /**
     * Sets up the query builder. Must be called after SQLManager is available.
     */
    fun initializeQueryBuilder(sqlManager: SQLManager) {
        queryBuilder = QueryBuilder(plugin, sqlManager)
    }

    /**
     * Shuts down the database service, flushing pending operations and closing connections.
     */
    fun shutdown() {
        if (!initialized) return

        plugin.info("Shutting down Exposed database service...")

        // Shutdown dispatcher (waits for pending operations)
        dispatcher.shutdown(30_000)

        // Close HikariCP connection pool
        DatabaseFactory.close()

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
     */
    fun cleanup() {
        blobRepository.cleanup()
        userRepository.cleanup()
    }
}
