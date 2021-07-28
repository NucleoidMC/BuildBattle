package eu.pb4.buildbattle.other;

import eu.pb4.buildbattle.game.BuildBattleConfig;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
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


    public static Text[] getAboutHologramText(GameSpace game, BuildBattleConfig config) {
        ArrayList<Text> out = new ArrayList<>();
        out.add(new LiteralText("").append(game.getSourceConfig().getName()).formatted(Formatting.GOLD));

        String type = "standard";
        for (int x = 1; x <= 8; x++) {
            out.add(new TranslatableText("text.buildbattle.about." + type + "." + x, (config.timeLimitSecs() / 60)));
        }

        return out.toArray(new Text[0]);
    }
}
