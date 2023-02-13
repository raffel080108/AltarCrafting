package me.raffel080108.altarcrafting.listeners;

import me.raffel080108.altarcrafting.DataHandler;
import me.raffel080108.altarcrafting.utils.Utils;
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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class PlayerEntityInteractListener implements Listener {
    private final DataHandler dataHandler;
    private final Utils utils;

    public PlayerEntityInteractListener(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.utils = new Utils(dataHandler);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
        Logger log = dataHandler.getLogger();
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
