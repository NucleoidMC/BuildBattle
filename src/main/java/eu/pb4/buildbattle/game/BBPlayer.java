package eu.pb4.buildbattle.game;

import eu.pb4.buildbattle.game.map.BuildArena;

public class BBPlayer {
    public final BuildArena arena;
    public int currentVote = 0;

    public BBPlayer(BuildArena arena) {
        this.arena = arena;
        arena.players.add(this);
    }


    public int getAndClearCurrentVote() {
        int vote = this.currentVote;
        this.currentVote = 0;
        return vote;
    }
}
