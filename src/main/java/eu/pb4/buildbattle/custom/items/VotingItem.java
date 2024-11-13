package eu.pb4.buildbattle.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class VotingItem extends Item implements PolymerItem {
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
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return this.proxy;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }

    public Text getName(ItemStack stack) {
        return ((MutableText) super.getName(stack)).formatted(this.formatting);
    }
}
