package eu.pb4.buildbattle.game;

import xyz.nucleoid.plasmid.game.GameSpace;

public class BBStageManager {
    private long closeTime = -1;
    public long finishTime = -1;
    public long finishDiffTime = -1;
    public long waitTime = -1;
    public long votingTime = -1;

    public boolean isVoting = false;
    public boolean isAfterWaiting = false;
    public boolean isFinished = false;


    public BBStageManager() {

    }

    public void onOpen(long time, BBConfig config) {
        this.finishTime = time - (time % 20) + ((long) config.timeLimitSecs * 20);
        this.finishDiffTime = (long) config.timeLimitSecs * 20;
        this.votingTime = (long) config.votingTimeSecs * 20;
    }

    public IdleTickResult tick(long time, GameSpace space) {
        // Game has finished. Wait a few seconds before finally closing the game.
        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                return IdleTickResult.GAME_CLOSED;
            }
            return IdleTickResult.GAME_WAIT_FOR_CLOSING;
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
                this.isAfterWaiting = false;
                if (!this.isVoting) {
                    this.isVoting = true;
                    this.finishTime = time + this.votingTime;
                    this.finishDiffTime = this.votingTime;
                    return IdleTickResult.BUILD_FINISHED_TICK;
                } else {
                    this.finishTime = time + this.votingTime;
                    this.finishDiffTime = this.votingTime;
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
        GAME_WAIT_FOR_CLOSING,
        GAME_CLOSED,
    }
}
