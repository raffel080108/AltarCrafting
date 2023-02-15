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

package me.raffel080108.altarcrafting.utils;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;

public final class MMOItemsItemHandler {
    public ItemStack getMMOItemsItem(String type, String id) {
        return MMOItems.plugin.getItem(MMOItems.plugin.getTypes().get(type), id);
    }
}
