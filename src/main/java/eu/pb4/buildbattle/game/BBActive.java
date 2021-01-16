package eu.pb4.buildbattle.game;

import eu.pb4.buildbattle.game.map.BuildArena;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.util.PlayerRef;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import eu.pb4.buildbattle.game.map.BBMap;

import java.util.*;
import java.util.stream.Collectors;

public class BBActive {
    private final BBConfig config;

    public final GameSpace gameSpace;
    public final BBMap gameMap;

    public final Object2ObjectMap<PlayerRef, BBPlayer> participants;
    private final BBSpawnLogic spawnLogic;
    private final BBStageManager stageManager;
    private final boolean ignoreWinState;
    private final BBTimerBar timerBar;

    private BBActive(GameSpace gameSpace, BBMap map, GlobalWidgets widgets, BBConfig config, Set<PlayerRef> participants) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.spawnLogic = new BBSpawnLogic(gameSpace, map, this);
        this.participants = new Object2ObjectOpenHashMap<>();

        Iterator<BuildArena> arenaIterator = map.buildArenas.iterator();
        BuildArena arena = arenaIterator.next();

        for (PlayerRef player : participants) {
            if (arena.players.size() >= this.config.teamSize) {
                arena = arenaIterator.next();
            }
            this.participants.put(player, new BBPlayer(arena));
        }

        this.stageManager = new BBStageManager();
        this.ignoreWinState = this.participants.size() <= 1;
        this.timerBar = new BBTimerBar(widgets);
    }

    public static void open(GameSpace gameSpace, BBMap map, BBConfig config) {
        gameSpace.openGame(game -> {
            Set<PlayerRef> participants = gameSpace.getPlayers().stream()
                    .map(PlayerRef::of)
                    .collect(Collectors.toSet());
            GlobalWidgets widgets = new GlobalWidgets(game);
            BBActive active = new BBActive(gameSpace, map, widgets, config, participants);

            game.setRule(GameRule.CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.PORTALS, RuleResult.DENY);
            game.setRule(GameRule.PVP, RuleResult.DENY);
            game.setRule(GameRule.HUNGER, RuleResult.DENY);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
            game.setRule(GameRule.INTERACTION, RuleResult.ALLOW);
            game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
            game.setRule(GameRule.UNSTABLE_TNT, RuleResult.DENY);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);
            game.on(PlaceBlockListener.EVENT, active::onPlaceBlock);
            game.on(BreakBlockListener.EVENT, active::onBreakBlock);

            game.on(GameTickListener.EVENT, active::tick);

            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
        });
    }

    private ActionResult onBreakBlock(ServerPlayerEntity serverPlayerEntity, BlockPos blockPos) {
        if (this.stageManager.isVoting) {
            return ActionResult.FAIL;
        }

        BuildArena buildArena = this.gameMap.getBuildArea(blockPos);
        BBPlayer bbPlayer = this.participants.get(PlayerRef.of(serverPlayerEntity));

        if (buildArena != null && bbPlayer != null && buildArena.canBuild(blockPos, bbPlayer)) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private ActionResult onPlaceBlock(ServerPlayerEntity serverPlayerEntity, BlockPos blockPos, BlockState blockState, ItemUsageContext itemUsageContext) {
        if (this.stageManager.isVoting) {
            return ActionResult.FAIL;
        }

        BuildArena buildArena = this.gameMap.getBuildArea(blockPos);
        BBPlayer bbPlayer = this.participants.get(PlayerRef.of(serverPlayerEntity));

        if (buildArena != null && bbPlayer != null && buildArena.canBuild(blockPos, bbPlayer)) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private void onOpen() {
        ServerWorld world = this.gameSpace.getWorld();
        for (PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(world, this::spawnParticipant);
        }
        this.stageManager.onOpen(world.getTime(), this.config);
        // TODO setup logic
    }

    private void onClose() {
        // TODO teardown logic
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnSpectator(player);
        }
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.participants.remove(PlayerRef.of(player));
    }

    private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        // TODO handle damage
        this.spawnParticipant(player);
        return ActionResult.FAIL;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        // TODO handle death
        this.spawnParticipant(player);
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    private void tick() {
        ServerWorld world = this.gameSpace.getWorld();
        long time = world.getTime();

        BBStageManager.IdleTickResult result = this.stageManager.tick(time, gameSpace);

        switch (result) {
            case BUILD_TICK:
                break;
            case TICK_FINISHED:
                return;
            case VOTE_FINISHED_TICK:
                this.broadcastWin(this.checkWinResult());
                return;
            case GAME_CLOSED:
                this.gameSpace.close();
                return;
        }

        this.timerBar.update(this.stageManager.finishTime - time, this.config.timeLimitSecs * 20);
    }

    private void broadcastWin(WinResult result) {
        ServerPlayerEntity winningPlayer = result.getWinningPlayer();

        Text message;
        if (winningPlayer != null) {
            message = winningPlayer.getDisplayName().shallowCopy().append(" has won the game!").formatted(Formatting.GOLD);
        } else {
            message = new LiteralText("The game ended, but nobody won!").formatted(Formatting.GOLD);
        }

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    private WinResult checkWinResult() {
        // for testing purposes: don't end the game if we only ever had one participant
        if (this.ignoreWinState) {
            return WinResult.no();
        }

        ServerWorld world = this.gameSpace.getWorld();
        ServerPlayerEntity winningPlayer = null;

        // TODO win result logic
        return WinResult.no();
    }

    static class WinResult {
        final ServerPlayerEntity winningPlayer;
        final boolean win;

        private WinResult(ServerPlayerEntity winningPlayer, boolean win) {
            this.winningPlayer = winningPlayer;
            this.win = win;
        }

        static WinResult no() {
            return new WinResult(null, false);
        }

        static WinResult win(ServerPlayerEntity player) {
            return new WinResult(player, true);
        }

        public boolean isWin() {
            return this.win;
        }

        public ServerPlayerEntity getWinningPlayer() {
            return this.winningPlayer;
        }
    }
}
