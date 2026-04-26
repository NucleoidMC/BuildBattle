package eu.pb4.buildbattle.other;


import eu.pb4.buildbattle.game.BuildBattleConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;

public class TextHelper {
    public static MutableComponent getOrdinal(int number) {
        int x1 = number % 100;
        if (x1 > 10 && x1 < 20) {
            return Component.translatable("text.buildbattle.ordinal.th");
        }
        return switch (number % 10) {
            case 1 -> Component.translatable("text.buildbattle.ordinal.st");
            case 2 -> Component.translatable("text.buildbattle.ordinal.nd");
            case 3 -> Component.translatable("text.buildbattle.ordinal.rd");
            default -> Component.translatable("text.buildbattle.ordinal.th");
        };
    }

    public static Component getHologramLines(GameSpace game, BuildBattleConfig config) {
        var out = Component.empty();
        out.append(Component.empty().append(GameConfig.name(game.getMetadata().sourceConfig())).setStyle(Style.EMPTY.withColor(0xffae36).withBold(true)));
        out.append("\n\n");
        String type = config.gamemode();
        for (int x = 1; x <= 7; x++) {
            out.append(Component.translatable("description.buildbattle." + type + "." + x, (config.timeLimitSecs() / 60)));
            out.append("\n");
        }

        return out;
    }
}
