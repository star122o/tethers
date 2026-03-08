package dev.star122o.tethersCore.manager

import dev.star122o.tethersCore.powers.BlockPower
import dev.star122o.tethersCore.powers.BuffSpec
import dev.star122o.tethersCore.powers.ItemPower
import dev.star122o.tethersCore.powers.ItemPowerSlot
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.lang.reflect.Modifier

enum class LinkType {
    BLOCK,
    ITEM
}

class PowerManager(
    private val plugin: JavaPlugin,
    private val databaseManager: DatabaseManager,
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
            LinkType.BLOCK -> applyBlockPower(player, link.linkName)
            LinkType.ITEM -> applyItemPower(player, link.linkName)
        }
    }

    private fun applyBlockPower(player: Player, linkName: String) {
        val material = Material.matchMaterial(linkName) ?: return
        val power = blockPowers[material.name] ?: return
        if (!isNearBlock(player, power.itemName, power.radius)) return

        applyBuffs(player, power.buffs)
    }

    private fun applyItemPower(player: Player, linkName: String) {
        val material = Material.matchMaterial(linkName) ?: return
        val power = itemPowers[material.name] ?: return
        if (!power.slots.any { slot -> slot.matches(player, power.itemName) }) return

        applyBuffs(player, power.buffs)
    }

    private fun isNearBlock(player: Player, material: Material, radius: Int): Boolean {
        val center = player.location.block

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    if (center.getRelative(x, y, z).type == material) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun applyBuffs(player: Player, buffs: List<BuffSpec>) {
        buffs.forEach { buff ->
            player.addPotionEffect(buff.toPotionEffect(), true)
        }
    }
}
