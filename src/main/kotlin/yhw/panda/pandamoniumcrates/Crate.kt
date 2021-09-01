package yhw.panda.pandamoniumcrates

import org.bukkit.Bukkit
import org.bukkit.ChatColor.GOLD
import org.bukkit.Material.AIR
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import yhw.panda.pandamoniumcrates.Utility.Companion.removeLastLore

data class Crate(val block: Block, var name: String) {
    var isBeingEdited = false
    var hashItems = mutableMapOf<ItemStack, Double>()
    val location = block.location
    val displayName = name[0].toUpperCase() + name.substring(1)

    /**
     * Saves the given newItems from the crate Inventory
     * and adds them into the hashItems. If the hashItems
     * doesn't contain the ItemStack already, add it in
     * with a default percent chance of 0.
     */
    fun saveItems(newItems: Array<ItemStack>) {
        val config = PandamoniumCrates.plugin.config
        config.set("crates.$name.items", null)

        if (hashItems.isEmpty()) {
            for (item in newItems) {
                if (item == null) continue
                val pair = removeLastLore(item)
                hashItems[pair.first] = pair.second
            }
        } else {
            // HashMap of everything that is in the new inventory
            val newHashItems = HashMap<ItemStack, Double>()
            for (hash in hashItems)
                for (item in newItems) {
                    if (item == null) continue
                    val cloneItem = removeLastLore(item).first
                    if (cloneItem.toString() == hash.key.toString()) {
                        newHashItems[hash.key] = hash.value
                        break
                    }
                }
            hashItems.clear()
            for (hash in newHashItems) hashItems[hash.key] = hash.value

            // Adds everything that isn't in the new inventory
            for (item in newItems) {
                if (item == null) continue
                var cloneItem = ItemStack(AIR)
                var contains = false
                for (hash in hashItems) {
                    cloneItem = removeLastLore(item).first
                    val hashItem = removeLastLore(hash.key).first
                    if (cloneItem.toString() == hashItem.toString()) {
                        contains = true
                        break
                    }
                }
                if (!contains && cloneItem.type != AIR) {
                    hashItems[cloneItem] = 0.0
                    Bukkit.getLogger().info("Setting $cloneItem to 0.0")
                }
            }
        }
        outputItems()
    }

    /**
     * Stores the hashItems HashMap contents to the config.yml file.
     */
    fun outputItems() {
        val config = PandamoniumCrates.plugin.config
        var i = 0
        for (item in hashItems) {
            if (item.key == null || item.key.type == AIR) continue
            config.set("crates.$name.items.$i.item", item.key)
            config.set("crates.$name.items.${i++}.chance", item.value)
        }
        PandamoniumCrates.plugin.saveConfig()
    }

    /**
     * Loads the ItemStack 'item' and the Double 'chance' field
     * from the config.yml file into the hashItems HashMap for
     * later usage. Will only read in the first 100 items/chances.
     */
    fun loadItems() {
        val config = PandamoniumCrates.plugin.config
        if (hashItems.isEmpty())
            for (i in 0 until 100) {
                if (config.get("crates.$name.items.$i") == null) continue
                val item = config.get("crates.$name.items.$i.item")
                if (item == null || item !is ItemStack) continue
                val percent = config.get("crates.$name.items.$i.chance")
                if (percent == null || percent !is Double) continue
                hashItems[item] = percent
            }
    }

    /**
     * Clones the ItemStacks from the hashItems HashMap and adds
     * a new lore line in the format of: Chance: # (# being the
     * Double value from hashItems).
     */
    fun inputItems(inv: Inventory, removeCommands: Boolean) {
        val copyHashItems = HashMap<ItemStack, Double>()
        for (hash in hashItems) copyHashItems[hash.key] = hash.value

        var i = 0
        for (item in copyHashItems.keys) {
            if (item == null) continue
            val cloneItem = ItemStack(item.type, item.amount)
            val meta = item.itemMeta ?: continue
            val lore = meta.lore

            if (lore == null || lore.isEmpty())
                meta.lore = mutableListOf(
                    "${GOLD}Chance: ${hashItems[item] ?: 0.0}"
                )
            else {
                // Removes any commands from the item
                if (removeCommands) {
                    val copyLore = mutableListOf<String>()
                    for (line in lore) copyLore.add(line)
                    for (line in copyLore)
                        if (line.length > 3 &&
                            line.substring(2).startsWith("/")
                        )
                            lore.remove(line)
                }

                // Solution to remove any duplicates
                var brokenItem = false
                for (line in lore)
                    if (line.contains("Chance: ")) {
                        brokenItem = true
                        break
                    }
                if (!brokenItem) {
                    lore.add(
                        "${GOLD}Chance: ${hashItems[item] ?: 0.0}"
                    )
                    meta.lore = lore
                }
            }
            cloneItem.itemMeta = meta
            inv.setItem(i++, cloneItem)
        }
    }

    /**
     * Generates a random ItemStack based on the chance values
     * from hashItems. If the total chance goes over 100, the
     * chance for the current ItemStack key will be adjusted
     * to stay below 100 and the remaining ItemStack keys will
     * be skipped.
     *
     * @return a Pair<ItemStack, Boolean> for the ItemStack that
     * was won, and the Boolean of whether a command was found or not.
     */
    fun open(opener: Player, fake: Boolean): Pair<ItemStack, Boolean> {
        var totalChance = 0.0
        var overflow = false
        val chanceArray = arrayListOf<Double>()
        val itemArray = arrayListOf<ItemStack>()
        val copyHashItems = mutableMapOf<ItemStack, Double>()
        for (hash in hashItems) copyHashItems[hash.key] = hash.value
        for (hash in copyHashItems) {
            var chance = hash.value
            if (totalChance + hash.value >= 100) {
                overflow = true
                chance = 100 - totalChance
            }
            totalChance += chance
            itemArray.add(ItemStack(hash.key))
            chanceArray.add(totalChance)
            if (overflow) break
        }

        chanceArray[chanceArray.lastIndex] = 100.0

        val randomChance = Math.random() * 100
        for ((i, item) in itemArray.withIndex()) {
            val chance = chanceArray[i]
            if (randomChance > chance) continue
            if (randomChance <= chance) {
                val meta = item.itemMeta ?: return Pair(item, false)
                val lore = meta.lore ?: mutableListOf()

                // Remove any command lore lines and run
                // them instead of giving the item
                val copyLore = mutableListOf<String>()
                var commandFound = false
                for (line in lore) copyLore.add(line)
                for (line in copyLore)
                    if (line.length > 3 &&
                        line.substring(2).startsWith("/")
                    ) {
                        lore.remove(line)
                        if (!fake)
                            Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                line.replace("{player}", opener.name)
                                    .substring(3)
                            )
                        commandFound = true
                    }
                lore.add("${GOLD}Chance: ${copyHashItems[item]}")
                meta.lore = lore
                item.itemMeta = meta
                return if (commandFound) Pair(item, true)
                else Pair(item, false)
            }
        }

        return Pair(ItemStack(AIR), false)
    }

    override fun toString(): String {
        return displayName
    }
}
