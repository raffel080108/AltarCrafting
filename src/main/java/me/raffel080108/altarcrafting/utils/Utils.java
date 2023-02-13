package me.raffel080108.altarcrafting.utils;

import me.raffel080108.altarcrafting.DataHandler;
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
import me.raffel080108.altarcrafting.AltarCrafting;
import revxrsal.commands.autocomplete.SuggestionProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class Utils {
    private AltarCrafting main = null;
    private DataHandler dataHandler;

    public Utils(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public Utils(AltarCrafting main, DataHandler dataHandler) {
        this.main = main;
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
        HashMap<Player, BukkitTask> activeCraftTimeoutTasks = dataHandler.getActiveCraftingTasks();
        if (activeCraftTimeoutTasks.containsKey(player)) {
            activeCraftTimeoutTasks.get(player).cancel();
            activeCraftTimeoutTasks.remove(player);
        }
    }

    public boolean invalidConfigCheck() {
        Logger log = dataHandler.getLogger();
        boolean success = true;

        try {
            new YamlConfiguration().load(new File(main.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            success = false;
            try {
                Files.copy(Paths.get(new File(main.getDataFolder(), "config.yml").getPath()), Paths.get(new File(main.getDataFolder(), "old_config.yml").getPath()), REPLACE_EXISTING);
                main.saveResource("config.yml", true);
                log.warning("----------\nInvalid config detected - Current configuration was backed up to old_config.yml and a new config.yml generated");
            } catch (IOException e2) {
                e2.printStackTrace();
                log.severe("----------\nInvalid config detected - Configuration backup failed");
            }
        }
        return success;
    }

    public void loadRecipes() {
        HashMap<String, Map<ItemStack, Map<ItemStack, Boolean>>> recipes = dataHandler.getRecipes();
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
                Map<ItemStack, Boolean> resultItemMap = getItemFromParams(result);
                if (resultItemMap == null)
                    continue;

                ConfigurationSection ingredients = recipeParams.getConfigurationSection("ingredients");
                if (ingredients == null) {
                    log.warning("Could not find parameter \"ingredients\" for recipe at path " + recipeParamsPath + ", while attempting to read recipes from the configuration");
                    continue;
                }

                HashMap<ItemStack, Boolean> ingredientsItems = new HashMap<>();
                ArrayList<Material> materials = new ArrayList<>();
                for (String ingredientString : ingredients.getKeys(false)) {
                    ConfigurationSection ingredientParams = ingredients.getConfigurationSection(ingredientString);
                    if (ingredientParams == null) {
                        log.warning("Could not find parameters for ingredient at path " + ingredients.getCurrentPath() + "." + ingredientString + ", while attempting to read recipes from the configuration");
                        continue;
                    }

                    Map<ItemStack, Boolean> itemForMap = getItemFromParams(ingredientParams);
                    if (itemForMap == null)
                        continue recipesLoop;

                    Material material = itemForMap.entrySet().iterator().next().getKey().getType();
                    if (materials.contains(material)) {
                        log.warning("Found duplicate material for recipe at path " + recipeParamsPath + ", ignoring it");
                        continue;
                    }
                    ingredientsItems.putAll(itemForMap);
                    materials.add(material);
                }

                recipes.put(recipeParamsPath, Map.of(resultItemMap.entrySet().iterator().next().getKey(), ingredientsItems));
            }
        }
    }

    private Map<ItemStack, Boolean> getItemFromParams(ConfigurationSection itemParams) {
        Logger log = dataHandler.getLogger();
        String itemParamsPath = itemParams.getCurrentPath();

        String mmoItemsType = itemParams.getString("mmoItems-type");
        String mmoItemsId = itemParams.getString("mmoItems-id");

        if (mmoItemsType != null && mmoItemsId != null) {
            if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
                ItemStack mmoitemsItem = new MMOItemsItemHandler().getMMOItemsItem(mmoItemsType, mmoItemsId);
                if (mmoitemsItem == null) {
                    log.severe("Found invalid mmoItems-item item-data for item at path " + itemParamsPath + ", while attempting to read recipes from the configuration");
                    return null;
                }
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

        int amount = itemParams.getInt("amount");
        if (amount < 1)
            amount = 1;

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

    public void setupAutoComplete() {
        ConfigurationSection entities = dataHandler.getConfig().getConfigurationSection("altars");
        if (entities != null)
            dataHandler.getAutoCompleter().registerSuggestion("altarsList", SuggestionProvider.of(entities.getKeys(false)));
        else
            dataHandler.getLogger().severe("Could not find configuration-section \"altars\" while attempting to register auto-completion. " +
                    "Please check your configuration");
    }
}
