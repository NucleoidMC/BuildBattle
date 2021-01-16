package eu.pb4.buildbattle.custom;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.fake.FakeItem;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class VotingItem extends Item implements FakeItem {
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
    public Item asProxy() {
        return proxy;
    }

    public ItemStack getItemStack() {
        return ItemStackBuilder.of(this).setName(new TranslatableText("buildbattle.item.vote" + this.score).formatted(this.formatting)).build();
    }
}
