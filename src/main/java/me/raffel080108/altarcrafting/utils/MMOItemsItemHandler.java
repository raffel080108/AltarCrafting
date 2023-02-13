package me.raffel080108.altarcrafting.utils;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;

public final class MMOItemsItemHandler {
    public ItemStack getMMOItemsItem(String type, String id) {
        return MMOItems.plugin.getItem(MMOItems.plugin.getTypes().get(type), id);
    }
}
