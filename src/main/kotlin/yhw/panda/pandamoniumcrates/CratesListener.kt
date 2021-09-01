package yhw.panda.pandamoniumcrates

import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.Material.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.plugin.java.JavaPlugin
import yhw.panda.pandamoniumcrates.PandamoniumCrates.Companion.busyPlayers
import yhw.panda.pandamoniumcrates.PandamoniumCrates.Companion.crateInventories
import yhw.panda.pandamoniumcrates.PandamoniumCrates.Companion.cratePrompts
import yhw.panda.pandamoniumcrates.PandamoniumCrates.Companion.plugin
import yhw.panda.pandamoniumcrates.PandamoniumCrates.Companion.scheduler
import yhw.panda.pandamoniumcrates.Utility.Companion.isInvFull
import yhw.panda.pandamoniumcrates.Utility.Companion.removeLastLore
import kotlin.math.floor

class CratesListener(plugin: JavaPlugin) : Listener {
    init {
        Bukkit.getLogger().info("Crates Listener up and running!!")
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        for (crate in PandamoniumCrates.crates)
            if (crate.location == block.location) {
                val player = event.player
                if (player.hasPermission("pandacrates.break") &&
                    player.isSneaking
                ) {
                    player.sendMessage(
                        "${BLUE}You broke a crate, " +
                                "but the crate still exists."
                    )
                    return
                } else if (player.hasPermission("pandacrates.break")) {
                    event.isCancelled = true
                    player.sendMessage(
                        "${RED}You must shift to break crates."
                    )
                    return
                } else {
                    event.isCancelled = true
                    player.sendMessage("${DARK_RED}You cannot break crates!")
                    return
                }
            }
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        val keyLore = "Right Click to Open the"

        val mainHand = player.inventory.itemInMainHand
        if (mainHand != null && mainHand.type == TRIPWIRE_HOOK) {
            val meta = mainHand.itemMeta
            if (meta != null && meta.hasLore()) {
                val lore = meta.lore
                if (lore != null)
                    for (line in lore)
                        if (line.contains(keyLore)) {
                            event.isCancelled = true
                            player.sendMessage(
                                "${LIGHT_PURPLE}A mystical energy " +
                                        "prevents you from placing this item."
                            )
                            return
                        }
            }
        }

        val offHand = player.inventory.itemInOffHand
        if (offHand != null && offHand.type == TRIPWIRE_HOOK) {
            val meta = offHand.itemMeta
            if (meta != null && meta.hasLore()) {
                val lore = meta.lore
                if (lore != null)
                    for (line in lore)
                        if (line.contains(keyLore)) {
                            event.isCancelled = true
                            player.sendMessage(
                                "${LIGHT_PURPLE}A mystical energy " +
                                        "prevents you from placing this item."
                            )
                            return
                        }
            }
        }
    }

    @EventHandler
    fun onClick(event: PlayerInteractEvent) {
        if (event.action != Action.LEFT_CLICK_BLOCK &&
            event.action != Action.RIGHT_CLICK_BLOCK
        ) return
        val block = event.clickedBlock ?: return
        val player = event.player
        for (crate in PandamoniumCrates.crates)
            if (crate.location == block.location) {
                if (event.action == Action.LEFT_CLICK_BLOCK)
                    viewCrateInv(player, crate)
                else if (event.action == Action.RIGHT_CLICK_BLOCK) {
                    event.isCancelled = true
                    val inv = player.inventory
                    val mainHand = inv.itemInMainHand
                    val offHand = inv.itemInOffHand
                    val keyLore = "Right Click to Open the $crate Crate."

                    var isKey = false
                    var keyFound = false
                    var itemToRemove = ItemStack(AIR)

                    if (mainHand != null && mainHand.type == TRIPWIRE_HOOK) {
                        val meta = mainHand.itemMeta
                        if (meta != null && meta.hasLore()) {
                            val lore = meta.lore
                            if (lore != null)
                                for (line in lore)
                                    if (line.contains(keyLore)) {
                                        isKey = true
                                        keyFound = true
                                        itemToRemove = mainHand
                                        break
                                    }
                        }
                    }

                    if (!keyFound && offHand != null &&
                        offHand.type == TRIPWIRE_HOOK
                    ) {
                        val meta = offHand.itemMeta
                        if (meta != null && meta.hasLore()) {
                            val lore = meta.lore
                            if (lore != null)
                                for (line in lore)
                                    if (line.contains(keyLore)) {
                                        isKey = true
                                        itemToRemove = offHand
                                        break
                                    }
                        }
                    }

                    if (isKey) {
                        for (busyPlayer in busyPlayers)
                            if (busyPlayer.toString() == player.toString()) {
                                player.sendMessage(
                                    "${RED}You must wait 5 seconds before" +
                                            " opening crates."
                                )
                                return
                            }

                        if (isInvFull(player)) {
                            player.sendMessage(
                                "${RED}You must have an open slot in your " +
                                        "inventory before opening the crate."
                            )
                            return
                        }

                        // TODO: Allow the player to open more than one.

                        itemToRemove.amount--
                        openCrateInv(player, crate)
                    } else if (player.hasPermission("pandacrates.chance")
                        && player.isSneaking
                    ) openChanceInv(player, crate)
                    else if (player.hasPermission("pandacrates.edit"))
                        openEditInv(player, crate)
                }
                return
            }
    }

