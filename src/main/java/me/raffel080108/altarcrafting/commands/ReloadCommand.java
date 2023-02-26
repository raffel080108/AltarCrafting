/*
 *   Copyright 2023 Raphael Roehrig (alias "raffel080108"). All rights reserved.
 *   Licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 *   You may not use any content of this file or software except in compliance with the license
 *   You can obtain a copy of the license here: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package me.raffel080108.altarcrafting.commands;

import me.raffel080108.altarcrafting.data.DataHandler;
import me.raffel080108.altarcrafting.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;

public final class ReloadCommand {
    private final Utils utils;
    private final DataHandler dataHandler;

    public ReloadCommand(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.utils = new Utils(dataHandler);
    }

    @Command("altarCrafting reload")
    @CommandPermission("altarCrafting.reload")
    private void reloadCommand(CommandActor sender) {
        FileConfiguration messages = dataHandler.getMessages();

        dataHandler.getLogger().info("Reloading configurations...");
        if (utils.loadConfigurations()) {
            String message = messages.getString("message-reload-configuration-success");
            sender.reply(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§aConfiguration reloaded successfully");
            dataHandler.getLogger().info("Configuration reloaded by " + sender.getName());
        } else {
            String message = messages.getString("message-reload-configuration-error");
            sender.reply(message != null ? ChatColor.translateAlternateColorCodes('&', message) : "§cAn error occurred while reloading configurations, please check the server console for detailed error-log");
        }
    }
}
