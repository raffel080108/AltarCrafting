/*
 *   Copyright 2023 Raphael Roehrig (alias "raffel080108"). All rights reserved.
 *   Licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 *   You may not use any content of this file or software except in compliance with the license.
 *   You can obtain a copy of the license here: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package raffel080108.altarcrafting;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import raffel080108.altarcrafting.commands.*;
import raffel080108.altarcrafting.commands.annotations.*;
import raffel080108.altarcrafting.listeners.*;
import raffel080108.altarcrafting.utils.Utils;
import revxrsal.commands.autocomplete.AutoCompleter;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class AltarCrafting extends JavaPlugin {
    private DataHandler dataHandler = DataHandler.getInstance();
    private File dataFile;
    private FileConfiguration data;

    @Override
    public void onEnable() {
        Logger log = getLogger();
        Utils utils = new Utils();

        dataHandler.setIngredientAmountKey(new NamespacedKey(this, "ingredientAmount"));
        utils.loadConfigurations();

        utils.loadRecipes();

        log.info("Registering listeners...");
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new AltarCraftHandler(), this);
        pluginManager.registerEvents(new AltarCreationHandler(), this);
        pluginManager.registerEvents(new AltarDestructionHandler(), this);
        pluginManager.registerEvents(new IngredientHolderListener(), this);
        pluginManager.registerEvents(new PlayerQuitListener(), this);

        log.info("Setting up commands...");
        BukkitCommandHandler commandHandler = BukkitCommandHandler.create(this);
        AutoCompleter autoCompleter = commandHandler.getAutoCompleter();
        autoCompleter.registerSuggestionFactory(parameter -> {
            if (parameter.hasAnnotation(AutoCompleteOnlinePlayers.class)) {
                return (args, sender, command) -> {
                    ArrayList<String> players = new ArrayList<>();
                    onlinePLayersLoop:
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        for (MetadataValue meta : player.getMetadata("vanished"))
                            if (meta.asBoolean())
                                continue onlinePLayersLoop;

                        players.add(player.getName());
                    }
                    return players;
                };
            }
            return null;
        });

        autoCompleter.registerSuggestionFactory(parameter -> {
            if (parameter.hasAnnotation(AutoCompleteAltarsList.class)) {
                return (args, sender, command) -> {
                    ConfigurationSection altars = dataHandler.getConfig().getConfigurationSection("altars");
                    if (altars == null) {
                        getLogger().severe("Could not find configuration-section \"altars\" while attempting to auto-complete a command. Please check your configuration");
                        return List.of();
                    }
                    return altars.getKeys(false);
                };
            }
            return null;
        });

        commandHandler.register(new CancelAltarCraftingCommand());
        commandHandler.register(new CreateAltarCommand());
        commandHandler.register(new ReloadCommand());
        commandHandler.registerBrigadier();

        log.info("Loading data...");
        if (!new File(getDataFolder(), "data.yml").exists())
            saveResource("data.yml", false);
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        String errorMsg = "Error occurred while attempting to read some data from data-file. " +
                "This most likely occurred due to modification of the data-file.\n" +
                "If this issue persists, try clearing the data-file - If the issue still persists please contact the plugin developer";

        HashMap<Location, String> altarLocations = new HashMap<>();
        List<String> altarLocationsStringList = data.getStringList("altarLocations");
        for (String string : altarLocationsStringList) {
            String[] splitString = string.split("\\|");
            String[] locationSplit = splitString[0].split(",");
            World world = Bukkit.getWorld(locationSplit[0]);
            if (world == null) {
                log.severe("Found value null for world of location-value in \"altarLocations\" at index " + altarLocationsStringList.indexOf(string) + " while attempting to read data from data-file");
                log.severe(errorMsg);
                continue;
            }

            if (getConfig().getConfigurationSection(splitString[1]) == null) {
                log.severe("Could not find altar-parameters for path " + splitString[1] + " while attempting to load data from data-file");
                continue;
            }

            double x, y, z;
            try {
                x = Double.parseDouble(locationSplit[1]);
                y = Double.parseDouble(locationSplit[2]);
                z = Double.parseDouble(locationSplit[3]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                log.severe("Found invalid coordinate value of location-value in \"altarLocations\" at index " + altarLocationsStringList.indexOf(string) + " while attempting to read data from data-file");
                continue;
            }
            altarLocations.put(new Location(world, x, y, z), splitString[1]);
        }
        dataHandler.getAltarLocations().putAll(altarLocations);

        HashMap<Location, Location> baseLayerLocations = new HashMap<>();
        List<String> baseLayerLocationsStringList = data.getStringList("baseLayerLocations");
        for (String string : baseLayerLocationsStringList) {
            String[] splitString = string.split("\\|");
            String[] location1Split = splitString[0].split(",");
            String[] location2Split = splitString[1].split(",");

            World world1, world2;

            world1 = Bukkit.getWorld(location1Split[0]);
            world2 = Bukkit.getWorld(location2Split[0]);
            if (world1 == null || world2 == null) {
                log.severe("Found value null for world of a location-value in list \"baseLayerLocations\" at index " + baseLayerLocationsStringList.indexOf(string) + " while attempting to read data from data-file");
                continue;
            }

            double x1, y1, z1, x2, y2, z2;
            try {
                x1 = Double.parseDouble(location1Split[1]);
                y1 = Double.parseDouble(location1Split[2]);
                z1 = Double.parseDouble(location1Split[3]);
                x2 = Double.parseDouble(location2Split[1]);
                y2 = Double.parseDouble(location2Split[2]);
                z2 = Double.parseDouble(location2Split[3]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                log.severe("Found invalid coordinate value of a location-value in list \"baseLayerLocations\" at index " + baseLayerLocationsStringList.indexOf(string) + " while attempting to read data from data-file");
                continue;
            }

            baseLayerLocations.put(new Location(world1, x1, y1, z1), new Location(world2, x2, y2, z2));
        }
        dataHandler.getBaseLayerLocations().putAll(baseLayerLocations);

        HashMap<Location, Location> ingredientPlacementLocations = new HashMap<>();
        List<String> ingredientPlacementLocationsStringList = data.getStringList("ingredientPlacementLocations");
        for (String string : ingredientPlacementLocationsStringList) {
            String[] splitString = string.split("\\|");
            String[] location1Split = splitString[0].split(",");
            String[] location2Split = splitString[1].split(",");

            World world1, world2;

            world1 = Bukkit.getWorld(location1Split[0]);
            world2 = Bukkit.getWorld(location2Split[0]);
            if (world1 == null || world2 == null) {
                log.severe("Found value null for world of a location-value in list \"ingredientPlacementLocations\" at index " + ingredientPlacementLocationsStringList.indexOf(string) + " while attempting to read data from data-file");
                continue;
            }

            double x1, y1, z1, x2, y2, z2;
            try {
                x1 = Double.parseDouble(location1Split[1]);
                y1 = Double.parseDouble(location1Split[2]);
                z1 = Double.parseDouble(location1Split[3]);
                x2 = Double.parseDouble(location2Split[1]);
                y2 = Double.parseDouble(location2Split[2]);
                z2 = Double.parseDouble(location2Split[3]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                log.severe("Found invalid coordinate value of a location-value in list \"ingredientPlacementLocations\" at index " + ingredientPlacementLocationsStringList.indexOf(string) + " while attempting to read data from data-file");
                continue;
            }

            ingredientPlacementLocations.put(new Location(world1, x1, y1, z1), new Location(world2, x2, y2, z2));
        }
        dataHandler.getIngredientPlacementLocations().putAll(ingredientPlacementLocations);

        log.info("Plugin started!");
    }

    @Override
    public void onDisable() {
        Logger log = getLogger();

        log.info("Saving data...");
        if (!new File(getDataFolder(), "data.yml").exists())
            saveResource("data.yml", false);

        ArrayList<String> altarLocationsStringList = new ArrayList<>();
        for (Map.Entry<Location, String> entry : dataHandler.getAltarLocations().entrySet()) {
            Location location = entry.getKey();
            altarLocationsStringList.add(location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "|" +
                    entry.getValue());
        }
        data.set("altarLocations", altarLocationsStringList);

        ArrayList<String> baseLayerLocationsStringList = new ArrayList<>();
        for (Map.Entry<Location, Location> entry : dataHandler.getBaseLayerLocations().entrySet()) {
            Location location1 = entry.getKey();
            Location location2 = entry.getValue();
            baseLayerLocationsStringList.add(location1.getWorld().getName() + "," + location1.getX() + "," + location1.getY() + "," + location1.getZ() + "|" +
                    location2.getWorld().getName() + "," + location2.getX() + "," + location2.getY() + "," + location2.getZ());
        }
        data.set("baseLayerLocations", baseLayerLocationsStringList);

        ArrayList<String> ingredientPlacementLocationsStringList = new ArrayList<>();
        for (Map.Entry<Location, Location> entry : dataHandler.getIngredientPlacementLocations().entrySet()) {
            Location location1 = entry.getKey();
            Location location2 = entry.getValue();
            ingredientPlacementLocationsStringList.add(location1.getWorld().getName() + "," + location1.getX() + "," + location1.getY() + "," + location1.getZ() + "|" +
                    location2.getWorld().getName() + "," + location2.getX() + "," + location2.getY() + "," + location2.getZ());
        }
        data.set("ingredientPlacementLocations", ingredientPlacementLocationsStringList);

        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
            log.severe("----------");
            log.severe("Error occurred while attempting to save data to data-file, any recent data will be lost!");
        }

        log.info("Plugin stopped!");
    }
}
