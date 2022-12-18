package eu.pb4.buildbattle.custom.items;

import eu.pb4.buildbattle.custom.BBItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class WrappedItem extends Item implements PolymerItem {
    public WrappedItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var item = getPolymerItem(context.getStack(), null);
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
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return itemStack.hasNbt() && itemStack.getNbt().contains("wrappedId", NbtElement.STRING_TYPE)
                ? Registries.ITEM.get(Identifier.tryParse(itemStack.getNbt().getString("wrappedId")))
                : Items.BARRIER;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, @Nullable ServerPlayerEntity player) {
        if (itemStack.hasNbt() && itemStack.getNbt().contains("wrappedId", NbtElement.STRING_TYPE)) {
            ItemStack out = new ItemStack(getPolymerItem(itemStack, player));
            out.setCount(itemStack.getCount());
            if (itemStack.getNbt().contains("wrappedNbt", NbtElement.COMPOUND_TYPE)) {
                out.setNbt(itemStack.getNbt().getCompound("wrappedNbt"));
            }
            return PolymerItemUtils.getPolymerItemStack(out, player);
        }
        return PolymerItemUtils.createItemStack(itemStack, player);
    }
    
    public static ItemStack createWrapped(ItemStack stack) {
        var out = new ItemStack(BBItems.WRAPPED);
        out.setCount(stack.getCount());
        out.getOrCreateNbt().putString("wrappedId", Registries.ITEM.getId(stack.getItem()).toString());
        if (stack.hasNbt()) {
            out.getOrCreateNbt().put("wrappedNbt", stack.getNbt());
        }
        return out;
    }
}
