package eu.pb4.buildbattle.ui;

import eu.pb4.buildbattle.game.PlayerData;
import eu.pb4.buildbattle.game.stages.BuildingStage;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

public class UtilsUi extends SimpleGui {
    private UtilsUi(ServerPlayerEntity player, PlayerData data, BuildingStage stage) {
        super(ScreenHandlerType.GENERIC_9X3, player, false);
    }

    public static void open(ServerPlayerEntity player, PlayerData data, BuildingStage stage) {
        new UtilsUi(player, data, stage).open();
    }
}
