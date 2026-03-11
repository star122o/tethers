package dev.star122o.tethersCore

import dev.star122o.tethersCore.manager.*
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class TethersCore : JavaPlugin() {
    private lateinit var databaseManager: DatabaseManager
    private lateinit var artifactManager: LinkArtifactManager
    private lateinit var powerManager: PowerManager

    override fun onEnable() {
        logger.info("Enabling Tethers Core")

        databaseManager = DatabaseManager(this)
        databaseManager.connect()

        artifactManager = LinkArtifactManager(this)
        powerManager = PowerManager(this, databaseManager, artifactManager)
        powerManager.start()

        server.pluginManager.registerEvents(
            LinkArtifactListener(this, databaseManager, artifactManager),
            this,
        )
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

        val item = sender.inventory.itemInMainHand
        val material = item.type
        if (material == Material.AIR) {
            sender.sendMessage("Hold an item in your main hand first.")
            return true
        }

        if (item.amount != 1) {
            sender.sendMessage("Hold exactly one item to link it.")
            return true
        }

        val linkType = if (material.isBlock) LinkType.BLOCK else LinkType.ITEM
        val identity = artifactManager.createIdentity(sender.uniqueId)
        artifactManager.writeIdentity(item, identity)
        sender.inventory.setItemInMainHand(item)
        databaseManager.set(sender.uniqueId, linkType, material.name, identity.linkId)
        sender.sendMessage("Linked ${material.name.lowercase()} with id ${identity.linkId}.")
        return true
    }
}