    private fun openEditInv(player: Player, crate: Crate) {
        if (crate.isBeingEdited) {
            player.sendMessage(
                "${RED}Sorry, but this crate is being worked on at" +
                        " the moment. Please wait until it is done."
            )
            return
        }
        crate.isBeingEdited = true

        // val newInvSize = crate.items.size / 9
        // Check if newInvSize % 9 == 0, else find a 9 between 9-54.
        val inv = Bukkit.createInventory(
            null, 27, DARK_GREEN.toString() +
                    "Editing Items Of ${crate.displayName} Crate:"
        )
        crateInventories[crate] = inv
        crate.loadItems()
        crate.inputItems(inv, false)
        player.closeInventory()
        player.openInventory(inv)
    }

    private fun openChanceInv(player: Player, crate: Crate) {
        if (crate.isBeingEdited) {
            player.sendMessage(
                "${RED}Sorry, but this crate is being worked on at" +
                        " the moment. Please wait until it is done."
            )
            return
        }
        crate.isBeingEdited = true

        val inv = Bukkit.createInventory(
            null, 27, DARK_GREEN.toString() +
                    "Editing Chances Of ${crate.displayName} Crate:"
        )
        crateInventories[crate] = inv
        crate.loadItems()
        crate.inputItems(inv, false)
        player.closeInventory()
        player.openInventory(inv)
    }

    private fun viewCrateInv(player: Player, crate: Crate) {
        if (crate.isBeingEdited) {
            player.sendMessage(
                "${RED}Sorry, but this crate is being worked on at" +
                        " the moment. Please wait until it is done."
            )
            return
        }

        // val newInvSize = crate.items.size / 9
        // Check if newInvSize % 9 == 0, else find a 9 between 9-54.
        val inv = Bukkit.createInventory(
            null, 27, DARK_GREEN.toString() +
                    "Viewing The ${crate.displayName} Crate:"
        )
        crateInventories[crate] = inv
        crate.loadItems()
        crate.inputItems(inv, true)
        player.closeInventory()
        player.openInventory(inv)
    }

