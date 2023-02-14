package me.raffel080108.altarcrafting.listeners;

import me.raffel080108.altarcrafting.data.DataHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public final class HangingBreakByEntityListener implements Listener {
    private final DataHandler dataHandler;

    public HangingBreakByEntityListener(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void hangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(dataHandler.getIngredientAmountKey()))
            event.setCancelled(true);
    }
}
