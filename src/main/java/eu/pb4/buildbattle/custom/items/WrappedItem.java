package eu.pb4.buildbattle.custom.items;

import eu.pb4.buildbattle.custom.BBRegistry;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

public final class WrappedItem extends Item implements PolymerItem {
    public WrappedItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var item = getPolymerItem(context.getItemInHand(), PacketContext.get());
        if (item instanceof ShovelItem) {
            Items.IRON_SHOVEL.useOn(context);
        } else if (item instanceof AxeItem) {
            Items.IRON_AXE.useOn(context);
        } else if (item instanceof HoeItem) {
            Items.IRON_HOE.useOn(context);
        } else if (item == Items.BONE_MEAL) {
            item.useOn(context);
        }

        return super.useOn(context);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return itemStack.has(BBRegistry.WRAPPED_ITEM)
                ? itemStack.getOrDefault(BBRegistry.WRAPPED_ITEM, ItemStack.EMPTY).getItem()
                : Items.BARRIER;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context, HolderLookup.Provider provider) {
        if (itemStack.has(BBRegistry.WRAPPED_ITEM)) {
            return PolymerItemUtils.getPolymerItemStack(itemStack.getOrDefault(BBRegistry.WRAPPED_ITEM, ItemStack.EMPTY), tooltipType, context, provider);
        }
        return PolymerItemUtils.createItemStack(itemStack, tooltipType, context, provider);
    }

    public static ItemStack createWrapped(ItemStack stack) {
        var out = new ItemStack(BBRegistry.WRAPPED);
        out.setCount(stack.getCount());
        out.set(BBRegistry.WRAPPED_ITEM, stack.copy());
        return out;
    }
}
