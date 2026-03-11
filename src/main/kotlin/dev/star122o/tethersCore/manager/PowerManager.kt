package dev.star122o.tethersCore.manager

import dev.star122o.tethersCore.powers.BlockPower
import dev.star122o.tethersCore.powers.BuffSpec
import dev.star122o.tethersCore.powers.ItemPower
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.lang.reflect.Modifier
import kotlin.math.abs

enum class LinkType {
    BLOCK,
    ITEM,
}

class PowerManager(
    private val plugin: JavaPlugin,
    private val databaseManager: DatabaseManager,
    private val artifactManager: LinkArtifactManager,
) {
    private val blockPowers = mutableMapOf<String, BlockPower>()
    private val itemPowers = mutableMapOf<String, ItemPower>()
    private var task: BukkitTask? = null

    fun start() {
        loadPowers()

        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            Bukkit.getOnlinePlayers().forEach(::applyLinkedPower)
        }, 20L, 20L)

        plugin.logger.info("Loaded ${blockPowers.size} block power(s) and ${itemPowers.size} item power(s)")
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun loadPowers() {
        loadBlockPowers()
        loadItemPowers()
    }

    private fun loadBlockPowers() {
        val reflections = Reflections("dev.star122o.tethersCore.powers.blocks", Scanners.SubTypes)

        reflections.getSubTypesOf(BlockPower::class.java)
            .filterNot { Modifier.isAbstract(it.modifiers) }
            .forEach { type ->
                runCatching { type.getDeclaredConstructor().newInstance() }
                    .onSuccess { power ->
                        blockPowers[power.itemName.name] = power
                    }
                    .onFailure { err ->
                        plugin.logger.warning("Failed to load block power ${type.name}: ${err.message}")
                    }
            }
    }

    private fun loadItemPowers() {
        val reflections = Reflections("dev.star122o.tethersCore.powers.items", Scanners.SubTypes)

        reflections.getSubTypesOf(ItemPower::class.java)
            .filterNot { Modifier.isAbstract(it.modifiers) }
            .forEach { type ->
                runCatching { type.getDeclaredConstructor().newInstance() }
                    .onSuccess { power ->
                        itemPowers[power.itemName.name] = power
                    }
                    .onFailure { err ->
                        plugin.logger.warning("Failed to load item power ${type.name}: ${err.message}")
                    }
            }
    }

    private fun applyLinkedPower(player: Player) {
        val link = databaseManager.get(player.uniqueId) ?: return

        when (link.linkType) {
            LinkType.BLOCK -> applyBlockPower(player, link)
            LinkType.ITEM -> applyItemPower(player, link)
        }
    }

    private fun applyBlockPower(player: Player, link: PlayerLink) {
        val material = Material.matchMaterial(link.linkName) ?: return
        val power = blockPowers[material.name] ?: return
        val placedBlock = databaseManager.getPlacedBlock(link.linkId) ?: return
        val world = Bukkit.getWorld(placedBlock.world)

        if (world == null) {
            databaseManager.clearIfMatches(player.uniqueId, link.linkId)
            return
        }

        val block = world.getBlockAt(placedBlock.x, placedBlock.y, placedBlock.z)
        if (block.type != material || placedBlock.material != material.name) {
            databaseManager.clearIfMatches(player.uniqueId, link.linkId)
            return
        }

        if (player.world.uid != world.uid) {
            return
        }

        if (!isNearBlock(player, placedBlock.x, placedBlock.y, placedBlock.z, power.radius)) {
            return
        }

        applyBuffs(player, power.buffs)
    }

    private fun applyItemPower(player: Player, link: PlayerLink) {
        val material = Material.matchMaterial(link.linkName) ?: return
        val power = itemPowers[material.name] ?: return
        if (!power.slots.any { slot ->
                slot.matches(player) { item ->
                    artifactManager.matches(item, material.name, link.linkId)
                }
            }) {
            return
        }

        applyBuffs(player, power.buffs)
    }

    private fun isNearBlock(player: Player, x: Int, y: Int, z: Int, radius: Int): Boolean {
        return abs(player.location.blockX - x) <= radius
                && abs(player.location.blockY - y) <= radius
                && abs(player.location.blockZ - z) <= radius
    }

    private fun applyBuffs(player: Player, buffs: List<BuffSpec>) {
        buffs.forEach { buff ->
            player.addPotionEffect(buff.toPotionEffect(), true)
        }
    }
}
