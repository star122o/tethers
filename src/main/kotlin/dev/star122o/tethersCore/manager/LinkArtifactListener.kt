package dev.star122o.tethersCore.manager

import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.uuid.ExperimentalUuidApi

class LinkArtifactListener(
    private val plugin: JavaPlugin,
    private val databaseManager: DatabaseManager,
    private val artifactManager: LinkArtifactManager,
) : Listener {
    private val destructiveItemDamageCauses = setOf(
        EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
        EntityDamageEvent.DamageCause.CONTACT,
        EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
        EntityDamageEvent.DamageCause.FIRE,
        EntityDamageEvent.DamageCause.FIRE_TICK,
        EntityDamageEvent.DamageCause.HOT_FLOOR,
        EntityDamageEvent.DamageCause.LAVA,
        EntityDamageEvent.DamageCause.VOID,
    )

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val identity = artifactManager.readIdentity(event.itemInHand) ?: return
        val link = databaseManager.get(identity.ownerUuid) ?: return
        if (link.linkId != identity.linkId || link.linkType != LinkType.BLOCK) {
            return
        }

        databaseManager.upsertPlacedBlock(
            identity.ownerUuid,
            identity.linkId,
            event.blockPlaced.location,
            event.blockPlaced.type.name
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    @EventHandler(ignoreCancelled = true)
    fun onBlockDropItems(event: BlockDropItemEvent) {
        val placedBlock = databaseManager.getPlacedBlockAt(event.block.location) ?: return
        val ownerUuid = UUID.fromString(placedBlock.ownerUuid.toString())
        val link = databaseManager.get(ownerUuid)
        if (link == null || link.linkId != placedBlock.linkId || link.linkType != LinkType.BLOCK) {
            databaseManager.clearPlacedBlock(placedBlock.linkId)
            return
        }

        val linkedDrop = event.items.firstOrNull { item ->
            item.itemStack.type.name == link.linkName
        }

        if (linkedDrop == null) {
            databaseManager.clearIfMatches(ownerUuid, placedBlock.linkId)
            return
        }

        val updated =
            artifactManager.writeIdentity(linkedDrop.itemStack.clone(), ArtifactIdentity(ownerUuid, placedBlock.linkId))
        linkedDrop.itemStack = updated
        databaseManager.clearPlacedBlock(placedBlock.linkId)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.isDropItems) {
            return
        }

        handlePlacedBlockDestroyed(event.block)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().forEach(::handlePlacedBlockDestroyed)
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().forEach(::handlePlacedBlockDestroyed)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        handlePlacedBlockDestroyed(event.block)
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemDespawn(event: ItemDespawnEvent) {
        clearLinkForItem(event.entity)
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemDamaged(event: EntityDamageEvent) {
        val itemEntity = event.entity as? Item ?: return
        if (event.cause !in destructiveItemDamageCauses) {
            return
        }

        val identity = artifactManager.readIdentity(itemEntity.itemStack) ?: return
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (!itemEntity.isValid) {
                databaseManager.clearIfMatches(identity.ownerUuid, identity.linkId)
            }
        })
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun handlePlacedBlockDestroyed(block: Block) {
        val placedBlock = databaseManager.getPlacedBlockAt(block.location) ?: return
        databaseManager.clearIfMatches(UUID.fromString(placedBlock.ownerUuid.toString()), placedBlock.linkId)
    }

    private fun clearLinkForItem(itemEntity: Item) {
        val identity = artifactManager.readIdentity(itemEntity.itemStack) ?: return
        databaseManager.clearIfMatches(identity.ownerUuid, identity.linkId)
    }
}
