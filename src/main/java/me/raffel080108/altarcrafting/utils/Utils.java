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

    “Commons Clause” License Condition v1.0

    The Software is provided to you by the Licensor under the License, as defined above, subject to the following condition.

    Without limiting other conditions in the License, the grant of rights under the License will not include, and the License does not grant to you, right to Sell the Software.

    For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you under the License to provide to third parties, for a fee or other consideration (including without limitation fees for hosting or consulting/ support services related to the Software), a product or service whose value derives, entirely or substantially, from the functionality of the Software.  Any license notice or attribution required by the License must also include this Commons Cause License Condition notice.

    Software: AltarCrafting Plugin (https://github.com/raffel080108/AltarCrafting; https://www.spigotmc.org/resources/altarcrafting.107980/)
    License: Apache License Version 2.0
    Licensor: Raphael Roehrig (raffel080108)
 */

package me.raffel080108.altarcrafting.utils;

import me.raffel080108.altarcrafting.AltarCrafting;
import me.raffel080108.altarcrafting.data.DataHandler;
import org.apache.commons.collections4.MultiValuedMap;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class Utils {
    private DataHandler dataHandler;

    public Utils(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public void cancelAltarCraftingSession(Player player) {
        HashMap<Player, Location> playerCraftingAltarLocations = dataHandler.getPlayerCraftingAltarLocations();
        MultiValuedMap<Location, ItemStack> itemsPlacedForCrafting = dataHandler.getItemsPlacedForCrafting();

        if (!playerCraftingAltarLocations.containsKey(player))
            return;
        Location altarLocation = playerCraftingAltarLocations.get(player);

        cancelCraftTimeout(player);
        playerCraftingAltarLocations.remove(player);

        if (!itemsPlacedForCrafting.containsKey(altarLocation))
            return;
        for (ItemStack item : itemsPlacedForCrafting.get(altarLocation)) {
            Collection<ItemFrame> foundItemFrames = dataHandler.getPlacedItemsLocations().get(item).getNearbyEntitiesByType(ItemFrame.class, 0.5D);
            for (ItemFrame itemFrame : foundItemFrames)
                if (itemFrame.getPersistentDataContainer().has(dataHandler.getIngredientAmountKey()))
                    itemFrame.remove();

            if (!player.getInventory().addItem(item).isEmpty()) {
                Location playerLocation = player.getLocation();
                World world = playerLocation.getWorld();
                world.dropItem(playerLocation, item);
            } else player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1000F, 1F);
            dataHandler.getPlacedItemsLocations().remove(item);
        }
        itemsPlacedForCrafting.remove(altarLocation);
    }

    public void cancelCraftTimeout(Player player) {
        HashMap<Player, BukkitTask> activeCraftTimeoutTasks = dataHandler.getActiveCraftingTimeoutTasks();
        if (activeCraftTimeoutTasks.containsKey(player)) {
            activeCraftTimeoutTasks.get(player).cancel();
            activeCraftTimeoutTasks.remove(player);
        }
    }

    public boolean validConfigCheck(File config) {
        AltarCrafting main = dataHandler.getMainInstance();
        Logger log = dataHandler.getLogger();
        boolean error = false;

        try {
            new YamlConfiguration().load(config);
        } catch (IOException e) {
            e.printStackTrace();
            error = true;
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            error = true;
            try {
                String newFileName = "old_" + config.getName();
                Files.copy(Paths.get(config.getPath()), Paths.get(new File(main.getDataFolder(), newFileName).getPath()), REPLACE_EXISTING);
                main.saveResource("config.yml", true);
                log.warning("----------\nInvalid configuration detected - Current configuration was backed up to " + newFileName + " and a new file containing the corresponding default configuration generated");
            } catch (IOException e2) {
                e2.printStackTrace();
                log.severe("----------\nInvalid configuration detected - Backup failed");
            }
        }
        return error;
    }

    public void loadRecipes() {
        HashMap<String, Map<ItemStack, HashMap<ItemStack, Boolean>>> recipes = dataHandler.getRecipes();
        Logger log = dataHandler.getLogger();

        ConfigurationSection altars = dataHandler.getConfig().getConfigurationSection("altars");
        if (altars == null) {
            log.severe("Could not find configuration-section \"altars\", while attempting to read recipes from the configuration. Please check your configuration");
            return;
        }

        recipes.clear();
        for (String altar : altars.getKeys(false)) {
            String altarParamsPath = "altars." + altar;
            ConfigurationSection altarParams = altars.getConfigurationSection(altar);
            if (altarParams == null) {
                log.severe("Could not find parameters for altar at path " + altarParamsPath + ", while attempting to read recipes from the configuration");
                return;
            }

            ConfigurationSection recipesConfigSection = altarParams.getConfigurationSection("recipes");
            if (recipesConfigSection == null) {
                log.severe("Could not find parameter \"recipes\" for altar at path " + altarParamsPath + ", while attempting to read recipes from the configuration");
                return;
            }

            Set<String> recipesKeys = recipesConfigSection.getKeys(false);
            recipesLoop:
            for (String recipe : recipesKeys) {
                ConfigurationSection recipeParams = recipesConfigSection.getConfigurationSection(recipe);
                String recipeParamsPath = recipesConfigSection.getCurrentPath() + "." + recipe;
                if (recipeParams == null) {
                    log.warning("Could not find parameters for recipe at path " + recipeParamsPath + ", while attempting to read recipes from the configuration");
                    continue;
                }

                ConfigurationSection result = recipeParams.getConfigurationSection("result");
                if (result == null) {
                    log.warning("Could not find parameter \"result\" for recipe at path " + recipeParamsPath + ", while attempting to read recipes from the configuration");
                    continue;
                }

                String actionType = recipeParams.getString("on-completion");
                if (actionType == null) {
                    log.warning("Could not find parameter \"on-completion\" for recipe at path " + recipeParamsPath + ", while attempting to read recipes from the configuration. Assuming default value of \"item\"");
                    actionType = "item";
                }

                Map<ItemStack, Boolean> resultItemMap = new HashMap<>();
                if (!actionType.equalsIgnoreCase("command")) {
                    resultItemMap = getItemFromParams(result);
                }

                if (resultItemMap == null)
                    continue;

                ConfigurationSection ingredients = recipeParams.getConfigurationSection("ingredients");
                if (ingredients == null) {
                    log.warning("Could not find parameter \"ingredients\" for recipe at path " + recipeParamsPath + ", while attempting to read recipes from the configuration");
                    continue;
                }

                HashMap<ItemStack, Boolean> ingredientsItems = new HashMap<>();
                for (String ingredientString : ingredients.getKeys(false)) {
                    ConfigurationSection ingredientParams = ingredients.getConfigurationSection(ingredientString);
                    if (ingredientParams == null) {
                        log.warning("Could not find parameters for ingredient at path " + ingredients.getCurrentPath() + "." + ingredientString + ", while attempting to read recipes from the configuration");
                        continue;
                    }

                    Map<ItemStack, Boolean> itemForMap = getItemFromParams(ingredientParams);
                    if (itemForMap == null)
                        continue recipesLoop;

                    if (ingredientsItems.containsKey(itemForMap.entrySet().iterator().next().getKey())) {
                        log.warning("Found duplicate item while reading recipe at path " + recipeParamsPath + " - The recipe will not be cached. Please make sure there are no items listed as ingredients that have exactly matching nbt-data. Please note, that issues with other recipes might occur because of this");
                        continue recipesLoop;
                    }

                    ingredientsItems.putAll(itemForMap);
                }

                HashMap<ItemStack, HashMap<ItemStack, Boolean>> mapToPut = new HashMap<>();
                if (resultItemMap.isEmpty()) {
                    mapToPut.put(null, ingredientsItems);
                } else mapToPut.put(resultItemMap.entrySet().iterator().next().getKey(), ingredientsItems);

                recipes.put(recipeParamsPath, mapToPut);
            }
        }
    }

    private Map<ItemStack, Boolean> getItemFromParams(ConfigurationSection itemParams) {
        Logger log = dataHandler.getLogger();
        String itemParamsPath = itemParams.getCurrentPath();

        int amount = itemParams.getInt("amount");
        if (amount < 1)
            amount = 1;

        String mmoItemsType = itemParams.getString("mmoItems-type");
        String mmoItemsId = itemParams.getString("mmoItems-id");

        if (mmoItemsType != null && mmoItemsId != null) {
            if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
                ItemStack mmoitemsItem = new MMOItemsItemHandler().getMMOItemsItem(mmoItemsType, mmoItemsId);
                if (mmoitemsItem == null) {
                    log.severe("Found invalid mmoItems-item item-data for item at path " + itemParamsPath + ", while attempting to read recipes from the configuration");
                    return null;
                }
                mmoitemsItem.setAmount(amount);
                return Map.of(mmoitemsItem, false);
            } else
                log.severe("Could not find MMOItems plugin, while attempting to read MMOItems item-data. Please check if the plugin is correctly installed and accessible by this plugin");
        }

        String materialString = itemParams.getString("material");
        if (materialString == null) {
            log.severe("Could not find parameter \"material\" for item at path " + itemParamsPath + ", while attempting to read recipes from the configuration");
            return null;
        }
        Material material = Material.matchMaterial(materialString);
        if (material == null) {
            log.severe("Found invalid value for parameter \"material\" for item at path " + itemParamsPath + ", while attempting to read recipes from the configuration");
            return null;
        }

        ItemStack item = new ItemStack(material, amount);

        ItemMeta meta = item.getItemMeta();
        ConfigurationSection nbtParams = itemParams.getConfigurationSection("nbt");
        if (nbtParams != null) {
            String name = nbtParams.getString("name");
            if (name != null)
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> lore = nbtParams.getStringList("lore");
            if (lore.size() > 0) {
                int index = 0;
                for (String loreLine : lore) {
                    lore.set(index, ChatColor.translateAlternateColorCodes('&', loreLine));
                    index++;
                }
                meta.setLore(lore);
            }

            ConfigurationSection enchantments = nbtParams.getConfigurationSection("enchantments");
            if (enchantments != null) {
                for (String enchantmentString : enchantments.getKeys(false)) {
                    int enchantmentLevel = enchantments.getInt(enchantmentString);
                    if (enchantmentLevel < 0) {
                        log.warning("Found invalid or missing value for enchantment-level for enchantment at path " + enchantments + "." + enchantmentString + ", default value (1) will be used");
                        enchantmentLevel = 1;
                    }

                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentString));
                    if (enchantment != null)
                        meta.addEnchant(enchantment, enchantmentLevel, true);
                }
            }
            if (nbtParams.getBoolean("hide-enchants"))
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }
        return Map.of(item, nbtParams == null && itemParams.getBoolean("ignore-nbt"));
    }

    public boolean loadConfigurations() {
        AltarCrafting main = dataHandler.getMainInstance();
        Logger log = dataHandler.getLogger();
        boolean success = true;

        log.info("Loading config...");
        if (!new File(main.getDataFolder(), "config.yml").exists())
            main.saveResource("config.yml", false);

        File configFile = new File(main.getDataFolder(), "config.yml");
        if (validConfigCheck(configFile))
            success = false;

        dataHandler.setConfig(YamlConfiguration.loadConfiguration(configFile));

        log.info("Loading messages...");
        if (!new File(main.getDataFolder(), "messages.yml").exists())
            main.saveResource("messages.yml", false);

        File messagesFile = new File(main.getDataFolder(), "messages.yml");
        if (validConfigCheck(messagesFile))
            success = false;

        dataHandler.setMessages(YamlConfiguration.loadConfiguration(messagesFile));

        log.info("Loading recipes...");
        loadRecipes();

        log.info("Reload complete!");
        return success;
    }
}
