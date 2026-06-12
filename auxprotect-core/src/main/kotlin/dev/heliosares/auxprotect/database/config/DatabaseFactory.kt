package dev.heliosares.auxprotect.database.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

/**
 * Creates Exposed Database instances from DatabaseConfig.
 * Configures HikariCP datasource with appropriate settings per backend.
 *
 * Performance tuning:
 * - SQLite: single connection (SQLite limitation), WAL mode for concurrent reads
 * - MySQL/MariaDB: configurable pool (default 10), prepared statement cache
 */
object DatabaseFactory {

    private var dataSource: HikariDataSource? = null

    /**
     * Creates and returns an Exposed Database instance configured from the given DatabaseConfig.
     */
    @JvmStatic
    fun create(config: DatabaseConfig): Database {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.getJdbcUrl()
            driverClassName = config.getDriverClassName()

            if (config.isMySQL) {
                username = config.user
                password = config.password
                maximumPoolSize = config.poolSize
                minimumIdle = 2
                idleTimeout = 300_000 // 5 minutes
                maxLifetime = 1_800_000 // 30 minutes
                connectionTimeout = 30_000 // 30 seconds

                // Prepared statement cache for MySQL/MariaDB
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")

                // MySQL performance tuning
                addDataSourceProperty("rewriteBatchedStatements", "true")
                addDataSourceProperty("useLocalSessionState", "true")
                addDataSourceProperty("cacheResultSetMetadata", "true")
                addDataSourceProperty("cacheServerConfiguration", "true")
                addDataSourceProperty("elideSetAutoCommits", "true")
                addDataSourceProperty("maintainTimeStats", "false")
            } else {
                // SQLite: single connection, WAL mode
                maximumPoolSize = 1
                minimumIdle = 1
                connectionTimeout = 30_000
                // Enable WAL mode for better concurrent read performance
                connectionInitSql = "PRAGMA journal_mode=WAL; PRAGMA busy_timeout=30000;"
            }

            poolName = "AuxProtect-HikariPool"
            isAutoCommit = true

            // Leak detection for debugging
            leakDetectionThreshold = 60_000 // 1 minute
        }

        val ds = HikariDataSource(hikariConfig)
        dataSource = ds
        return Database.connect(ds)
    }

    /**
     * Closes the underlying HikariCP datasource gracefully.
     * Waits for active connections to finish before closing.
     */
    @JvmStatic
    fun close() {
        dataSource?.close()
        dataSource = null
    }

    /**
     * Returns whether the datasource is currently active.
     */
    @JvmStatic
    fun isActive(): Boolean = dataSource?.isClosed == false
}
