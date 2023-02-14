package me.raffel080108.altarcrafting.commands;

import me.raffel080108.altarcrafting.AltarCrafting;
import me.raffel080108.altarcrafting.DataHandler;
import me.raffel080108.altarcrafting.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;

public final class ReloadCommand {
    private final Utils utils;
    private final DataHandler dataHandler;

    public ReloadCommand(AltarCrafting main, DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.utils = new Utils(main, dataHandler);
    }

    @Command("altarCrafting reload")
    @CommandPermission("altarCrafting.reload")
    private void reloadCommand(CommandActor sender) {
        FileConfiguration messages = dataHandler.getMessages();

        dataHandler.getLogger().info("RELOADING...");
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
