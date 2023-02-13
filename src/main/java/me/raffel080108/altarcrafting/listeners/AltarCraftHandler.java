package me.raffel080108.altarcrafting.listeners;

import me.raffel080108.altarcrafting.AltarCrafting;
import me.raffel080108.altarcrafting.DataHandler;
import me.raffel080108.altarcrafting.utils.Utils;
import org.apache.commons.collections4.MultiValuedMap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Logger;

public final class AltarCraftHandler implements Listener {
    private final AltarCrafting main;
    private final DataHandler dataHandler;
    private final Utils utils;

    public AltarCraftHandler(AltarCrafting main, DataHandler dataHandler) {
        this.main = main;
        this.dataHandler = dataHandler;
        this.utils = new Utils(dataHandler);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void playerInteractEvent(PlayerInteractEvent event) {
        NamespacedKey ingredientAmountKey = dataHandler.getIngredientAmountKey();
        HashMap<Player, Location> playerCraftingAltarLocations = dataHandler.getPlayerCraftingAltarLocations();
        HashMap<Location, Location> ingredientPlacementLocations = dataHandler.getIngredientPlacementLocations();
        HashMap<ItemStack, Location> placedItemsLocations = dataHandler.getPlacedItemsLocations();
        HashMap<Location, String> altarLocations = dataHandler.getAltarLocations();
        MultiValuedMap<Location, ItemStack> itemsPlacedForCrafting = dataHandler.getItemsPlacedForCrafting();
        HashMap<Player, BukkitTask> activeCraftTimeoutTasks = dataHandler.getActiveCraftingTasks();
        ArrayList<Player> craftingInProgress = dataHandler.getCraftingInProgress();
        HashMap<Player, Long> interactEventCooldown = dataHandler.getInteractEventCooldown();

        if (event.useInteractedBlock().equals(Event.Result.DENY) || event.useItemInHand().equals(Event.Result.DENY))
            return;

        if (!event.getAction().isRightClick())
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;
        Location clickedLocation = clickedBlock.getLocation();
        boolean isIngredientPlacementLocation = ingredientPlacementLocations.containsKey(clickedLocation);
        if (!isIngredientPlacementLocation && !altarLocations.containsKey(clickedLocation))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();

        if (interactEventCooldown.containsKey(player))
            if (interactEventCooldown.get(player) + 150L > System.currentTimeMillis())
                return;
        interactEventCooldown.put(player, System.currentTimeMillis());

        if (craftingInProgress.contains(player)) {
            player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
            player.sendMessage("§cYou cannot use the altar, while there is crafting in progress!");
            return;
        }

        if (playerCraftingAltarLocations.containsValue(clickedLocation)) {
            if (!playerCraftingAltarLocations.get(player).equals(clickedLocation)) {
                player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
                player.sendMessage("§cAnother player is currently using this altar - Please wait until they finish");
                return;
            }
        }

        World world = clickedLocation.getWorld();

        FileConfiguration config = main.getConfig();
        Logger log = main.getLogger();
        if (isIngredientPlacementLocation) {
            Location itemFrameLocation = new Location(world, clickedLocation.getX() + 0.5D, clickedLocation.getY() + 1,
                    clickedLocation.getZ() + 0.5D);
            Location altarLocation = ingredientPlacementLocations.get(clickedLocation);
            ItemStack item = player.getInventory().getItemInMainHand();
            Material material = item.getType();
            if (material.isAir() || placedItemsLocations.containsValue(itemFrameLocation))
                return;

            if (itemsPlacedForCrafting.containsValue(item)) {
                player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
                player.sendMessage("§cAn item like this is already on the altar!");
                return;
            }

            if (playerCraftingAltarLocations.containsKey(player)) {
                if (!playerCraftingAltarLocations.get(player).equals(altarLocation)) {
                    utils.cancelAltarCraftingSession(player);
                    player.sendMessage("§6Your altar-crafting-session on a different altar was cancelled and a new one started for this altar");
                }
            }

            String altarParamsPath = altarLocations.get(ingredientPlacementLocations.get(clickedLocation));
            ConfigurationSection altarParams = config.getConfigurationSection(altarParamsPath);
            if (altarParams == null) {
                player.sendMessage("§cInternal error occurred while attempting to parse ingredient-placement");
                log.severe("Could not find parameters for altar at path " + altarParamsPath + ", while attempting to parse ingredient-placement for player " + player.getName());
                return;
            }

            long craftingTimeout = altarParams.getLong("crafting-timeout");
            if (craftingTimeout > 0 && !activeCraftTimeoutTasks.containsKey(player))
                activeCraftTimeoutTasks.put(player,
                        Bukkit.getScheduler().runTaskLater(main, () -> {
                            utils.cancelAltarCraftingSession(player);
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1000F, 1F);
                            player.sendMessage("§cYour altar-crafting-session timed out - All placed items have been returned to your inventory");
                        }, craftingTimeout));

            player.getEquipment().setItemInMainHand(null);

            ItemFrame itemFrame = world.spawn(itemFrameLocation, ItemFrame.class);
            itemFrame.setVisible(false);
            itemFrame.setFixed(true);
            itemFrame.setItem(item, false);
            itemFrame.getPersistentDataContainer().set(ingredientAmountKey, PersistentDataType.INTEGER, item.getAmount());

            world.playSound(itemFrameLocation, Sound.BLOCK_SMITHING_TABLE_USE, 1F, 1F);

            playerCraftingAltarLocations.put(player, altarLocation);
            itemsPlacedForCrafting.put(altarLocation, item);
            placedItemsLocations.put(item, itemFrameLocation);
        } else {
            Collection<ItemStack> placedItems = itemsPlacedForCrafting.get(clickedLocation);
            if (placedItems.size() == 0) {
                player.playSound(playerLocation, Sound.ENTITY_VILLAGER_NO, 1000F, 1F);
                player.sendMessage("§cPlease place items on the altar before trying to craft something");
                return;
            }

            String playerErrorMsg = "§cInternal error occurred while attempting to parse valid-recipe-check";
            String altarParamsPath = altarLocations.get(clickedLocation);
            ConfigurationSection altarParams = config.getConfigurationSection(altarParamsPath);
            if (altarParams == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, 1000F, 1F);
                player.sendMessage(playerErrorMsg);
                log.severe("Could not find parameters for altar at path " + altarParamsPath + ", while attempting to parse a valid-recipe-check");
                return;
            }

            ConfigurationSection altarRecipes = altarParams.getConfigurationSection("recipes");
            if (altarRecipes == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, 1000F, 1F);
                player.sendMessage(playerErrorMsg);
                log.severe("Could not find parameter \"recipes\" for altar at path " + altarParamsPath + ", while attempting to parse a valid-recipe-check");
                return;
            }

