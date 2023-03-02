/*
 *   Copyright 2023 Raphael Roehrig (alias "raffel080108"). All rights reserved.
 *   Licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 *   You may not use any content of this file or software except in compliance with the license.
 *   You can obtain a copy of the license here: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package raffel080108.altarcrafting.listeners;

import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import raffel080108.altarcrafting.AltarCrafting;
import raffel080108.altarcrafting.DataHandler;
import raffel080108.altarcrafting.utils.Utils;
import org.apache.commons.collections4.MultiValuedMap;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class IngredientHolderListener implements Listener {
    private final DataHandler dataHandler = DataHandler.getInstance();
    private final Utils utils = new Utils();

    @EventHandler(priority = EventPriority.HIGHEST)
    private void hangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(dataHandler.getIngredientAmountKey()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
        Logger log = JavaPlugin.getPlugin(AltarCrafting.class).getLogger();
        NamespacedKey ingredientAmountKey = dataHandler.getIngredientAmountKey();
        HashMap<Player, Location> playerCraftingAltarLocations = dataHandler.getPlayerCraftingAltarLocations();
        HashMap<ItemStack, Location> placedItemsLocations = dataHandler.getPlacedItemsLocations();
        MultiValuedMap<Location, ItemStack> itemsPlacedForCrafting = dataHandler.getItemsPlacedForCrafting();
        FileConfiguration messages = dataHandler.getMessages();

        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        if (dataHandler.getCraftingInProgress().contains(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
            String message = messages.getString("message-interaction-failed-crafting-in-progress");
            player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cYou cannot use the altar, while there is crafting in progress!");
            return;
        }

        Entity entity = event.getRightClicked();
        PersistentDataContainer dataContainer = entity.getPersistentDataContainer();
        if (!dataContainer.has(ingredientAmountKey))
            return;
        Integer itemAmount = dataContainer.get(ingredientAmountKey, PersistentDataType.INTEGER);
        if (itemAmount == null) {
            log.severe("Could not find value mapped for ingredientAmountKey for itemFrame at " + entity.getLocation());
            return;
        }

        ItemFrame itemFrame = (ItemFrame) entity;
        ItemStack item = itemFrame.getItem();
        item.setAmount(itemAmount);
        Location location = itemFrame.getLocation();
        Location altarLocation = null;
        for (Map.Entry<Location, ItemStack> entry : itemsPlacedForCrafting.entries()) {
            if (entry.getValue().equals(item)) {
                altarLocation = entry.getKey();
                break;
            }
        }
        if (altarLocation == null) {
            log.severe("Could not find mapped altarLocation for itemStack " + item + ", while attempting to process altar-ingredient-pickup");
            return;
        }

        if (!altarLocation.equals(playerCraftingAltarLocations.get(player))) {
            String message = messages.getString("message-interaction-failed-occupied");
            player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cAnother player is currently using this altar - Please wait until they finish");
            return;
        }

        itemFrame.remove();

        placedItemsLocations.remove(item);
        itemsPlacedForCrafting.get(altarLocation).remove(item);
        if (itemsPlacedForCrafting.get(altarLocation).isEmpty())
            utils.cancelAltarCraftingSession(player);

        World world = location.getWorld();
        world.playSound(location, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1F, 1F);
        if (!player.getInventory().addItem(item).isEmpty())
            world.dropItem(location, item);
    }
}
