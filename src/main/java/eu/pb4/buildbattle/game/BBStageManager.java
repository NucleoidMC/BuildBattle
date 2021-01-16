package eu.pb4.buildbattle.game;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.sound.SoundCategory;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.Flag;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.Set;

public class BBStageManager {
    private long closeTime = -1;
    public long finishTime = -1;
    public long waitTime = -1;
    private long startTime = -1;
    public boolean isVoting = false;
    public boolean isAfterWaiting = false;
    public boolean isFinished = false;
    public long nextVoteTime = 0;


    public BBStageManager() {

    }

    public void onOpen(long time, BBConfig config) {
        this.startTime = time - (time % 20) + (4 * 20) + 19;
        this.finishTime = this.startTime + (config.timeLimitSecs * 20);
    }

    public IdleTickResult tick(long time, GameSpace space) {
        // Game has finished. Wait a few seconds before finally closing the game.
        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                return IdleTickResult.GAME_CLOSED;
            }
            return IdleTickResult.TICK_FINISHED;
        }


        // Game has just finished. Transition to the waiting-before-close state.
        if (space.getPlayers().isEmpty() || this.isFinished) {
            this.closeTime = time + (5 * 20);

            return IdleTickResult.GAME_FINISHED;
        }

        if (this.waitTime > 0) {
            if (time < this.waitTime) {
                return IdleTickResult.TICK_FINISHED;
            }
            this.waitTime = -1;
        }


        if (time > this.finishTime) {
            if (this.isAfterWaiting && this.waitTime < 0) {
                long voteTime = time + 30 * 20;
                this.isAfterWaiting = false;
                if (!this.isVoting) {
                    this.isVoting = true;
                    this.finishTime = voteTime;
                    this.nextVoteTime = voteTime;
                    return IdleTickResult.BUILD_FINISHED_TICK;
                } else {
                    this.finishTime = voteTime;
                    this.nextVoteTime = voteTime;
                    return IdleTickResult.VOTE_NEXT;
                }
            } else {
                this.waitTime = time + (5 * 20);
                this.isAfterWaiting = true;
                return this.isVoting ? IdleTickResult.VOTE_WAIT : IdleTickResult.BUILD_WAIT;
            }
        }

        return (this.isVoting) ? IdleTickResult.VOTE_TICK : IdleTickResult.BUILD_TICK;
    }


    public enum IdleTickResult {
        BUILD_TICK,
        BUILD_WAIT,
        BUILD_FINISHED_TICK,
        VOTE_TICK,
        VOTE_WAIT,
        VOTE_NEXT,
        GAME_FINISHED,
        TICK_FINISHED,
        GAME_CLOSED,
    }
}
