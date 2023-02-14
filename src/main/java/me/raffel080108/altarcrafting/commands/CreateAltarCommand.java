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
