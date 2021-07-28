package eu.pb4.buildbattle.other;

import eu.pb4.buildbattle.mixin.CreativeActionPacketAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

public class GeneralUtils {
    public static boolean equalsOrInstance(Object tested, Object... objects) {
        boolean result;
        for (Object object : objects) {
            if (object instanceof Class<?> clazz) {
                result = clazz.isInstance(tested);
            } else {
                result = tested.equals(object);
            }

            if (result) {
                return true;
            }
        }
        return false;
    }


    public static void setCreativeStack(CreativeInventoryActionC2SPacket packet, ItemStack stack) {
        ((CreativeActionPacketAccessor) packet).bb_setStack(stack);
    }
}
