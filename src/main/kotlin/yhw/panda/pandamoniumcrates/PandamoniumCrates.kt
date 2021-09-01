package yhw.panda.pandamoniumcrates

import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.Material.AIR
import org.bukkit.Material.TRIPWIRE_HOOK
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment.DURABILITY
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import yhw.panda.pandamoniumcrates.Utility.Companion.alert
import yhw.panda.pandamoniumcrates.Utility.Companion.checkCrates
import yhw.panda.pandamoniumcrates.Utility.Companion.error
import yhw.panda.pandamoniumcrates.Utility.Companion.isInvFull
import java.io.File
import java.io.IOException

class PandamoniumCrates : JavaPlugin(), CommandExecutor {
    companion object {
        lateinit var plugin: JavaPlugin
        lateinit var scheduler: BukkitScheduler
        val crates = ArrayList<Crate>()
        val crateInventories = HashMap<Crate, Inventory>()
        val cratePrompts = HashMap<Crate, Pair<Player, ItemStack>>()
        val busyPlayers = ArrayList<Player>()
    }

    override fun onEnable() {
        plugin = this
        scheduler = this.server.scheduler
        createConfig()
        CratesListener(this)
        checkCrates()
        for (crate in crates) crate.loadItems()
        Bukkit.getLogger().info("PandamoniumCrates Enabled.")
    }

    override fun onDisable() {
        Bukkit.getLogger().info("PandamoniumCrates Disabled.")
    }

