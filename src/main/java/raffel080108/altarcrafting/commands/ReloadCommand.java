/*
 *   Copyright 2023 Raphael Roehrig (alias "raffel080108"). All rights reserved.
 *   Licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 *   You may not use any content of this file or software except in compliance with the license.
 *   You can obtain a copy of the license here: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package raffel080108.altarcrafting.commands;

import raffel080108.altarcrafting.AltarCrafting;
import raffel080108.altarcrafting.DataHandler;
import raffel080108.altarcrafting.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;

import java.util.logging.Logger;

public final class ReloadCommand {
    private final Utils utils = new Utils();
    private final DataHandler dataHandler = DataHandler.getInstance();

    @Command("altarCrafting reload")
    @CommandPermission("altarCrafting.reload")
    private void reloadCommand(CommandActor sender) {
        Logger log = JavaPlugin.getPlugin(AltarCrafting.class).getLogger();
        FileConfiguration messages = dataHandler.getMessages();

        log.info("Reloading configurations...");
        if (utils.loadConfigurations()) {
            String message = messages.getString("message-reload-configuration-success");
            sender.reply(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§aConfiguration reloaded successfully");
        } else {
            String message = messages.getString("message-reload-configuration-error");
            sender.reply(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cAn error occurred while reloading configurations, please check the server console for detailed error-log");
        }
        log.info("Configuration reloaded by " + sender.getName());
    }
}
