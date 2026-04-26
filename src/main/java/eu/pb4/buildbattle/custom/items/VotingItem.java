package eu.pb4.buildbattle.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VotingItem extends Item implements PolymerItem {
    private final Item proxy;
    public final int score;
    private final ChatFormatting formatting;

    public VotingItem(int score, ChatFormatting formatting, Item item, Properties settings) {
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
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider provider) {
        return null;
    }

    public Component getName(ItemStack stack) {
        return ((MutableComponent) super.getName(stack)).withStyle(this.formatting);
    }
}
