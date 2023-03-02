/*
 *   Copyright 2023 Raphael Roehrig (alias "raffel080108"). All rights reserved.
 *   Licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 *   You may not use any content of this file or software except in compliance with the license.
 *   You can obtain a copy of the license here: https://creativecommons.org/licenses/by-nc/4.0/legalcode
 */

package raffel080108.altarcrafting.utils;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;

public final class MMOItemsItemHandler {
    public ItemStack getMMOItemsItem(String type, String id) {
        return MMOItems.plugin.getItem(MMOItems.plugin.getTypes().get(type), id);
    }
}
