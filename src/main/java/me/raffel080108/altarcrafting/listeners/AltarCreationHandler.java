package me.raffel080108.altarcrafting.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import me.raffel080108.altarcrafting.data.DataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class AltarCreationHandler implements Listener {
    private final DataHandler dataHandler;

    public AltarCreationHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void playerInteractEvent(PlayerInteractEvent event) {
        HashMap<Player, String> pendingAltarCreations = dataHandler.getPendingAltarCreations();
        HashMap<Location, String> altarLocations = dataHandler.getAltarLocations();
        HashMap<Location, Location> baseLayerLocations = dataHandler.getBaseLayerLocations();
        HashMap<Location, Location> ingredientPlacementLocations = dataHandler.getIngredientPlacementLocations();
        HashMap<Player, Long> interactCooldown = dataHandler.getInteractEventCooldown();
        FileConfiguration messages = dataHandler.getMessages();

        if (event.useInteractedBlock().equals(Event.Result.DENY) || event.useItemInHand().equals(Event.Result.DENY))
            return;

        Logger log = dataHandler.getLogger();

        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();
        if (!pendingAltarCreations.containsKey(player))
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;
        Material clickedBlockType = clickedBlock.getType();

        event.setCancelled(true);
        String paramsPath = pendingAltarCreations.get(player);
        pendingAltarCreations.remove(player);
        ConfigurationSection altarParams = dataHandler.getConfig().getConfigurationSection(paramsPath);
        String playerErrorMsgConfig = messages.getString("message-altar-creation-failed-internal-error");
        String playerErrorMsg = playerErrorMsgConfig != null ? ChatColor.translateAlternateColorCodes('&', playerErrorMsgConfig) : "§cInternal error occurred while attempting to parse altar-creation";
        if (altarParams == null) {
            player.sendMessage(playerErrorMsg);
            log.severe("Could not find altar-parameters at path " + paramsPath + ", while attempting to parse altar-creation for player " + player.getName() + ". The altar's parameters were either altered or removed - Please check your configuration");
            return;
        }
        Location clickedBlockLocation = event.getClickedBlock().getLocation();
        if (altarLocations.containsKey(clickedBlockLocation) ||
                baseLayerLocations.containsKey(clickedBlockLocation) ||
                ingredientPlacementLocations.containsKey(clickedBlockLocation)) {
            player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
            String message = messages.getString("message-altar-creation-failed-overlap");
            interactCooldown.put(player, System.currentTimeMillis());
            player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cThe altar you are attempting to create would overlap with an existing altar! Please try again at a different location");
            return;
        }

        String altarName = altarParams.getName();
        String baseMaterialString = altarParams.getString("base-material");
        if (baseMaterialString == null) {
            player.playSound(playerLocation, Sound.BLOCK_ANVIL_LAND, 1000F, 1F);
            player.sendMessage(playerErrorMsg);
            log.severe("Could not find value for parameter base-material for altar " + altarName + ", while attempting to parse altar creation");
            return;
        }

        Material baseMaterial = Material.matchMaterial(baseMaterialString);
        if (baseMaterial == null) {
            player.playSound(playerLocation, Sound.BLOCK_ANVIL_LAND, 1000F, 1F);
            player.sendMessage(playerErrorMsg);
            log.severe("Found invalid value for parameter base-material for altar " + altarName + ", while attempting to parse altar creation");
            return;
        }
        baseMaterialString = baseMaterialString.toUpperCase(Locale.ROOT);

        String centerMaterialString = altarParams.getString("center-material");
        if (centerMaterialString == null) {
            if (!clickedBlockType.equals(baseMaterial)) {
                player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
                String message = messages.getString("message-altar-creation-failed-incorrect-center-block");
                player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message).replace("%correctMaterial%", baseMaterialString) : "§cClicked block is of an incorrect type. The block's type should be " + baseMaterialString + " instead");
                return;
            }
        } else {
            Material centerMaterial;
            centerMaterial = Material.matchMaterial(centerMaterialString);
            if (centerMaterial == null) {
                player.playSound(playerLocation, Sound.BLOCK_ANVIL_LAND, 1000F, 1F);
                player.sendMessage(playerErrorMsg);
                log.severe("Found invalid value for parameter center-material for altar " + altarName + ", while attempting to parse altar creation");
                return;
            }
            if (!clickedBlockType.equals(centerMaterial)) {
                centerMaterialString = centerMaterialString.toUpperCase(Locale.ROOT);
                String message = messages.getString("message-altar-creation-failed-incorrect-center-block");
                player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message).replace("%correctMaterial%", centerMaterialString) : "§cClicked block is of an incorrect type. The block's type should be " + centerMaterialString + " instead");
                return;
            }
        }

        World world = clickedBlock.getWorld();
        Location centerLocation = clickedBlock.getLocation();
        double y = centerLocation.getY();
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) {
            player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
            String message = messages.getString("message-altar-creation-failed-invalid-height");
            player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cInvalid location for altar creation - Please try again at a different Y-level");
            return;
        }

        double x = centerLocation.getX(), yM1 = centerLocation.getY() - 1, z = centerLocation.getZ();
        ArrayList<Location> localBaseLayerLocations = new ArrayList<>(List.of(new Location(world, x, yM1, z)));
        for (int i = 1; i <= 2; i++) {
            localBaseLayerLocations.add(new Location(world, x + i, yM1, z).getBlock().getLocation());
            localBaseLayerLocations.add(new Location(world, x - i, yM1, z).getBlock().getLocation());
            localBaseLayerLocations.add(new Location(world, x, yM1, z + i).getBlock().getLocation());
            localBaseLayerLocations.add(new Location(world, x, yM1, z - i).getBlock().getLocation());
            localBaseLayerLocations.add(new Location(world, x + i, yM1, z + i).getBlock().getLocation());
            localBaseLayerLocations.add(new Location(world, x + i, yM1, z - i).getBlock().getLocation());
            localBaseLayerLocations.add(new Location(world, x - i, yM1, z + i).getBlock().getLocation());
            localBaseLayerLocations.add(new Location(world, x - i, yM1, z - i).getBlock().getLocation());
            for (int i2 = 1; i2 < i; i2++) {
                localBaseLayerLocations.add(new Location(world, x + i2, yM1, z + i).getBlock().getLocation());
                localBaseLayerLocations.add(new Location(world, x + i2, yM1, z - i).getBlock().getLocation());
                localBaseLayerLocations.add(new Location(world, x - i2, yM1, z + i).getBlock().getLocation());
                localBaseLayerLocations.add(new Location(world, x - i2, yM1, z - i).getBlock().getLocation());
                localBaseLayerLocations.add(new Location(world, x + i, yM1, z + i2).getBlock().getLocation());
                localBaseLayerLocations.add(new Location(world, x - i, yM1, z + i2).getBlock().getLocation());
                localBaseLayerLocations.add(new Location(world, x + i, yM1, z - i2).getBlock().getLocation());
                localBaseLayerLocations.add(new Location(world, x - i, yM1, z - i2).getBlock().getLocation());
            }
        }

        List<Location> localIngredientPlacementLocations = new ArrayList<>(List.of(
                new Location(world, x + 2, y, z + 2),
                new Location(world, x + 2, y, z - 2),
                new Location(world, x - 2, y, z + 2),
                new Location(world, x - 2, y, z - 2)
        ));

        String altarType = altarParams.getString("altar-type");
        if (altarType == null) {
            log.warning("Could not find parameter altar-type for altar " + altarName + ". Default (4) will be used");
            altarType = "4";
        }

        if (altarType.equals("8")) {
            localBaseLayerLocations.addAll(List.of(
                    new Location(world, x + 3, yM1, z).getBlock().getLocation(),
                    new Location(world, x + 3, yM1, z + 1).getBlock().getLocation(),
                    new Location(world, x + 3, yM1, z - 1).getBlock().getLocation(),
                    new Location(world, x - 3, yM1, z).getBlock().getLocation(),
                    new Location(world, x - 3, yM1, z + 1).getBlock().getLocation(),
                    new Location(world, x - 3, yM1, z - 1).getBlock().getLocation(),
                    new Location(world, x, yM1, z + 3).getBlock().getLocation(),
                    new Location(world, x + 1, yM1, z + 3).getBlock().getLocation(),
                    new Location(world, x - 1, yM1, z + 3).getBlock().getLocation(),
                    new Location(world, x, yM1, z - 3).getBlock().getLocation(),
                    new Location(world, x + 1, yM1, z - 3).getBlock().getLocation(),
                    new Location(world, x - 1, yM1, z - 3).getBlock().getLocation()
            ));
            localIngredientPlacementLocations.addAll(List.of(
                    new Location(world, x + 3, y, z).getBlock().getLocation(),
                    new Location(world, x - 3, y, z).getBlock().getLocation(),
                    new Location(world, x, y, z + 3).getBlock().getLocation(),
                    new Location(world, x, y, z - 3).getBlock().getLocation()
            ));
        } else if (!altarType.equals("4"))
            log.warning("Found invalid value for parameter altar-type for altar " + altarName + ". Default (4) will be used");

        String message = messages.getString("message-altar-creation-failed-invalid-altar-structure");
        HashMap<Location, Location> baseLayerLocationsMap = new HashMap<>();
        for (Location location : localBaseLayerLocations) {
            Material material = location.getBlock().getType();
            if (!material.equals(baseMaterial)) {
                player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
                player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message).replace("%incorrectMaterial%", material.name()).replace("%correctMaterial%", baseMaterialString) : "§cInvalid altar structure - Found block of type " + material.name() + " where type " + baseMaterialString + " was expected");
                return;
            }
            baseLayerLocationsMap.put(location, centerLocation);
        }

        String ingredientPlacementMaterialString = altarParams.getString("corners-material");
        Material ingredientPlacementMaterial = baseMaterial;
        if (ingredientPlacementMaterialString != null) {
            ingredientPlacementMaterial = Material.matchMaterial(ingredientPlacementMaterialString);
            if (ingredientPlacementMaterial == null) {
                player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
                player.sendMessage(playerErrorMsg);
                log.severe("Found invalid value for parameter center-material for altar " + altarName + ", while attempting to parse altar creation");
                return;
            }
            ingredientPlacementMaterialString = ingredientPlacementMaterialString.toUpperCase(Locale.ROOT);
        }
        HashMap<Location, Location> ingredientPlacementLocationsMap = new HashMap<>();
        for (Location location : localIngredientPlacementLocations) {
            Material material = location.getBlock().getType();
            if (!material.equals(ingredientPlacementMaterial)) {
                player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
                player.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message).replace("%incorrectMaterial%", material.name()).replace("%correctMaterial%", ingredientPlacementMaterialString != null ? ingredientPlacementMaterialString : baseMaterialString) : "§cInvalid altar structure - Found block of type " + material.name() + " where type " + ingredientPlacementMaterialString + " was expected");
                return;
            }
            ingredientPlacementLocationsMap.put(location, centerLocation);
        }

        altarLocations.put(centerLocation, paramsPath);
        baseLayerLocations.putAll(baseLayerLocationsMap);
        ingredientPlacementLocations.putAll(ingredientPlacementLocationsMap);
        interactCooldown.put(player, System.currentTimeMillis());
        world.playSound(centerLocation, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1F);
        String message2 = messages.getString("message-altar-creation-success");
        player.sendMessage(message2 != null ? ChatColor.translateAlternateColorCodes('&', message2) : "§aAltar created successfully!");
    }
}
