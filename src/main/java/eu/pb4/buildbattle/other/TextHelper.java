package eu.pb4.buildbattle.other;


import eu.pb4.buildbattle.game.BuildBattleConfig;
import net.minecraft.text.*;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;

public class TextHelper {
    public static MutableText getOrdinal(int number) {
        int x1 = number % 100;
        if (x1 > 10 && x1 < 20) {
            return Text.translatable("text.buildbattle.ordinal.th");
        }
        return switch (number % 10) {
            case 1 -> Text.translatable("text.buildbattle.ordinal.st");
            case 2 -> Text.translatable("text.buildbattle.ordinal.nd");
            case 3 -> Text.translatable("text.buildbattle.ordinal.rd");
            default -> Text.translatable("text.buildbattle.ordinal.th");
        };
    }

    public static Text getHologramLines(GameSpace game, BuildBattleConfig config) {
        var out = Text.empty();
        out.append(Text.empty().append(GameConfig.name(game.getMetadata().sourceConfig())).setStyle(Style.EMPTY.withColor(0xffae36).withBold(true)));
        out.append("\n\n");
        String type = config.gamemode();
        for (int x = 1; x <= 7; x++) {
            out.append(Text.translatable("description.buildbattle." + type + "." + x, (config.timeLimitSecs() / 60)));
            out.append("\n");
        }

        return out;
    }
}
