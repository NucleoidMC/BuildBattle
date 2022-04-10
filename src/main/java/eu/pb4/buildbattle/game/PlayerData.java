package eu.pb4.buildbattle.game;

import eu.pb4.buildbattle.custom.BBItems;
import eu.pb4.buildbattle.game.map.BuildArena;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class PlayerData {
    public final BuildArena arena;
    public final PlayerRef playerRef;

    public int currentVote = BBItems.VOTE_OKAY.score;

    @Nullable
    public BlockPos selectionStart;
    @Nullable
    public BlockPos selectionEnd;
    public long lastTryFill;

    public void resetSelection() {
        this.selectionStart = null;
        this.selectionEnd = null;
        this.lastTryFill = -1;
    }

    public boolean isSelected() {
        return this.selectionEnd != null && this.selectionStart != null;
    }

    public PlayerData(BuildArena arena, PlayerRef playerRef) {
        this.arena = arena;
        this.playerRef = playerRef;
        arena.addPlayer(this);
    }


    public int getAndClearCurrentVote() {
        int vote = this.currentVote;
        this.currentVote = BBItems.VOTE_OKAY.score;
        return vote;
    }
}
