package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.game.BBPlayer;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.util.BlockBounds;

import javax.security.auth.login.CredentialException;
import java.util.ArrayList;
import java.util.List;

public class BuildArena {
    public final BlockBounds area;
    public final BlockBounds ground;
    public final BlockBounds bounds;
    public final BlockBounds spawn;
    public final List<BBPlayer> players = new ArrayList();
    public int score = 0;

    public BuildArena(BlockBounds area, BlockBounds ground, BlockBounds bounds, BlockBounds spawn) {
        this.area = area;
        this.ground = ground;
        this.bounds = bounds;
        this.spawn = spawn;
    }

    public boolean canBuild(BlockPos blockPos, BBPlayer bbPlayer) {
        return this.players.contains(bbPlayer) && this.area.contains(blockPos);
    }
}
