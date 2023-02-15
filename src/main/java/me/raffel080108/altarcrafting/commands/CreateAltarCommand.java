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

package me.raffel080108.altarcrafting.commands;

import me.raffel080108.altarcrafting.data.DataHandler;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.HashMap;
import java.util.logging.Logger;

public final class CreateAltarCommand {
    private final DataHandler dataHandler;

    public CreateAltarCommand(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Command("createAltar")
    @CommandPermission("altarCrafting.createAltar")
    private void createAltarCommand(Player sender, @Named("altar-name") @AutoCompleteAltarsList String altarName) {
        HashMap<Player, String> pendingAltarCreations = dataHandler.getPendingAltarCreations();
        Logger log = dataHandler.getLogger();
        FileConfiguration messages = dataHandler.getMessages();

        ConfigurationSection altars = dataHandler.getConfig().getConfigurationSection("altars");
        if (altars == null) {
            log.severe("Could not find configuration-section \"altars\" while attempting to process execution of a \"/createAltar\" command. Please check your configuration");
            return;
        }

        ConfigurationSection altarParams = altars.getConfigurationSection(altarName);
        if (altarParams == null) {
            sender.sendMessage("§cCould not find altar by the name of \"" + altarName + "\"");
            return;
        }

        if (!pendingAltarCreations.containsKey(sender)) {
            pendingAltarCreations.put(sender, altarParams.getCurrentPath());
            String message = messages.getString("message-success-pending-altar-creation");
            sender.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§aPlease select the §a§ncenter block§r §aof the altar-structure to create the specified altar");
        } else {
            String message = messages.getString("message-error-pending-altar-creation");
            sender.sendMessage(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cYou already have a pending creation ongoing, select any block to cancel/execute it");
        }
    }
}
