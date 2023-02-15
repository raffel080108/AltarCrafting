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
