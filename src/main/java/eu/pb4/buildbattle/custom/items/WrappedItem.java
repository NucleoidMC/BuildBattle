package eu.pb4.buildbattle.custom.items;

import eu.pb4.buildbattle.custom.BBRegistry;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.ActionResult;
import xyz.nucleoid.packettweaker.PacketContext;

public final class WrappedItem extends Item implements PolymerItem {
    public WrappedItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var item = getPolymerItem(context.getStack(), PacketContext.get());
        if (item instanceof ShovelItem) {
            Items.IRON_SHOVEL.useOnBlock(context);
        } else if (item instanceof AxeItem) {
            Items.IRON_AXE.useOnBlock(context);
        } else if (item instanceof HoeItem) {
            Items.IRON_HOE.useOnBlock(context);
        } else if (item == Items.BONE_MEAL) {
            item.useOnBlock(context);
        }

        return super.useOnBlock(context);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return itemStack.contains(BBRegistry.WRAPPED_ITEM)
                ? itemStack.getOrDefault(BBRegistry.WRAPPED_ITEM, ItemStack.EMPTY).getItem()
                : Items.BARRIER;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        if (itemStack.contains(BBRegistry.WRAPPED_ITEM)) {
            return PolymerItemUtils.getPolymerItemStack(itemStack.getOrDefault(BBRegistry.WRAPPED_ITEM, ItemStack.EMPTY), tooltipType, context);
        }
        return PolymerItemUtils.createItemStack(itemStack, tooltipType, context);
    }

    public static ItemStack createWrapped(ItemStack stack) {
        var out = new ItemStack(BBRegistry.WRAPPED);
        out.setCount(stack.getCount());
        out.set(BBRegistry.WRAPPED_ITEM, stack.copy());
        return out;
    }
}
