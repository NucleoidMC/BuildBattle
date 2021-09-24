package eu.pb4.buildbattle.other;

import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.mixin.CreativeActionPacketAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class BbUtils {
    public static Identifier id(String path) {
        return new Identifier(BuildBattle.ID, path);
    }

    public static MutableText getText(String type, String path, Object... values) {
        return new TranslatableText(Util.createTranslationKey(type, new Identifier(BuildBattle.ID, path)), values);
    }

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
