package me.raffel080108.altarcrafting.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import me.raffel080108.altarcrafting.data.DataHandler;
import me.raffel080108.altarcrafting.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class AltarDestructionHandler implements Listener {
    private final DataHandler dataHandler;
    private final Utils utils;

    public AltarDestructionHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.utils = new Utils(dataHandler);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void blockBreakEvent(BlockBreakEvent event) {
        HashMap<Location, String> altarLocations = dataHandler.getAltarLocations();
        HashMap<Location, Location> baseLayerLocations = dataHandler.getBaseLayerLocations();
        HashMap<Location, Location> ingredientPlacementLocations = dataHandler.getIngredientPlacementLocations();
        HashMap<Player, Location> playerCraftingAltarLocations = dataHandler.getPlayerCraftingAltarLocations();
        FileConfiguration messages = dataHandler.getMessages();

        if (event.isCancelled())
            return;

        Location location = event.getBlock().getLocation();
        boolean isAltarCenterLocation = altarLocations.containsKey(location), isBaseLayerLocation = baseLayerLocations.containsKey(location);
        if (!isAltarCenterLocation && !isBaseLayerLocation && !ingredientPlacementLocations.containsKey(location))
            return;

        Player player = event.getPlayer();
        if (!player.hasPermission("altarCrafting.createAltar")) {
            event.setCancelled(true);
            String message = messages.getString("message-break-altar-no-permission");
            player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cYou do not have permission to break that block!");
            return;
        }

        Location altarLocation;
        if (isAltarCenterLocation)
            altarLocation = location;
        else if (isBaseLayerLocation)
            altarLocation = baseLayerLocations.get(location);
        else
            altarLocation = ingredientPlacementLocations.get(location);

        altarLocations.remove(altarLocation);

        ArrayList<Location> keysToRemove1 = new ArrayList<>();
        for (Map.Entry<Location, Location> entry : baseLayerLocations.entrySet())
            if (entry.getValue().equals(altarLocation))
                keysToRemove1.add(entry.getKey());
        for (Location key : keysToRemove1)
            baseLayerLocations.remove(key);

        ArrayList<Location> keysToRemove2 = new ArrayList<>();
        for (Map.Entry<Location, Location> entry : ingredientPlacementLocations.entrySet())
            if (entry.getValue().equals(altarLocation))
                keysToRemove2.add(entry.getKey());
        for (Location key : keysToRemove2)
            ingredientPlacementLocations.remove(key);

        if (playerCraftingAltarLocations.containsValue(altarLocation)) {
            for (Map.Entry<Player, Location> entry : playerCraftingAltarLocations.entrySet()) {
                if (entry.getValue().equals(altarLocation)) {
                    utils.cancelAltarCraftingSession(entry.getKey());
                    break;
                }
            }
        }
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1F, 1F);
        String message = messages.getString("message-break-altar");
        player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§6You broke an altar!");
    }
}
