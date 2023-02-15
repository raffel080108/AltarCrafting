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

package me.raffel080108.altarcrafting.commands;

import me.raffel080108.altarcrafting.data.DataHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import me.raffel080108.altarcrafting.utils.Utils;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Optional;

import java.util.HashMap;

public final class CancelAltarCraftingCommand {
    private final DataHandler dataHandler;
    private final Utils utils;

    public CancelAltarCraftingCommand(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.utils = new Utils(dataHandler);
    }

    @Command("cancelAltarCrafting")
    private void cancelCraftingCommand(Player sender, @Named("player") @Optional(def = "") @AutoCompleteOnlinePlayers String target) {
        HashMap<Player, Location> playerCraftingAltarLocations = dataHandler.getPlayerCraftingAltarLocations();
        FileConfiguration messages = dataHandler.getMessages();

        if (target.equals("") || target.equalsIgnoreCase(sender.getName())) {
            if (playerCraftingAltarLocations.containsKey(sender)) {
                utils.cancelAltarCraftingSession(sender);
                String message = messages.getString("crafting-session-cancelled");
                sender.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§6Your altar-crafting-session was cancelled and all placed items returned to your inventory");
            } else {
                String message = messages.getString("error-self-no-active-crafting-session");
                sender.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cYou do not have an active altar-crafting-session");
            }
        } else {
            if (!sender.hasPermission("altarCrafting.cancelAltarCrafting")) {
                String message = messages.getString("error-no-permission");
                sender.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cYou do not have permission to execute this command!");
                return;
            }

            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                String message = messages.getString("error-player-not-found");
                sender.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message).replace("%target%", target) : "§cCould not find player " + target);
                return;
            }

            if (playerCraftingAltarLocations.containsKey(targetPlayer)) {
                utils.cancelAltarCraftingSession(targetPlayer);
                String message = messages.getString("crafting-session-cancelled");
                targetPlayer.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§6Your altar-crafting-session was cancelled and all placed items returned to your inventory");
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1000F, 1F);
            } else {
                String message = messages.getString("error-target-no-active-crafting-session");
                sender.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message).replace("%target%", target) : "§c" + target + " does not have an active altar-crafting-session");
            }
        }
    }
}
