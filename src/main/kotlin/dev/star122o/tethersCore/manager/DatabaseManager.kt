package dev.star122o.tethersCore.manager

import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
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
    val linkId = varchar("link_id", 64).default("")

    override val primaryKey = PrimaryKey(id)
}

object PlacedBlocks : Table() {
    val linkId = varchar("link_id", 64).uniqueIndex()

    @OptIn(ExperimentalUuidApi::class)
    val ownerUuid = uuid("owner_uuid")

    val world = varchar("world", 128)
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val material = varchar("material", 128)

    override val primaryKey = PrimaryKey(linkId)
}

data class PlayerLink @OptIn(ExperimentalUuidApi::class) constructor(
    val uuid: Uuid,
    val linkType: LinkType,
    val linkName: String,
    val linkId: String,
)

data class LinkedBlockRecord @OptIn(ExperimentalUuidApi::class) constructor(
    val linkId: String,
    val ownerUuid: Uuid,
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val material: String,
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
            SchemaUtils.createMissingTablesAndColumns(Players, PlacedBlocks)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun get(uuid: UUID): PlayerLink? = transaction {
        val kotlinUuid = Uuid.parse(uuid.toString())

        Players
            .selectAll()
            .where { Players.uuid eq kotlinUuid }
            .singleOrNull()
            ?.takeIf { it[Players.linkId].isNotBlank() }
            ?.let {
                PlayerLink(
                    uuid = it[Players.uuid],
                    linkType = it[Players.linkType],
                    linkName = it[Players.linkName],
                    linkId = it[Players.linkId],
                )
            }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun getByLinkId(linkId: String): PlayerLink? = transaction {
        Players
            .selectAll()
            .where { Players.linkId eq linkId }
            .singleOrNull()
            ?.takeIf { it[Players.linkId].isNotBlank() }
            ?.let {
                PlayerLink(
                    uuid = it[Players.uuid],
                    linkType = it[Players.linkType],
                    linkName = it[Players.linkName],
                    linkId = it[Players.linkId],
                )
            }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun set(uuid: UUID, linkType: LinkType, linkName: String, linkId: String) {
        transaction {
            val kotlinUuid = Uuid.parse(uuid.toString())
            val previousLinkId = Players
                .selectAll()
                .where { Players.uuid eq kotlinUuid }
                .singleOrNull()
                ?.get(Players.linkId)
                ?.takeIf(String::isNotBlank)

            if (previousLinkId != null && previousLinkId != linkId) {
                PlacedBlocks.deleteWhere { PlacedBlocks.linkId eq previousLinkId }
            }

            val updatedRows = Players.update({ Players.uuid eq kotlinUuid }) {
                it[Players.linkType] = linkType
                it[Players.linkName] = linkName
                it[Players.linkId] = linkId
            }

            if (updatedRows == 0) {
                Players.insert {
                    it[Players.uuid] = kotlinUuid
                    it[Players.linkType] = linkType
                    it[Players.linkName] = linkName
                    it[Players.linkId] = linkId
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun clearIfMatches(uuid: UUID, linkId: String): Boolean = transaction {
        val kotlinUuid = Uuid.parse(uuid.toString())
        PlacedBlocks.deleteWhere { PlacedBlocks.linkId eq linkId }
        Players.deleteWhere {
            (Players.uuid eq kotlinUuid) and (Players.linkId eq linkId)
        } > 0
    }

    @OptIn(ExperimentalUuidApi::class)
    fun isActiveLink(uuid: UUID, linkId: String, linkType: LinkType? = null): Boolean = transaction {
        val kotlinUuid = Uuid.parse(uuid.toString())
        Players
            .selectAll()
            .where { (Players.uuid eq kotlinUuid) and (Players.linkId eq linkId) }
            .singleOrNull()
            ?.let { row -> linkType == null || row[Players.linkType] == linkType }
            ?: false
    }

    @OptIn(ExperimentalUuidApi::class)
    fun upsertPlacedBlock(ownerUuid: UUID, linkId: String, location: Location, material: String) {
        val worldName = location.world?.name ?: return

        transaction {
            val kotlinUuid = Uuid.parse(ownerUuid.toString())
            val updatedRows = PlacedBlocks.update({ PlacedBlocks.linkId eq linkId }) {
                it[PlacedBlocks.ownerUuid] = kotlinUuid
                it[PlacedBlocks.world] = worldName
                it[PlacedBlocks.x] = location.blockX
                it[PlacedBlocks.y] = location.blockY
                it[PlacedBlocks.z] = location.blockZ
                it[PlacedBlocks.material] = material
            }

            if (updatedRows == 0) {
                PlacedBlocks.insert {
                    it[PlacedBlocks.linkId] = linkId
                    it[PlacedBlocks.ownerUuid] = kotlinUuid
                    it[PlacedBlocks.world] = worldName
                    it[PlacedBlocks.x] = location.blockX
                    it[PlacedBlocks.y] = location.blockY
                    it[PlacedBlocks.z] = location.blockZ
                    it[PlacedBlocks.material] = material
                }
            }
        }
    }

    fun getPlacedBlock(linkId: String): LinkedBlockRecord? = transaction {
        PlacedBlocks
            .selectAll()
            .where { PlacedBlocks.linkId eq linkId }
            .singleOrNull()
            ?.toLinkedBlockRecord()
    }

    fun getPlacedBlockAt(location: Location): LinkedBlockRecord? {
        val worldName = location.world?.name ?: return null

        return transaction {
            PlacedBlocks
                .selectAll()
                .where {
                    (PlacedBlocks.world eq worldName) and
                            (PlacedBlocks.x eq location.blockX) and
                            (PlacedBlocks.y eq location.blockY) and
                            (PlacedBlocks.z eq location.blockZ)
                }
                .singleOrNull()
                ?.toLinkedBlockRecord()
        }
    }

    fun clearPlacedBlock(linkId: String): Boolean = transaction {
        PlacedBlocks.deleteWhere { PlacedBlocks.linkId eq linkId } > 0
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun org.jetbrains.exposed.v1.core.ResultRow.toLinkedBlockRecord(): LinkedBlockRecord {
        return LinkedBlockRecord(
            linkId = this[PlacedBlocks.linkId],
            ownerUuid = this[PlacedBlocks.ownerUuid],
            world = this[PlacedBlocks.world],
            x = this[PlacedBlocks.x],
            y = this[PlacedBlocks.y],
            z = this[PlacedBlocks.z],
            material = this[PlacedBlocks.material],
        )
    }
}
