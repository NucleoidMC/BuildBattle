package eu.pb4.buildbattle.game;

import eu.pb4.buildbattle.game.map.BuildArena;

public class BBPlayer {
    public final BuildArena arena;

    public BBPlayer(BuildArena arena) {
        this.arena = arena;
        arena.players.add(this);
    }
}
