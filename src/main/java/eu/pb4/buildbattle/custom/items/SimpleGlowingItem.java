package eu.pb4.buildbattle.custom.items;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SimpleGlowingItem extends SimplePolymerItem {
    public SimpleGlowingItem(Settings settings, Item polymerItem) {
        super(settings, polymerItem);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
