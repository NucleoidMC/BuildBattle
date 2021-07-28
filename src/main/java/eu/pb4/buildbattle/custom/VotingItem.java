package eu.pb4.buildbattle.custom;

import eu.pb4.polymer.item.VirtualItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class VotingItem extends Item implements VirtualItem {
    private final Item proxy;
    public final int score;
    private final Formatting formatting;

    public VotingItem(int score, Formatting formatting, Item item, Settings settings) {
        super(settings);

        this.proxy = item;
        this.formatting = formatting;
        this.score = score;
    }

    @Override
    public Item getVirtualItem() {
        return this.proxy;
    }

    public Text getName() {
        return ((MutableText) super.getName()).formatted(this.formatting);
    }

    public Text getName(ItemStack stack) {
        return ((MutableText) super.getName(stack)).formatted(this.formatting);
    }
}
