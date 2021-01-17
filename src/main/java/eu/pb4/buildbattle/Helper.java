package eu.pb4.buildbattle;

import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;

public class Helper {
    public static MutableText getOrdinal(int number) {
        int x1 = number % 100;
        if (x1 > 10 && x1 < 20) {
            return new TranslatableText("buildbattle.ordinal.th");
        }
        switch (number % 10) {
            case 1:
                return new TranslatableText("buildbattle.ordinal.st");
            case 2:
                return new TranslatableText("buildbattle.ordinal.nd");
            case 3:
                return new TranslatableText("buildbattle.ordinal.rd");
            default:
                return new TranslatableText("buildbattle.ordinal.th");
        }
    }
}