            for (String altarRecipe : altarRecipes.getKeys(false)) {
                String recipePath = altarRecipes.getCurrentPath() + "." + altarRecipe;
                Map<ItemStack, Map<ItemStack, Boolean>> recipeMap = dataHandler.getRecipes().get(recipePath);
                if (recipeMap == null) {
                    log.severe("Could not find cached recipe for path " + recipePath + ", during a valid-recipe-check. You can try to fix this by reloading the configuration. If the issue persists, there is most likely an issue with your configuration");
                    continue;
                }

                for (Map.Entry<ItemStack, Map<ItemStack, Boolean>> recipe : recipeMap.entrySet()) {
                    Map<ItemStack, Boolean> ingredients = recipe.getValue();
                    int matchedItemsAmount = 0;

                    for (ItemStack placedItem : placedItems) {
                        for (Map.Entry<ItemStack, Boolean> entry : ingredients.entrySet()) {
                            ItemStack ingredient = entry.getKey();

                            if (entry.getValue()) {
                                ItemStack placedItemNoNbt = placedItem.clone();
                                placedItemNoNbt.setItemMeta(null);
                                if (!ingredient.equals(placedItemNoNbt))
                                    continue;
                            } else if (!ingredient.equals(placedItem))
                                continue;

                            matchedItemsAmount++;
                            break;
                        }
                    }

                    if (matchedItemsAmount != placedItems.size() || matchedItemsAmount != ingredients.size())
                        continue;

                    ItemStack result = recipe.getKey();

                    ConfigurationSection recipeParams = config.getConfigurationSection(recipePath);
                    if (recipeParams == null) {
                        player.sendMessage(playerErrorMsg);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, 1000F, 1F);
                        log.severe("Could not find parameters for recipe at path " + recipePath + ", while attempting to parse a valid-recipe-check");
                        return;
                    }

                    utils.cancelCraftTimeout(player);

                    String recipePermission = recipeParams.getString("permission");
                    if (recipePermission != null) {
                        if (!player.hasPermission(recipePermission)) {
                            utils.cancelAltarCraftingSession(player);
                            String noPermissionMessage = recipeParams.getString("no-permission-message");
                            if (noPermissionMessage != null) {
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1000F, 1F);
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermissionMessage));
                                return;
                            } else break;
                        }
                    }

                    craftingInProgress.add(player);

                    boolean lightningEnabled = recipeParams.getBoolean("enable-lightning");
                    long completionDelay = recipeParams.getLong("completion-delay");

                    Location centerLocation = new Location(world, clickedLocation.getX() + 0.5D,
                            clickedLocation.getY() + 1, clickedLocation.getZ() + 0.5D);

                    if (completionDelay == 0) {
                        triggerCraftingComplete(world, centerLocation, result, lightningEnabled, clickedLocation);
                        return;
                    }

                    Particle particle = Particle.REDSTONE;
                    String particleType = recipeParams.getString("delay-particle-type");
                    if (particleType != null) {
                        try {
                            particle = Particle.valueOf(Particle.class, particleType);
                        } catch (IllegalArgumentException e) {
                            log.warning("Found invalid value for parameter \"delay-particle-type\" for recipe at path " + recipePath + ", defaulting to particle-type REDSTONE");
                        }
                    }

                    Particle.DustOptions particleData;
                    if (particle.equals(Particle.REDSTONE)) {
                        Color particleColor = Color.PURPLE;
                        String particleColorString = recipeParams.getString("delay-particle-color");
                        if (particleColorString != null) {
                            String[] splitString = particleColorString.split(", ");
                            try {
                                particleColor = Color.fromRGB(Integer.parseInt(splitString[0]), Integer.parseInt(splitString[1]), Integer.parseInt(splitString[2]));
                            } catch (IllegalArgumentException e) {
                                log.warning("Found invalid value for parameter \"delay-particle-color\" for recipe at path " + recipePath + ", defaulting to 128, 0, 128");
                            }
                        }
                        particleData = new Particle.DustOptions(particleColor, 1F);
                    } else
                        particleData = null;

                    ArrayList<Location> altarIngredientPlacementLocations = new ArrayList<>();
                    for (Map.Entry<Location, Location> entry : ingredientPlacementLocations.entrySet()) {
                        Location location = entry.getKey();
                        if (entry.getValue().equals(clickedLocation))
                            altarIngredientPlacementLocations.add(new Location(world, location.getX() + 0.5D,
                                    location.getY() + 1, location.getZ() + 0.5D));
                    }

                    ArrayList<Boolean> hasPlacedItem = new ArrayList<>();
                    locationsLoop:
                    for (Location location : altarIngredientPlacementLocations) {
                        Collection<ItemFrame> foundItemFrames = location.getNearbyEntitiesByType(ItemFrame.class, 0.5D);
                        for (ItemFrame itemFrame : foundItemFrames)
                            if (itemFrame.getPersistentDataContainer().has(ingredientAmountKey)) {
                                hasPlacedItem.add(true);
                                continue locationsLoop;
                            }
                        hasPlacedItem.add(false);
                    }

                    ArrayList<Location> startLocations = new ArrayList<>();
                    for (int i = 0; i < altarParams.getInt("altar-type"); i++)
                        if (hasPlacedItem.get(i))
                            startLocations.add(altarIngredientPlacementLocations.get(i));

                    BukkitScheduler scheduler = Bukkit.getScheduler();
                    ArrayList<BukkitTask> particleTasks = new ArrayList<>();

                    for (Location location : startLocations) {
                        Particle finalParticle = particle;
                        particleTasks.add(scheduler.runTaskTimer(main, () -> {
                            Vector vector = location.toVector();
                            for (double length = 0; length < location.distance(centerLocation);
                                 vector.add(centerLocation.toVector().clone().subtract(vector).normalize().multiply(0.1))) {
                                if (finalParticle.equals(Particle.REDSTONE))
                                    world.spawnParticle(Particle.REDSTONE, vector.getX(), vector.getY(), vector.getZ(), 1, particleData);
                                else world.spawnParticle(finalParticle, vector.getX(), vector.getY(), vector.getZ(), 1);

                                length += 0.1;
                            }
                        }, 0L, 10L));
                    }

                    BukkitTask soundTask = scheduler.runTaskTimer(main, () -> world.playSound(centerLocation, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1F, 1F), 0L, 20L);

                    scheduler.runTaskLater(main, () -> {
                        triggerCraftingComplete(world, centerLocation, result, lightningEnabled, clickedLocation);
                        soundTask.cancel();
                        for (BukkitTask task : particleTasks)
                            task.cancel();
                        craftingInProgress.remove(player);
                        playerCraftingAltarLocations.remove(player);
                    }, completionDelay);
                    return;
                }
            }
            utils.cancelAltarCraftingSession(player);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1000F, 1F);
            player.sendMessage("§cInvalid recipe, please try again");
        }
    }

    private void triggerCraftingComplete(World world, Location centerLocation, ItemStack result, boolean lightningEnabled, Location clickedLocation) {
        HashMap<ItemStack, Location> placedItemLocations = dataHandler.getPlacedItemsLocations();
        MultiValuedMap<Location, ItemStack> itemsPlacedForCrafting = dataHandler.getItemsPlacedForCrafting();

        if (lightningEnabled)
            world.strikeLightningEffect(centerLocation);
        else world.playSound(centerLocation, Sound.BLOCK_ANVIL_USE, 1L, 1L);

        Item droppedItem = world.dropItem(centerLocation, result);
        droppedItem.setVelocity(new Vector());

        for (ItemStack item : itemsPlacedForCrafting.get(clickedLocation)) {
            Collection<ItemFrame> foundItemFrames = placedItemLocations.get(item).getNearbyEntitiesByType(ItemFrame.class, 0.5D);
            for (ItemFrame itemFrame : foundItemFrames) {
                if (itemFrame.getPersistentDataContainer().has(dataHandler.getIngredientAmountKey())) {
                    itemFrame.remove();
                    break;
                }
            }
            placedItemLocations.remove(item);
        }
        itemsPlacedForCrafting.remove(clickedLocation);
    }
}
