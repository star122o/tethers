package dev.star122o.tethersCore.powers

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

data class BuffSpec(
    val type: PotionEffectType,
    val amplifier: Int,
    val durationTicks: Int = 60,
    val ambient: Boolean = false,
    val particles: Boolean = true,
    val icon: Boolean = true,
) {
    fun toPotionEffect(): PotionEffect {
        return PotionEffect(type, durationTicks, amplifier, ambient, particles, icon)
    }
}

abstract class BasePower {
    abstract val itemName: Material
    abstract val buffs: List<BuffSpec>
}

abstract class BlockPower : BasePower() {
    abstract val radius: Int
}

abstract class ItemPower : BasePower() {
    abstract val slots: Set<ItemPowerSlot>
}

enum class ItemPowerSlot {
    MAIN_HAND,
    OFF_HAND,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    HOTBAR,
    INVENTORY,
    ANY,
    ;

    fun matches(player: Player, material: Material): Boolean {
        val inventory = player.inventory

        return when (this) {
            MAIN_HAND -> inventory.itemInMainHand.type == material
            OFF_HAND -> inventory.itemInOffHand.type == material
            HELMET -> inventory.helmet?.type == material
            CHESTPLATE -> inventory.chestplate?.type == material
            LEGGINGS -> inventory.leggings?.type == material
            BOOTS -> inventory.boots?.type == material
            HOTBAR -> (0..8).any { slot -> inventory.getItem(slot)?.type == material }
            INVENTORY -> inventory.contents.any { it?.type == material }
            ANY -> {
                MAIN_HAND.matches(player, material)
                        || OFF_HAND.matches(player, material)
                        || HELMET.matches(player, material)
                        || CHESTPLATE.matches(player, material)
                        || LEGGINGS.matches(player, material)
                        || BOOTS.matches(player, material)
                        || HOTBAR.matches(player, material)
                        || INVENTORY.matches(player, material)
            }
        }
    }
}
