package eu.pb4.buildbattle;

import eu.pb4.buildbattle.game.BBConfig;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.GameLogic;

import java.util.ArrayList;

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


    public static Text[] getAboutHologramText(GameLogic game, BBConfig config) {
        ArrayList<Text> out = new ArrayList<>();
        out.add(new LiteralText(game.getSpace().getGameConfig().getOptionalName().orElse("Build Battle")).formatted(Formatting.GOLD));

        String type = config.isBuildSwap ? "buildswap" : "standard";
        for (int x = 1; x <= 8; x++) {
            out.add(new TranslatableText("buildbattle.about." + type + "." + x, (config.timeLimitSecs / 60)));
        }

        return out.toArray(new Text[0]);
    }
}
