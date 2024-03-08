/*
 AltarCrafting © 2023 by Raphael "raffel080108" Roehrig is licensed under CC BY-NC 4.0. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc/4.0/
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
