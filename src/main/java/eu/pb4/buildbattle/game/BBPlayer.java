package eu.pb4.buildbattle.game;

import eu.pb4.buildbattle.game.map.BuildArena;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class BBPlayer {
    public final BuildArena arena;
    public final PlayerRef playerRef;

    public int currentVote = 0;

    public BBPlayer(BuildArena arena, PlayerRef playerRef) {
        this.arena = arena;
        this.playerRef = playerRef;
        arena.players.add(this);
    }


    public int getAndClearCurrentVote() {
        int vote = this.currentVote;
        this.currentVote = 0;
        return vote;
    }
}
