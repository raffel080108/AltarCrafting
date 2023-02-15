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