    override fun onCommand(
        sender: CommandSender, command: Command,
        label: String, args: Array<out String>
    ): Boolean {
        if (label == "listcrates") {
            if (crates.isEmpty()) checkCrates()
            if (crates.isEmpty())
                error(sender, "There are no registered crates.")
            for ((i, crate) in crates.withIndex())
                alert(sender, "${i + 1}: $crate")
        } else if (label == "loadcrates") {
            if (sender is Player)
                alert(
                    sender,
                    "All crates have been reloaded from the config."
                )
            checkCrates()
            for (crate in crates) crate.loadItems()
        } else if (label == "savecrates") {
//            sender.sendMessage(
//                "${GREEN}All crates current inventories have " +
//                        "been saved from config.yml."
//            )
//            plugin.saveConfig()
        } else if (label == "swapkeys") {
            if (sender !is Player) return false
            for (item in sender.inventory) {
                if (item == null || item.type == AIR) continue
                if (item.type == TRIPWIRE_HOOK) {
                    if (!item.hasItemMeta()) continue
                    val meta = item.itemMeta ?: continue
                    if (!meta.hasLore()) continue
                    val lore = meta.lore ?: continue
                    for (line in lore)
                        if (line.contains("Right-Click on a \"")) {
                            val crateName = line.split("\"")[1]
                            val fixedName = crateName.substring(
                                2, crateName.length - 2
                            )
                            for (crate in crates)
                                if (crate.name == fixedName.toLowerCase()) {
                                    val name = crate.displayName
                                    val newLore = mutableListOf(
                                        "${WHITE}Right Click to Open the " +
                                                "$name Crate."
                                    )
                                    meta.lore = newLore
                                    meta.setDisplayName(
                                        "$BLUE${name} Crate Key"
                                    )
                                    meta.addEnchant(DURABILITY, 1, false)
                                    meta.addItemFlags(HIDE_ENCHANTS)
                                    item.itemMeta = meta
                                    break
                                }
                            break
                        }
                }
            }
        } else if (label == "addcrate") {
            if (sender !is Player) return false
            if (args.isNotEmpty() && args.size < 2) {
                for (crate in crates)
                    if (crate.name == args[0]) {
                        error(
                            sender,
                            "There is already a crate with this name."
                        )
                        return true
                    }
                val player: Player = sender
                val blockLookingAt = player.getTargetBlockExact(10)
                    ?: return true
                if (blockLookingAt.state !is InventoryHolder) return true
                val location = blockLookingAt.location
                val config = plugin.config

                config.createSection("crates.${args[0]}")
                config.set(
                    "crates.${args[0]}.location",
                    "${blockLookingAt.world.name}|" +
                            "${location.x}|${location.y}|${location.z}"
                )
                plugin.saveConfig()

                alert(sender, "The ${args[0]} crate has been added.")
            } else error(
                sender,
                "One argument was expected, " +
                        "but ${args.size} were found."
            )
            checkCrates()
        } else if (label == "setcrate") {
            if (sender !is Player) return false
            if (args.isNotEmpty() && args.size < 2) {
                if (crates.isEmpty()) checkCrates()
                for (crate in crates)
                    if (crate.name == args[0]) {
                        val player: Player = sender
                        val blockLookingAt =
                            player.getTargetBlockExact(10) ?: return true
                        if (blockLookingAt.state !is InventoryHolder)
                            return true
                        val location = blockLookingAt.location
                        val config = plugin.config
                        config.set(
                            "crates.${args[0]}.location",
                            "${blockLookingAt.world.name}|${location.x}|" +
                                    "${location.y}|${location.z}"
                        )
                        alert(
                            sender,
                            "The ${args[0]} crate has been " +
                                    "overwritten to ${location.x}, " +
                                    "${location.y}, ${location.z}."
                        )
                        plugin.saveConfig()
                        return true
                    }

                error(sender, "The ${args[0]} crate does not exist.")
            } else error(
                sender,
                "One argument was expected, " +
                        "but ${args.size} were found."
            )
            checkCrates()
        } else if (label == "delcrate") {
            if (sender !is Player) return false
            if (args.isNotEmpty() && args.size < 2) {
                val config = plugin.config
                for (crate in crates)
                    if (crate.name == args[0]) {
                        config.set("crates.${args[0]}", null)
                        alert(
                            sender,
                            "The ${args[0]} crate has been deleted."
                        )
                        plugin.saveConfig()
                        checkCrates()
                        return true
                    }
                error(sender, "The ${args[0]} crate does not exist.")
            } else error(
                sender,
                "One argument was expected, " +
                        "but ${args.size} were found."
            )
        } else if (label == "givekey") {
            if (args.isNotEmpty() && args.size < 4 && args.size > 1) {
                val playerName = args[0]
                var playerToGive: Player? = null
                for (player in Bukkit.getOnlinePlayers())
                    if (player.name == playerName) {
                        playerToGive = player
                        break
                    }
                if (playerToGive == null) {
                    error(sender, "$playerName is not on the server.")
                    // TODO: Add them to a waitlist that is searched
                    //  through every time someone joins to give them
                    //  any keys they missed. Make sure to check their
                    //  inventory before adding it too.
                    return true
                }

                val crateName = args[1]
                var crateToOpen: Crate? = null
                for (crate in crates)
                    if (crate.name.equals(crateName, ignoreCase = true)) {
                        crateToOpen = crate
                        break
                    }
                if (crateToOpen == null) {
                    error(sender, "$crateName does not exist.")
                    return true
                }

                var quantity = 1
                if (args.size > 2)
                    if (args[2].toIntOrNull() == null) {
                        error(
                            sender,
                            "Quantity was expected to be an integer."
                        )
                        return true
                    } else quantity = args[2].toInt()

                val newKeys = ItemStack(TRIPWIRE_HOOK, quantity)
                val newMeta = newKeys.itemMeta ?: return true
                val newLore = mutableListOf(
                    "${WHITE}Right Click to Open the $crateToOpen Crate."
                )
                newMeta.lore = newLore
                newMeta.setDisplayName("${BLUE}$crateToOpen Crate Key")
                newMeta.addEnchant(DURABILITY, 1, false)
                newMeta.addItemFlags(HIDE_ENCHANTS)
                newKeys.itemMeta = newMeta

                if (isInvFull(playerToGive)) {
                    error(
                        sender, "$playerName's inventory is full."
                    )
                    return true
                }

                playerToGive.inventory.addItem(newKeys)
                alert(playerToGive, "Enjoy your new keys!")
            } else {
                error(
                    sender, "Two or three argument were " +
                            "expected, but ${args.size} were found."
                )
            }
        } else if (label == "setcommand") {
            if (sender !is Player) return false
            val mainHand = sender.inventory.itemInMainHand
            val offHand = sender.inventory.itemInOffHand

            if (args.isEmpty()) {
                error(
                    sender,
                    "The command was expected in this format: '" +
                            "${BLUE}/setcommand /command goes here${RED}'."
                )
                return true
            }

            var givenCommand = "$LIGHT_PURPLE"
            if (!args[0].startsWith("/")) givenCommand += "/"
            for (arg in args) givenCommand += "$arg "

            if (mainHand != null && mainHand.type != AIR) {
                val meta = mainHand.itemMeta ?: return true
                val lore = (if (meta.hasLore()) meta.lore
                else mutableListOf<String>()) ?: return true
                lore.add(givenCommand)
                meta.lore = lore
                mainHand.itemMeta = meta
                return true
            } else if (offHand != null && offHand.type != AIR) {
                val meta = offHand.itemMeta ?: return true
                val lore = (if (meta.hasLore()) meta.lore
                else mutableListOf<String>()) ?: return true
                lore.add(givenCommand)
                meta.lore = lore
                offHand.itemMeta = meta
                return true
            } else {
                error(
                    sender,
                    "You must be be holding something to " +
                            "apply a command to it."
                )
                return true
            }
        } else if (label == "delcommand") {
            if (sender !is Player) return false
            val mainHand = sender.inventory.itemInMainHand
            val offHand = sender.inventory.itemInOffHand

            if (args.isEmpty()) {
                error(
                    sender,
                    "The command was expected in this format: '" +
                            "${BLUE}/delcommand /command goes here${RED}'."
                )
                return true
            }

            var givenCommand = "$LIGHT_PURPLE"
            if (!args[0].startsWith("/")) givenCommand += "/"
            for (arg in args) givenCommand += "$arg "

            if (mainHand != null && mainHand.type != AIR) {
                val meta = mainHand.itemMeta ?: return true
                if (meta.hasLore()) {
                    val lore = meta.lore ?: return true
                    for (line in lore)
                        if (line == givenCommand) {
                            lore.remove(line)
                            meta.lore = lore
                            mainHand.itemMeta = meta
                            alert(
                                sender,
                                "Successfully removed the command."
                            )
                            return true
                        }
                }
            }

            if (offHand != null && offHand.type != AIR) {
                val meta = offHand.itemMeta ?: return true
                if (meta.hasLore()) {
                    val lore = meta.lore ?: return true
                    for (line in lore)
                        if (line == givenCommand) {
                            lore.remove(line)
                            meta.lore = lore
                            offHand.itemMeta = meta
                            alert(
                                sender,
                                "Successfully removed the command."
                            )
                            return true
                        }
                }
            }

            error(
                sender,
                "The item you are holding does not have the " +
                        "given command attached to it."
            )
            return true
        }
        return false
    }

    private fun createConfig() {
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists() || configFile.length() == 0L) {
            configFile.parentFile.mkdirs()
            saveResource("config.yml", true)
        }
        val config = YamlConfiguration()
        try {
            config.load(configFile)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
        }
    }
}