    private fun openCrateInv(player: Player, crate: Crate) {
        if (crate.isBeingEdited) {
            player.sendMessage(
                "${RED}Sorry, but this crate is being worked on at" +
                        " the moment. Please wait until it is done."
            )
            return
        }

        val inv = Bukkit.createInventory(
            null, 27, DARK_GREEN.toString() +
                    "Opening ${crate.displayName} Crate!"
        )
        crateInventories[crate] = inv
        crate.loadItems()

        val paneArray = arrayListOf(
            PINK_STAINED_GLASS_PANE, RED_STAINED_GLASS_PANE,
            ORANGE_STAINED_GLASS_PANE, YELLOW_STAINED_GLASS_PANE,
            LIME_STAINED_GLASS_PANE, GREEN_STAINED_GLASS_PANE,
            LIGHT_BLUE_STAINED_GLASS_PANE, CYAN_STAINED_GLASS_PANE,
            BLUE_STAINED_GLASS_PANE, MAGENTA_STAINED_GLASS_PANE,
            PURPLE_STAINED_GLASS_PANE, LIGHT_GRAY_STAINED_GLASS_PANE,
            GRAY_STAINED_GLASS_PANE, BROWN_STAINED_GLASS_PANE,
            BLACK_STAINED_GLASS_PANE, WHITE_STAINED_GLASS_PANE
        )

        var i = 0
        while (i++ < inv.size - 1) {
            val randomInt = (Math.random() * paneArray.size).toInt()
            val randomPane = ItemStack(paneArray[randomInt])
            inv.setItem(i, randomPane)
        }

        val opening = crate.open(player, true)
        var item = removeLastLore(opening.first).first
        inv.setItem(floor(inv.size / 2.0).toInt(), item)

        player.closeInventory()
        player.openInventory(inv)

        if (!busyPlayers.contains(player)) busyPlayers.add(player)

        var openItemShuffle = 0
        val shuffle = scheduler.runTaskTimer(
            plugin,
            Runnable {
                i = inv.size
                openItemShuffle++
                while (--i >= 0) {
//                    Bukkit.getLogger().info("$i")
                    if (i == 0) {
                        val randomInt =
                            (Math.random() * paneArray.size).toInt()
                        inv.setItem(i, ItemStack(paneArray[randomInt]))
                        continue
                    } else if (i == floor(inv.size / 2.0).toInt()) {
                        if (openItemShuffle == 4) {
                            openItemShuffle = 0
                            continue
                        }
                        item = crate.open(player, true).first
                        inv.setItem(i, item)
                        continue
                    } else if (i == floor(inv.size / 2.0).toInt() + 1) {
                        inv.setItem(i, inv.getItem(i - 2))
                        continue
                    }
                    inv.setItem(i, inv.getItem(i - 1))
                }
//                Bukkit.getLogger().info("Iteration Done.")
            }, 0L, 5L // 1/4 second (delay, period)
        )

        scheduler.runTaskLater(
            plugin,
            Runnable {
                Bukkit.getLogger().info("Cancelling shuffle...")
                shuffle.cancel()
                busyPlayers.remove(player)

                val lastOpen = crate.open(player, false)
                val lastItem = removeLastLore(lastOpen.first).first
                inv.setItem(floor(inv.size / 2.0).toInt(), lastItem)
                if (!lastOpen.second) player.inventory.addItem(lastItem)
            }, 80L // 4 seconds
        )
    }

    @EventHandler
    fun onInvOpen(event: InventoryOpenEvent) {
        val inv = event.inventory
        val player = event.player as Player
        if (!event.player.hasPermission("pandacrates.edit")) return
        val title = event.view.title
        if (inv.holder == null && title.contains("Editing Chances"))
            for (hash in crateInventories)
                if (hash.value.toString() == inv.toString()) {
                    cratePrompts[hash.key] = Pair(player, ItemStack(AIR))
                    return
                }
    }

    @EventHandler
    fun onInvClose(event: InventoryCloseEvent) {
        val inv = event.inventory
        val player = event.player as Player

        // TODO: Re-enable this once the opening procedure is worked on again.
//        for (busyPlayer in busyPlayers)
//            if (busyPlayer.toString() == player.toString()) {
//                if (event.view.title.contains("Opening"))
//                    scheduler.runTaskLater(
//                        plugin,
//                        Runnable {
//                            if (!player.openInventory.title.contains(
//                                    "Opening"
//                                )
//                            ) player.openInventory(inv)
//                        }, 20L
//                    )
//                return
//            }

        if (!(player.hasPermission("pandacrates.edit") ||
                    player.hasPermission("pandacrates.chance"))
        ) return
        val title = player.openInventory.title
        if (inv.holder == null && (title.contains("Editing Items") ||
                    title.contains("Editing Chances"))
        ) {
            for (item in player.inventory) {
                if (item == null) continue
                item.itemMeta = removeLastLore(item).first.itemMeta
            }

            var crate: Crate? = null
            for (hash in crateInventories)
                if (hash.value.toString() == inv.toString()) {
                    crate = hash.key
                    hash.key.saveItems(inv.contents)
                    crateInventories.remove(hash.key)
                    hash.key.isBeingEdited = false
                    break
                }

            val copyPrompts = HashMap<Crate, Pair<Player, ItemStack>>()
            for (prompt in cratePrompts)
                copyPrompts[prompt.key] = prompt.value

            for (prompt in copyPrompts) {
                Bukkit.getLogger().info("$prompt")
                val value = prompt.value
                if (value.first.toString() != player.toString()) continue
                if (value.second.type != AIR) continue
                cratePrompts.remove(prompt.key)
                break
            }

            var total = 0.0
            for (item in inv.contents)
                if (item != null)
                    total += removeLastLore(item).second

            val color = if (total == 100.0) GREEN else RED
            player.sendMessage("${color}Total Percent for $crate is $total%")
        }
    }

