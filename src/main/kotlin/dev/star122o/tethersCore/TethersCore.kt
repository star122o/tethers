package dev.star122o.tethersCore

import dev.star122o.tethersCore.manager.DatabaseManager
import dev.star122o.tethersCore.manager.LinkType
import dev.star122o.tethersCore.manager.PowerManager
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class TethersCore : JavaPlugin() {
    private lateinit var databaseManager: DatabaseManager
    private lateinit var powerManager: PowerManager

    override fun onEnable() {
        logger.info("Enabling Tethers Core")

        databaseManager = DatabaseManager(this)
        databaseManager.connect()

        powerManager = PowerManager(this, databaseManager)
        powerManager.start()
    }

    override fun onDisable() {
        if (::powerManager.isInitialized) {
            powerManager.stop()
        }

        logger.info("Shutting down Tethers Core")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!command.name.equals("link", ignoreCase = true)) {
            return false
        }

        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }

        val material = sender.inventory.itemInMainHand.type
        if (material == Material.AIR) {
            sender.sendMessage("Hold a block in your main hand first.")
            return true
        }

        val linkType = if (material.isBlock) LinkType.BLOCK else LinkType.ITEM
        databaseManager.set(sender.uniqueId, linkType, material.name)
        sender.sendMessage("Linked you to ${linkType.name.lowercase()} ${material.name}.")
        return true
    }
}
