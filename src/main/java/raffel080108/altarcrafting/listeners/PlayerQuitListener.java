/*
 AltarCrafting © 2023 by Raphael "raffel080108" Roehrig is licensed under CC BY-NC 4.0. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc/4.0/
 */

package raffel080108.altarcrafting.listeners;

import raffel080108.altarcrafting.DataHandler;
import raffel080108.altarcrafting.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {
    private final DataHandler dataHandler = DataHandler.getInstance();
    private final Utils utils = new Utils();

    @EventHandler(priority = EventPriority.MONITOR)
    private void playerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dataHandler.getPendingAltarCreations().remove(player);
        utils.cancelAltarCraftingSession(player);
    }
}
