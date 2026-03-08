package dev.star122o.tethersCore

import dev.star122o.tethersCore.manager.DatabaseManager
import dev.star122o.tethersCore.manager.PowerManager
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
}
