package eu.pb4.buildbattle.game;

import net.minecraft.entity.boss.BossBar;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.BossBarWidget;
import net.minecraft.text.Text;

public final class TimerBar {
    private final BossBarWidget widget;

    public TimerBar(GlobalWidgets widgets) {
        this.widget = widgets.addBossBar(Text.empty(), BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
    }

    public void update(Text text, float progress) {
        this.widget.setProgress(progress);
        this.widget.setTitle(text);
    }

    public void setColor(BossBar.Color color) {
        this.widget.setStyle(color, BossBar.Style.NOTCHED_10);
    }
}
