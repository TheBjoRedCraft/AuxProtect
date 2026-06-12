package dev.heliosares.auxprotect.database.schema

import org.jetbrains.exposed.sql.Table as ExposedTable

// =============================================================================
// Utility tables
// =============================================================================

/** AUXPROTECT_UIDS — string-to-int ID mapping */
class UidsTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_uids") {
    val id = integer("id").autoIncrement()
    val value = varchar("value", 255).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

/** AUXPROTECT_WORLDS — world name-to-ID mapping */
class WorldsTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_worlds") {
    val name = varchar("name", 255)
    val wid = short("wid")
}

/** AUXPROTECT_INVDIFF — inventory diff tracking */
class InvDiffTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_invdiff") {
    val time = long("time")
    val uid = integer("uid")
    val slot = integer("slot")
    val qty = integer("qty")
    val blobid = long("blobid").nullable()
    val damage = integer("damage")
}

/** AUXPROTECT_INVBLOB — inventory blob storage with dedup */
class InvBlobTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_invblob") {
    val blobid = long("blobid")
    val ablob = blob("ablob")
    val hash = integer("hash")

    override val primaryKey = PrimaryKey(blobid)
}

/** AUXPROTECT_TRANSACTIONS_BLOB — transaction blob storage with dedup */
class TransactionsBlobTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_transactions_blob") {
    val blobid = long("blobid")
    val ablob = blob("ablob")
    val hash = integer("hash")

    override val primaryKey = PrimaryKey(blobid)
}

/** AUXPROTECT_INVDIFFBLOB — inventory diff blob storage */
class InvDiffBlobTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_invdiffblob") {
    val blobid = long("blobid")
    val ablob = blob("ablob")
    val hash = integer("hash")

    override val primaryKey = PrimaryKey(blobid)
}

/** AUXPROTECT_LASTS — key-value store for last timestamps */
class LastsTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_lasts") {
    val name = short("name")
    val value = long("value").nullable()

    override val primaryKey = PrimaryKey(name)
}

/** AUXPROTECT_API_ACTIONS — custom API action definitions */
class ApiActionsTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_api_actions") {
    val name = varchar("name", 255)
    val nid = short("nid")
    val pid = short("pid")
    val ntext = varchar("ntext", 255)
    val ptext = varchar("ptext", 255).nullable()
    val owner = varchar("owner", 255)
    val created = long("created")
}

/** AUXPROTECT_VERSION — version tracking */
class VersionTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_version") {
    val time = long("time")
    val version = integer("version")
}

/** AUXPROTECT_MIGRATION_TASKS — tracks completed migration tasks */
class MigrationTasksTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_migration_tasks") {
    val id = integer("id")

    override val primaryKey = PrimaryKey(id)
}

/** AUXPROTECT_ENUM_IDS — enum string-to-int ID mapping */
class EnumIdsTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_enum_ids") {
    val id = integer("id").autoIncrement()
    val value = varchar("value", 255).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

/** AUXPROTECT_USERDATA_PENDINV — pending inventory data per user */
class UserDataPendInvTable(prefix: String = PrefixedTable.tablePrefix) :
    PrefixedTable("${prefix}auxprotect_userdata_pendinv") {
    val time = long("time")
    val uid = integer("uid")
    val pending = blob("pending")

    override val primaryKey = PrimaryKey(uid)
}
