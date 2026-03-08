package dev.star122o.tethersCore.manager

import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object Players : Table() {
    val id = integer("id").autoIncrement()

    @OptIn(ExperimentalUuidApi::class)
    val uuid = uuid("uuid").uniqueIndex()

    val linkType = enumeration("link_type", LinkType::class)
    val linkName = varchar("link_name", 128)
    override val primaryKey = PrimaryKey(id)
}

data class PlayerLink @OptIn(ExperimentalUuidApi::class) constructor(
    val uuid: Uuid,
    val linkType: LinkType,
    val linkName: String,
)

class DatabaseManager(private val plugin: JavaPlugin) {
    fun connect() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        val dbFile = File(plugin.dataFolder, "tethers.db")
        Database.connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Players)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun get(uuid: UUID): PlayerLink? = transaction {
        val kotlinUuid = Uuid.parse(uuid.toString())

        Players
            .selectAll()
            .where { Players.uuid eq kotlinUuid }
            .singleOrNull()
            ?.let {
                PlayerLink(
                    uuid = it[Players.uuid],
                    linkType = it[Players.linkType],
                    linkName = it[Players.linkName],
                )
            }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun set(uuid: UUID, linkType: LinkType, linkName: String) {
        transaction {
            val kotlinUuid = Uuid.parse(uuid.toString())

            val updatedRows = Players.update({ Players.uuid eq kotlinUuid }) {
                it[Players.linkType] = linkType
                it[Players.linkName] = linkName
            }

            if (updatedRows == 0) {
                Players.insert {
                    it[Players.uuid] = kotlinUuid
                    it[Players.linkType] = linkType
                    it[Players.linkName] = linkName
                }
            }
        }
    }
}