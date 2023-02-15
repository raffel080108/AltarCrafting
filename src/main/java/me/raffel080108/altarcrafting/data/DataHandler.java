/*
   Copyright 2023 Raphael Roehrig (raffel080108)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package me.raffel080108.altarcrafting.data;

import me.raffel080108.altarcrafting.AltarCrafting;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class DataHandler {
    private final AltarCrafting main;
    private final NamespacedKey ingredientAmountKey;
    private FileConfiguration config;
    private FileConfiguration messages;
    private final Logger logger;
    private final HashMap<Location, String> altarLocations = new HashMap<>();
    private final HashMap<Location, Location> baseLayerLocations = new HashMap<>();
    private final HashMap<Location, Location> ingredientPlacementLocations = new HashMap<>();
    private final HashMap<String, Map<ItemStack, HashMap<ItemStack, Boolean>>> recipes = new HashMap<>();
    private final HashMap<Player, Location> playerCraftingAltarLocations = new HashMap<>();
    private final MultiValuedMap<Location, ItemStack> itemsPlacedForCrafting = new ArrayListValuedHashMap<>();
    private final HashMap<ItemStack, Location> placedItemsLocations = new HashMap<>();
    private final HashMap<Player, String> pendingAltarCreations = new HashMap<>();
    private final HashMap<Player, BukkitTask> activeCraftTimeoutTasks = new HashMap<>();
    private final ArrayList<Player> craftingInProgress = new ArrayList<>();
    private final HashMap<Player, Long> interactEventCooldown = new HashMap<>();

    public DataHandler(AltarCrafting main) {
        this.main = main;
        ingredientAmountKey = new NamespacedKey(main, "ingredientAmount");
        config = main.getConfig();
        logger = main.getLogger();
    }

    public AltarCrafting getMainInstance() {
        return main;
    }

    public NamespacedKey getIngredientAmountKey() {
        return ingredientAmountKey;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }

    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public void setMessages(FileConfiguration messages) {
        this.messages = messages;
    }
    
    public Logger getLogger() {
        return logger;
    }

    public HashMap<Location, String> getAltarLocations() {
        return altarLocations;
    }

    public HashMap<Location, Location> getBaseLayerLocations() {
        return baseLayerLocations;
    }

    public HashMap<Location, Location> getIngredientPlacementLocations() {
        return ingredientPlacementLocations;
    }

    public HashMap<String, Map<ItemStack, HashMap<ItemStack, Boolean>>> getRecipes() {
        return recipes;
    }

    public HashMap<Player, Location> getPlayerCraftingAltarLocations() {
        return playerCraftingAltarLocations;
    }

    public MultiValuedMap<Location, ItemStack> getItemsPlacedForCrafting() {
        return itemsPlacedForCrafting;
    }

    public HashMap<ItemStack, Location> getPlacedItemsLocations() {
        return placedItemsLocations;
    }

    public HashMap<Player, String> getPendingAltarCreations() {
        return pendingAltarCreations;
    }
    
    public HashMap<Player, BukkitTask> getActiveCraftingTimeoutTasks() {
        return activeCraftTimeoutTasks;
    }

    public ArrayList<Player> getCraftingInProgress() {
        return craftingInProgress;
    }

    public HashMap<Player, Long> getInteractEventCooldown() {
        return interactEventCooldown;
    }
}
