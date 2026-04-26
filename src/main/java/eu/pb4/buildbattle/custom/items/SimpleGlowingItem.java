package eu.pb4.buildbattle.custom.items;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SimpleGlowingItem extends SimplePolymerItem {
    public SimpleGlowingItem(Properties settings, Item polymerItem) {
        super(settings, polymerItem);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
