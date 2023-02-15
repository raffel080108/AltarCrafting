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

package me.raffel080108.altarcrafting.listeners;

import me.raffel080108.altarcrafting.data.DataHandler;
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
