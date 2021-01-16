package eu.pb4.buildbattle.game;

import net.minecraft.entity.boss.BossBar;
import xyz.nucleoid.plasmid.widget.BossBarWidget;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class BBTimerBar {
    private final BossBarWidget widget;

    public BBTimerBar(GlobalWidgets widgets) {
        LiteralText title = new LiteralText("Waiting for the game to start...");
        this.widget = widgets.addBossBar(title, BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
    }

    public void update(long ticksUntilEnd, long totalTicksUntilEnd, String theme) {
        if (ticksUntilEnd % 20 == 0) {
            this.widget.setTitle(this.getText(ticksUntilEnd, theme));
            this.widget.setProgress((float) ticksUntilEnd / totalTicksUntilEnd);
        }
    }

    private Text getText(long ticksUntilEnd, String theme) {
        long secondsUntilEnd = ticksUntilEnd / 20;

        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;
        String time = String.format("§f%02d:%02d left §7- §eTheme: §f%s", minutes, seconds, theme);

        return new LiteralText(time);
    }
}