    @EventHandler
    fun onInvClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem ?: return
        if (clickedItem.type == AIR || clickedItem.itemMeta == null) return
        if (event.whoClicked !is Player) return
        val player = event.whoClicked as Player

        val openInv = event.view
        val title = openInv.title

        if (title.contains("Viewing The") ||
            title.contains("Opening")
        ) {
            event.isCancelled = true
            return
        } else if (title.contains("Editing Chances")) {
            event.isCancelled = true
            if (event.clickedInventory is PlayerInventory) return

            val copyPrompts = HashMap<Crate, Pair<Player, ItemStack>>()
            for (prompt in cratePrompts)
                copyPrompts[prompt.key] = prompt.value

            // Finds crate associated with player who will edit the chance
            for (hash in copyPrompts)
                if (hash.value.first.toString() == player.toString()) {
                    val newPair = hash.value.copy(second = clickedItem)
                    cratePrompts[hash.key] = newPair
                    promptChance(player, clickedItem)
                    return
                }
        }
    }

    private fun promptChance(player: Player, itemToEdit: ItemStack) {
        val itemMeta = itemToEdit.itemMeta
        val typeName = itemToEdit.type.name.toLowerCase()
        var displayName = typeName.replace("_", " ")
        if (itemMeta?.displayName != "")
            displayName = itemMeta?.displayName.toString()
        // TODO: Potentially start a timer here to allow the player
        //  just a few seconds to enter the chance and be able to
        //  cancel the event in time.
        player.closeInventory()
        player.sendMessage("${GOLD}Enter the chance for $displayName")
    }

    @EventHandler
    fun onMessage(event: AsyncPlayerChatEvent) {
        val player = event.player
        val message = event.message.toLowerCase()

        if (message == "stop" || message == "cancel")
            for (hash in cratePrompts)
                if (hash.value.first.toString() == player.toString()) {
                    event.isCancelled = true
                    cratePrompts.keys.remove(hash.key)
                    player.sendMessage(
                        "${GOLD}Cancelled the edit chance transaction."
                    )
                    return
                }

        var newChance = 0.0
        try {
            newChance = message.toDouble()
        } catch (ignored: Exception) {
            for (hash in cratePrompts)
                if (hash.value.first.toString() == player.toString()) {
                    event.isCancelled = true
                    player.sendMessage(
                        "${RED}You must send a double value for the chance " +
                                "or send '${GOLD}stop${RED}' to stop!"
                    )
                    return
                    // TODO: cratePrompts is never removed from after editing
                }
        }

        val copyPrompts = HashMap<Crate, Pair<Player, ItemStack>>()
        for (prompt in cratePrompts)
            copyPrompts[prompt.key] = prompt.value

        for (hash in copyPrompts) {
            if (hash.value.first.toString() == player.toString()) {
                event.isCancelled = true
                val crate = hash.key
                for (itemHash in crate.hashItems) {
                    val item = hash.value.second
                    val compareItem = removeLastLore(item).first
                    if (itemHash.key == compareItem) {
                        crate.hashItems[itemHash.key] = newChance
                        cratePrompts.remove(crate)
                        Bukkit.getScheduler().runTaskLater(
                            PandamoniumCrates.plugin,
                            Runnable { openChanceInv(player, crate) }, 20L
                        )
                        player.sendMessage(
                            "${GOLD}${itemHash.key} should have it's " +
                                    "chance set to $newChance"
                        )
                        return
                    }
                }
                return
            }
        }
    }
}
