package dev.star122o.tethersCore.powers

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

    fun matches(player: Player, material: Material): Boolean = matches(player) { it?.type == material }

    fun matches(player: Player, matcher: (ItemStack?) -> Boolean): Boolean {
        val inventory = player.inventory

        return when (this) {
            MAIN_HAND -> matcher(inventory.itemInMainHand)
            OFF_HAND -> matcher(inventory.itemInOffHand)
            HELMET -> matcher(inventory.helmet)
            CHESTPLATE -> matcher(inventory.chestplate)
            LEGGINGS -> matcher(inventory.leggings)
            BOOTS -> matcher(inventory.boots)
            HOTBAR -> (0..8).any { slot -> matcher(inventory.getItem(slot)) }
            INVENTORY -> inventory.contents.any(matcher)
            ANY -> {
                MAIN_HAND.matches(player, matcher)
                        || OFF_HAND.matches(player, matcher)
                        || HELMET.matches(player, matcher)
                        || CHESTPLATE.matches(player, matcher)
                        || LEGGINGS.matches(player, matcher)
                        || BOOTS.matches(player, matcher)
                        || HOTBAR.matches(player, matcher)
                        || INVENTORY.matches(player, matcher)
            }
        }
    }
}
