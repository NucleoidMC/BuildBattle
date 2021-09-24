package eu.pb4.buildbattle.other;

import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.holograms.api.elements.HologramElement;
import eu.pb4.holograms.api.elements.SpacingHologramElement;
import eu.pb4.holograms.api.elements.item.SpinningItemHologramElement;
import eu.pb4.holograms.api.elements.text.StaticTextHologramElement;
import net.minecraft.item.Items;
import net.minecraft.text.*;
import xyz.nucleoid.plasmid.game.GameSpace;

import java.util.ArrayList;

public class TextHelper {
    public static MutableText getOrdinal(int number) {
        int x1 = number % 100;
        if (x1 > 10 && x1 < 20) {
            return new TranslatableText("text.buildbattle.ordinal.th");
        }
        return switch (number % 10) {
            case 1 -> new TranslatableText("text.buildbattle.ordinal.st");
            case 2 -> new TranslatableText("text.buildbattle.ordinal.nd");
            case 3 -> new TranslatableText("text.buildbattle.ordinal.rd");
            default -> new TranslatableText("text.buildbattle.ordinal.th");
        };
    }


    public static HologramElement[] getHologramLines(GameSpace game, BuildBattleConfig config) {
        ArrayList<HologramElement> out = new ArrayList<>();
        out.add(new SpinningItemHologramElement(game.getMetadata().sourceConfig().icon()));
        out.add(new StaticTextHologramElement(new LiteralText("").append(game.getMetadata().sourceConfig().name()).setStyle(Style.EMPTY.withColor(0xffae36).withBold(true))));
        out.add(new SpacingHologramElement(0.2f));
        String type = config.gamemode();
        for (int x = 1; x <= 7; x++) {
            out.add(new StaticTextHologramElement(new TranslatableText("description.buildbattle." + type + "." + x, (config.timeLimitSecs() / 60))));
        }

        return out.toArray(new HologramElement[0]);
    }
}
