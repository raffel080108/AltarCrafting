/*
 AltarCrafting © 2023 by Raphael "raffel080108" Roehrig is licensed under CC BY-NC 4.0. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc/4.0/
 */

package raffel080108.altarcrafting.utils;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;

public final class MMOItemsItemHandler {
    public ItemStack getMMOItemsItem(String type, String id) {
        return MMOItems.plugin.getItem(MMOItems.plugin.getTypes().get(type), id);
    }
}
