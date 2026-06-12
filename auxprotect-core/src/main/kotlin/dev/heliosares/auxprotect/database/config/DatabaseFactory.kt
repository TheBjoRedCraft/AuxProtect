package dev.heliosares.auxprotect.database.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

/**
 * Creates Exposed Database instances from DatabaseConfig.
 * Configures HikariCP datasource with appropriate settings per backend.
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
                maxLifetime = 600_000 // 10 minutes
                connectionTimeout = 30_000 // 30 seconds
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
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
        }

        val ds = HikariDataSource(hikariConfig)
        dataSource = ds
        return Database.connect(ds)
    }

    /**
     * Closes the underlying HikariCP datasource if open.
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
