package me.raffel080108.altarcrafting.commands;

import me.raffel080108.altarcrafting.DataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
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

        if (target.equals("") || target.equalsIgnoreCase(sender.getName())) {
            if (playerCraftingAltarLocations.containsKey(sender)) {
                utils.cancelAltarCraftingSession(sender);
                sender.sendMessage("§6Your altar-crafting-session was cancelled and all placed items returned to your inventory");
            } else sender.sendMessage("§cYou do not have an active altar-crafting-session");
        } else {
            if (!sender.hasPermission("altarCrafting.cancelAltarCrafting")) {
                sender.sendMessage("§cYou do not have permission to execute this command!");
                return;
            }

            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                sender.sendMessage("§cCould not find player " + target);
                return;
            }

            if (playerCraftingAltarLocations.containsKey(targetPlayer)) {
                utils.cancelAltarCraftingSession(targetPlayer);
                targetPlayer.sendMessage("§6Your altar-crafting-session was cancelled and all placed items returned to your inventory");
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1000F, 1F);
            } else sender.sendMessage("§c" + target + " does not have an active altar-crafting-session");
        }
    }
}
