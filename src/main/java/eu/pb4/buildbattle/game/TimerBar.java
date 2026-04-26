package eu.pb4.buildbattle.game;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;

public final class TimerBar {
    private final BossBarWidget widget;

    public TimerBar(GlobalWidgets widgets) {
        this.widget = widgets.addBossBar(Component.empty(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    }

    public void update(Component text, float progress) {
        this.widget.setProgress(progress);
        this.widget.setTitle(text);
    }

    public void setColor(BossEvent.BossBarColor color) {
        this.widget.setStyle(color, BossEvent.BossBarOverlay.NOTCHED_10);
    }
}
