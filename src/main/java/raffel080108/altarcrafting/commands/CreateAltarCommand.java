/*
 AltarCrafting © 2023 by Raphael "raffel080108" Roehrig is licensed under CC BY-NC 4.0. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc/4.0/
 */

package raffel080108.altarcrafting.commands;

import raffel080108.altarcrafting.AltarCrafting;
import raffel080108.altarcrafting.commands.annotations.AutoCompleteAltarsList;
import raffel080108.altarcrafting.DataHandler;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.HashMap;
import java.util.logging.Logger;

public final class CreateAltarCommand {
    private final DataHandler dataHandler = DataHandler.getInstance();

    @Command("createAltar")
    @CommandPermission("altarCrafting.createAltar")
    private void createAltarCommand(Player sender, @Named("altar-name") @AutoCompleteAltarsList String altarName) {
        HashMap<Player, String> pendingAltarCreations = dataHandler.getPendingAltarCreations();
        Logger log = JavaPlugin.getPlugin(AltarCrafting.class).getLogger();
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
