package eu.pb4.buildbattle.themes;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;

public class ThemeVotingManager {
    private final Object2IntMap<String> votes = new Object2IntArrayMap<>();
    private int voteCount = 0;
    private final List<String> possible;
    private final List<Gui> guis = new ArrayList<>();
    private boolean active = true;

    public ThemeVotingManager(Theme theme) {
        this.possible = theme != null ? theme.getMultipleRandom(5) : List.of("Tiny Potato", "Tiny Potato", "Tiny Potato", "Tiny Potato", "Tiny Potato");

        for (String possible : this.possible) {
            this.votes.put(possible, 0);
        }
    }

    public void addPlayer(ServerPlayer player) {
        for (var gui : this.guis) {
            if (gui.getPlayer() == player) {
                return;
            }
        }

        Gui gui = new Gui(player);
        this.guis.add(gui);
        gui.open();
    }


    private void updateGuis() {
        for (var entry : this.votes.object2IntEntrySet()) {
            for (Gui gui : this.guis) {
                gui.updateThemePercentage(entry.getKey(), entry.getIntValue(), this.voteCount);
            }
        }
    }


    public String getResultsAndClose() {
        this.active = false;
        for (Gui gui : this.guis) {
            gui.close();
        }

        String theme = null;
        int count = 0;

        for (var entry : this.votes.object2IntEntrySet()) {
            if (entry.getIntValue() > count || (entry.getIntValue() == count && Math.random() > 0.5)) {
                theme = entry.getKey();
                count = entry.getIntValue();
            }
        }
        return theme;
    }

    private class Gui extends SimpleGui {
        public String vote = null;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x5, player, false);

            for (String theme : ThemeVotingManager.this.possible) {
                this.updateThemePercentage(theme, 0, 1);
            }
            this.setTitle(Component.translatable("text.buildbattle.timer_bar.voting_theme"));
        }

        @Override
        public boolean onClick(int index, ClickType type, ContainerInput action, GuiElement element) {
            return super.onClick(index, type, action, element);
        }

        public void updateThemePercentage(String theme, int votes, int allVotes) {
            int pos = ThemeVotingManager.this.possible.indexOf(theme);
            float percentFloat = (float) votes / allVotes;
            int percent = Math.round(((float) votes / allVotes) * 100);

            GuiElementBuilder icon = new GuiElementBuilder(Items.BRICKS, Math.max(votes, 1))
                    .setName(Component.literal(theme).withStyle(ChatFormatting.YELLOW).append(Component.literal(" - " + percent + "%"))).hideDefaultTooltip();

            icon.setCallback(() -> {
                PlayerUtil.playSoundToPlayer(this.player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);

                if (this.vote != null) {
                    ThemeVotingManager.this.votes.put(this.vote, ThemeVotingManager.this.votes.getInt(this.vote) - 1);
                } else {
                    ThemeVotingManager.this.voteCount++;
                }
                ThemeVotingManager.this.votes.put(theme, ThemeVotingManager.this.votes.getInt(theme) + 1);
                this.vote = theme;
                ThemeVotingManager.this.updateGuis();
            });

            Item filledItem = Items.ORANGE_STAINED_GLASS_PANE;
            if (theme.equals(this.vote)) {
                icon.glow();
                filledItem = Items.GREEN_STAINED_GLASS_PANE;
            }

            int relativePos = pos * 9;

            this.setSlot(relativePos, icon);
            int slots = Math.round(percentFloat * 8);

            GuiElementBuilder filled = new GuiElementBuilder(filledItem).hideTooltip();

            GuiElementBuilder empty = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip();

            for (int x = 1; x <= slots; x++) {
                this.setSlot(relativePos + x, filled);
            }

            for (int x = 1 + slots; x <= 8; x++) {
                this.setSlot(relativePos + x, empty);
            }
        }

        @Override
        public void afterRemoval() {
            if (ThemeVotingManager.this.active) {
                this.open();
            }
            super.afterRemoval();
        }
    }
}
