package dev.heliosares.auxprotect.database.config

import dev.heliosares.auxprotect.core.APConfig
import java.io.File

/**
 * Holds all database connection parameters.
 */
data class DatabaseConfig(
    val isMySQL: Boolean,
    val host: String = "localhost",
    val port: String = "3306",
    val database: String = "database",
    val user: String = "",
    val password: String = "",
    val sqliteFile: File? = null,
    val tablePrefix: String = "",
    val poolSize: Int = 10,
    val isIndexing: Boolean = true
) {
    companion object {
        /**
         * Creates a DatabaseConfig from the existing APConfig.
         */
        @JvmStatic
        fun fromAPConfig(config: APConfig, sqliteFile: File?): DatabaseConfig {
            val isMySQL = config.host != null && config.host.isNotEmpty()
            val prefix = config.config.getString("MySQL.table-prefix").orElse("").let { p ->
                if (p.isEmpty()) "" else {
                    val sanitized = p.replace(" ", "_")
                    if (sanitized.endsWith("_")) sanitized else "${sanitized}_"
                }
            }
            return DatabaseConfig(
                isMySQL = isMySQL,
                host = config.host ?: "localhost",
                port = config.port ?: "3306",
                database = config.database ?: "database",
                user = config.user ?: "",
                password = config.pass ?: "",
                sqliteFile = sqliteFile,
                tablePrefix = prefix,
                poolSize = if (isMySQL) 10 else 1,
                isIndexing = config.isIndexing
            )
        }
    }

    /**
     * Returns the JDBC URL for this configuration.
     */
    fun getJdbcUrl(): String {
        return if (isMySQL) {
            "jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true"
        } else {
            "jdbc:sqlite:${sqliteFile?.absolutePath}"
        }
    }

    /**
     * Returns the JDBC driver class name.
     */
    fun getDriverClassName(): String {
        return if (isMySQL) {
            "com.mysql.cj.jdbc.Driver"
        } else {
            "org.sqlite.JDBC"
        }
    }
}
