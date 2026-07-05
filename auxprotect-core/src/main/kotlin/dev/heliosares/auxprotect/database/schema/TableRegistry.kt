package dev.heliosares.auxprotect.database.schema

import dev.heliosares.auxprotect.database.Table
import org.jetbrains.exposed.sql.Table as ExposedTable

/**
 * Maps the existing Java Table enum entries to their corresponding Exposed table definitions.
 * Supports table prefix configuration.
 */
class TableRegistry(private val prefix: String) {

    // Entry tables (AP_ENTRIES)
    val mainTable = AuxProtectMainTable(prefix)
    val spamTable = AuxProtectSpamTable(prefix)
    val longtermTable = AuxProtectLongtermTable(prefix)
    val abandonedTable = AuxProtectAbandonedTable(prefix)
    val xrayTable = AuxProtectXrayTable(prefix)
    val inventoryTable = AuxProtectInventoryTable(prefix)
    val commandsTable = AuxProtectCommandsTable(prefix)
    val chatTable = AuxProtectChatTable(prefix)
    val positionTable = AuxProtectPositionTable(prefix)
    val transactionsTable = AuxProtectTransactionsTable(prefix)
    val apiTable = AuxProtectApiTable(prefix)

    // Utility tables
    val uidsTable = UidsTable(prefix)
    val worldsTable = WorldsTable(prefix)
    val invDiffTable = InvDiffTable(prefix)
    val invBlobTable = InvBlobTable(prefix)
    val transactionsBlobTable = TransactionsBlobTable(prefix)
    val invDiffBlobTable = InvDiffBlobTable(prefix)
    val lastsTable = LastsTable(prefix)
    val apiActionsTable = ApiActionsTable(prefix)
    val versionTable = VersionTable(prefix)
    val migrationTasksTable = MigrationTasksTable(prefix)
    val enumIdsTable = EnumIdsTable(prefix)
    val userDataPendInvTable = UserDataPendInvTable(prefix)

    /**
     * Maps a Java Table enum value to its corresponding Exposed table object.
     * Returns null for tables that don't have a direct Exposed mapping.
     */
    fun getExposedTable(table: Table): ExposedTable? {
        return when (table) {
            Table.AUXPROTECT_MAIN -> mainTable
            Table.AUXPROTECT_SPAM -> spamTable
            Table.AUXPROTECT_LONGTERM -> longtermTable
            Table.AUXPROTECT_ABANDONED -> abandonedTable
            Table.AUXPROTECT_XRAY -> xrayTable
            Table.AUXPROTECT_INVENTORY -> inventoryTable
            Table.AUXPROTECT_COMMANDS -> commandsTable
            Table.AUXPROTECT_CHAT -> chatTable
            Table.AUXPROTECT_POSITION -> positionTable
            Table.AUXPROTECT_TRANSACTIONS -> transactionsTable
            Table.AUXPROTECT_API -> apiTable
            Table.AUXPROTECT_INVDIFF -> invDiffTable
            Table.AUXPROTECT_UIDS -> uidsTable
            Table.AUXPROTECT_WORLDS -> worldsTable
            Table.AUXPROTECT_API_ACTIONS -> apiActionsTable
            Table.AUXPROTECT_VERSION -> versionTable
            Table.AUXPROTECT_MIGRATION_TASKS -> migrationTasksTable
            Table.AUXPROTECT_INVBLOB -> invBlobTable
            Table.AUXPROTECT_LASTS -> lastsTable
            Table.AUXPROTECT_INVDIFFBLOB -> invDiffBlobTable
            Table.AUXPROTECT_USERDATA_PENDINV -> userDataPendInvTable
            Table.AUXPROTECT_TRANSACTIONS_BLOB -> transactionsBlobTable
            Table.AUXPROTECT_ENUM_IDS -> enumIdsTable
        }
    }

    /**
     * Returns all entry tables (those with AP_ENTRIES characteristic).
     */
    fun getEntryTables(): List<ExposedTable> {
        return listOf(
            mainTable, spamTable, longtermTable, abandonedTable,
            xrayTable, inventoryTable, commandsTable, chatTable,
            positionTable, transactionsTable, apiTable
        )
    }

    /**
     * Returns all utility tables.
     */
    fun getUtilityTables(): List<ExposedTable> {
        return listOf(
            uidsTable, worldsTable, invDiffTable, invBlobTable,
            transactionsBlobTable, invDiffBlobTable, lastsTable,
            apiActionsTable, versionTable, migrationTasksTable,
            enumIdsTable, userDataPendInvTable
        )
    }

    /**
     * Returns all tables.
     */
    fun getAllTables(): List<ExposedTable> {
        return getEntryTables() + getUtilityTables()
    }
}
