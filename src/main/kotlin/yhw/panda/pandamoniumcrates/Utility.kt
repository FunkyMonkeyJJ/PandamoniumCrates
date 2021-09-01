package yhw.panda.pandamoniumcrates

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.ChatColor.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class Utility {
    companion object {
        fun checkCrates() {
            try {
                val config = PandamoniumCrates.plugin.config
                val crates = config.getConfigurationSection("crates")
                    ?: return
                PandamoniumCrates.crates.clear()
                crates.getKeys(false).forEach { crate ->
                    // Gets a String in the format of "World|x|y|z"
                    val location: String =
                        config.get("crates.$crate.location") as String
                    val data = location.split('|')
                    val world = PandamoniumCrates.plugin.server.getWorld(
                        data[0]
                    ) ?: return
                    val block = Location(
                        world, data[1].toDouble(), data[2].toDouble(),
                        data[3].toDouble()
                    ).block
                    if (block.state is InventoryHolder)
                        PandamoniumCrates.crates.add(Crate(block, crate))
                }
            } catch (e: Exception) {
                Bukkit.getLogger().info(
                    "Something went wrong while loading the crates!" +
                            "Make sure the locations of your crates are " +
                            "in this format: World|x|y|z"
                )
            }
        }

        /**
         * Clones the given itemStack, but removes the last line of
         * lore. If it contains "Chance: #", then this will return
         * a Pair of the ItemStack without the last lore line and
         * the chance value parsed from the last lore line.
         */
        fun removeLastLore(itemStack: ItemStack): Pair<ItemStack, Double> {
            val cloneItemStack = ItemStack(itemStack.type, itemStack.amount)
            val itemMeta = itemStack.itemMeta ?: return Pair(itemStack, 0.0)
            val itemLore = itemMeta.lore ?: return Pair(itemStack, 0.0)
            if (itemLore.isEmpty()) return Pair(itemStack, 0.0)

            val line = itemLore.removeLast()
            itemMeta.lore = itemLore
            cloneItemStack.itemMeta = itemMeta

            if (!line.contains("Chance: ")) return Pair(itemStack, 0.0)

            return Pair(
                cloneItemStack, line.split(" ")[1].toDouble()
            )
        }

        /**
         * Returns whether the given player has a full inventory or not.
         */
        fun isInvFull(player: Player): Boolean {
            var i = 0
            while (i++ <= 35) {
                val item = player.inventory.getItem(i)
                if (item == null || item.type == Material.AIR)
                    return false
            }
            return true
        }

        fun alert(sender: CommandSender, message: String) {
            if (sender is Player)
                sender.sendMessage(" ${BLUE}[${GREEN}+${BLUE}] $GREEN$message")
            else Bukkit.getLogger().info(message)
        }

        fun error(sender: CommandSender, message: String) {
            if (sender is Player)
                sender.sendMessage(
                    " ${DARK_RED}[${RED}!!!${DARK_RED}] $RED$message"
                )
            else Bukkit.getLogger().info(message)
        }
    }
}
