package dev.star122o.tethersCore.manager

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

data class ArtifactIdentity(
    val ownerUuid: UUID,
    val linkId: String,
)

class LinkArtifactManager(
    plugin: JavaPlugin,
) {
    private val linkIdKey = NamespacedKey(plugin, "link_id")
    private val linkOwnerKey = NamespacedKey(plugin, "link_owner")

    fun createIdentity(ownerUuid: UUID): ArtifactIdentity {
        return ArtifactIdentity(ownerUuid, UUID.randomUUID().toString())
    }

    fun readIdentity(item: ItemStack?): ArtifactIdentity? {
        if (item == null || item.type.isAir) {
            return null
        }

        val meta = item.itemMeta ?: return null
        val linkId = meta.persistentDataContainer.get(linkIdKey, PersistentDataType.STRING) ?: return null
        val owner = meta.persistentDataContainer.get(linkOwnerKey, PersistentDataType.STRING) ?: return null

        return runCatching {
            ArtifactIdentity(UUID.fromString(owner), linkId)
        }.getOrNull()
    }

    fun writeIdentity(item: ItemStack, identity: ArtifactIdentity): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.persistentDataContainer.set(linkIdKey, PersistentDataType.STRING, identity.linkId)
        meta.persistentDataContainer.set(linkOwnerKey, PersistentDataType.STRING, identity.ownerUuid.toString())
        item.itemMeta = meta
        return item
    }

    fun clearIdentity(item: ItemStack): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.persistentDataContainer.remove(linkIdKey)
        meta.persistentDataContainer.remove(linkOwnerKey)
        item.itemMeta = meta
        return item
    }

    fun matches(item: ItemStack?, expectedType: String, linkId: String): Boolean {
        val identity = readIdentity(item) ?: return false
        return item?.type?.name == expectedType && identity.linkId == linkId
    }
}
