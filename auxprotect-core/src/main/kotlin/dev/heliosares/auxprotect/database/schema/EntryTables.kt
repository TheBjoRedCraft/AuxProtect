package dev.heliosares.auxprotect.database.schema

import org.jetbrains.exposed.sql.Table as ExposedTable

/**
 * Base class for all AuxProtect tables that support a configurable table prefix.
 */
abstract class PrefixedTable(name: String) : ExposedTable(name) {
    companion object {
        /** Global table prefix, set once during initialization. */
        @JvmStatic
        var tablePrefix: String = ""
    }
}

// =============================================================================
// Main AP_ENTRIES tables
// =============================================================================

/** AUXPROTECT_MAIN — time, uid, action_id, world_id, x, y, z, target_id, data */
class AuxProtectMainTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_main") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")
    val data = text("data").nullable()

    override val primaryKey = PrimaryKey(time)

}

/** AUXPROTECT_SPAM — same structure as main */
class AuxProtectSpamTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_spam") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")
    val data = text("data").nullable()

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_LONGTERM — time, uid, action_id, target_id */
class AuxProtectLongtermTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_longterm") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val targetId = integer("target_id")

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_ABANDONED — time, uid, action_id, world_id, x, y, z, target_id */
class AuxProtectAbandonedTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_abandoned") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_XRAY — time, uid, world_id, x, y, z, target_id, rating, data */
class AuxProtectXrayTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_xray") {
    val time = long("time")
    val uid = integer("uid")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")
    val rating = short("rating")
    val data = text("data").nullable()

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_INVENTORY — time, uid, action_id, world_id, x, y, z, target_id, data, blobid, qty, damage */
class AuxProtectInventoryTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_inventory") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")
    val data = text("data").nullable()
    val blobid = long("blobid").nullable()
    val qty = integer("qty").nullable()
    val damage = integer("damage").nullable()

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_COMMANDS — time, uid, world_id, x, y, z, target (string) */
class AuxProtectCommandsTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_commands") {
    val time = long("time")
    val uid = integer("uid")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val target = text("target").nullable()

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_CHAT — time, uid, world_id, x, y, z, target_id, data */
class AuxProtectChatTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_chat") {
    val time = long("time")
    val uid = integer("uid")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")
    val data = text("data").nullable()

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_POSITION — time, uid, action_id, world_id, x, y, z, increment, pitch, yaw, target_id, ablob */
class AuxProtectPositionTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_position") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val increment = byte("increment")
    val pitch = short("pitch")
    val yaw = short("yaw")
    val targetId = integer("target_id")
    val ablob = blob("ablob").nullable()

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_TRANSACTIONS — time, uid, action_id, world_id, x, y, z, target_id, data, blobid, quantity, cost, balance, target_id2 */
class AuxProtectTransactionsTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_transactions") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")
    val data = text("data").nullable()
    val blobid = long("blobid").nullable()
    val quantity = short("quantity")
    val cost = decimal("cost", 12, 3)
    val balance = decimal("balance", 15, 3)
    val targetId2 = integer("target_id2")

    override val primaryKey = PrimaryKey(time)
}

/** AUXPROTECT_API — same as main */
class AuxProtectApiTable(prefix: String = tablePrefix) :
    PrefixedTable("${prefix}auxprotect_api") {
    val time = long("time")
    val uid = integer("uid")
    val actionId = short("action_id")
    val worldId = short("world_id")
    val x = integer("x")
    val y = short("y")
    val z = integer("z")
    val targetId = integer("target_id")
    val data = text("data").nullable()

    override val primaryKey = PrimaryKey(time)
}
