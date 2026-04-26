package eu.pb4.buildbattle.ui;

import eu.pb4.buildbattle.game.PlayerData;
import eu.pb4.buildbattle.game.stages.BuildingStage;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

public class UtilsUi extends SimpleGui {
    private UtilsUi(ServerPlayer player, PlayerData data, BuildingStage stage) {
        super(MenuType.GENERIC_9x3, player, false);
    }

    public static void open(ServerPlayer player, PlayerData data, BuildingStage stage) {
        new UtilsUi(player, data, stage).open();
    }
}
