package me.raffel080108.altarcrafting.listeners;

import me.raffel080108.altarcrafting.DataHandler;
import me.raffel080108.altarcrafting.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {
    private final DataHandler dataHandler;
    private final Utils utils;

    public PlayerQuitListener(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.utils = new Utils(dataHandler);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void playerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dataHandler.getPendingAltarCreations().remove(player);
        utils.cancelAltarCraftingSession(player);
    }
}
