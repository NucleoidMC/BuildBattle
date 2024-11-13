package eu.pb4.buildbattle.other;


import net.minecraft.text.*;

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

    // todo
    /*
    public static HologramElement[] getHologramLines(GameSpace game, BuildBattleConfig config) {
        ArrayList<HologramElement> out = new ArrayList<>();
        out.add(new SpinningItemHologramElement(game.getMetadata().sourceConfig().icon()));
        out.add(new StaticTextHologramElement(Text.empty().append(game.getMetadata().sourceConfig().name()).setStyle(Style.EMPTY.withColor(0xffae36).withBold(true))));
        out.add(new SpacingHologramElement(0.2f));
        String type = config.gamemode();
        for (int x = 1; x <= 7; x++) {
            out.add(new StaticTextHologramElement(Text.translatable("description.buildbattle." + type + "." + x, (config.timeLimitSecs() / 60))));
        }

        return out.toArray(new HologramElement[0]);
    }*/
}
