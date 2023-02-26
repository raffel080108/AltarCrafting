/*
 *   Copyright 2023 Raphael Roehrig (alias "raffel080108"). All rights reserved.
 *   Licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 *   You may not use any content of this file or software except in compliance with the license
 *   You can obtain a copy of the license here: https://creativecommons.org/licenses/by-nc/4.0/legalcode
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
