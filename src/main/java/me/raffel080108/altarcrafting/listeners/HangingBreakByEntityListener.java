/*
 *   Copyright 2023 Raphael Roehrig (alias "raffel080108"). All rights reserved.
 *   Licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 *   You may not use any content of this file or software except in compliance with the license
 *   You can obtain a copy of the license here: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

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
