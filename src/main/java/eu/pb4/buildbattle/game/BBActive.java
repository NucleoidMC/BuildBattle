package eu.pb4.buildbattle.game;

import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.Helper;
import eu.pb4.buildbattle.custom.BBItems;
import eu.pb4.buildbattle.custom.FloorChangingVillager;
import eu.pb4.buildbattle.custom.VotingItem;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.themes.Theme;
import eu.pb4.buildbattle.themes.ThemesRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.GameCloseReason;
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
    public final BBStageManager stageManager;
    private final BBTimerBar timerBar;

    public final String theme;

    public BuildArena votedArea = null;
    private Iterator<BuildArena> votingArenaIterator = null;

    private BBActive(GameSpace gameSpace, BBMap map, GlobalWidgets widgets, BBConfig config, Set<PlayerRef> participants) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.spawnLogic = new BBSpawnLogic(gameSpace, map, this);
        this.participants = new Object2ObjectOpenHashMap<>();

        for (BuildArena arena : this.gameMap.buildArenas) {
            arena.game = this;
        }

        Iterator<BuildArena> arenaIterator = map.buildArenas.iterator();
        BuildArena arena = arenaIterator.next();

        for (PlayerRef player : participants) {
            if (arena.players.size() >= this.config.teamSize) {
                arena = arenaIterator.next();
            }
            this.participants.put(player, new BBPlayer(arena, player));
        }

        this.stageManager = new BBStageManager();
        this.timerBar = new BBTimerBar(widgets);


        Theme theme = ThemesRegistry.get(config.theme);

        if (theme != null) {
            this.theme = theme.getRandom();
        } else {
            this.theme = "Undefined";
        }

        gameSpace.getPlayers().sendMessage(new LiteralText("» ").formatted(Formatting.GRAY)
                .append(new TranslatableText("buildbattle.text.theme",
                        new LiteralText(this.theme).formatted(Formatting.GOLD)
                ).formatted(Formatting.WHITE)));
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

            game.setRule(BuildBattle.CREATIVE_LIMIT, RuleResult.ALLOW);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);
            game.on(PlaceBlockListener.EVENT, active::onPlaceBlock);
            game.on(BreakBlockListener.EVENT, active::onBreakBlock);
            game.on(UseItemListener.EVENT, active::onItemUse);
            game.on(AttackEntityListener.EVENT, active::onEntityDamage);
            game.on(GameTickListener.EVENT, active::tick);
            game.on(ExplosionListener.EVENT, active::onExplosion);

            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
        });
    }

    private ActionResult onEntityDamage(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (entity instanceof FloorChangingVillager) {
            return ActionResult.FAIL;
        }

        BuildArena arena = this.gameMap.getBuildArea(entity.getBlockPos());
        BBPlayer bbPlayer = this.participants.get(PlayerRef.of(player));

        if (arena != null && arena.players.contains(bbPlayer)) {
            return ActionResult.PASS;
        }

        return ActionResult.FAIL;
    }

    private void onExplosion(List<BlockPos> blockPosList) {
        blockPosList.clear();
    }

    private TypedActionResult<ItemStack> onItemUse(ServerPlayerEntity player, Hand hand) {
        BBPlayer bbPlayer = this.participants.get(PlayerRef.of(player));
        if (bbPlayer != null && this.stageManager.isVoting && this.stageManager.waitTime <= -1) {
            if (this.votedArea.players.contains(bbPlayer)) {
                player.sendMessage(new LiteralText("» ").formatted(Formatting.GRAY)
                        .append(new TranslatableText("buildbattle.text.voteown").formatted(Formatting.RED)), false);

                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            ItemStack itemStack = player.getStackInHand(hand);
            if (itemStack.getItem() instanceof VotingItem) {
                bbPlayer.currentVote = ((VotingItem) itemStack.getItem()).score;
                player.sendMessage(new LiteralText("» ").formatted(Formatting.GRAY)
                        .append(new TranslatableText("buildbattle.text.vote", itemStack.getName()).formatted(Formatting.WHITE)), false);
            }
            return TypedActionResult.success(player.getStackInHand(hand), true);
        }
        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    private ActionResult onBreakBlock(ServerPlayerEntity serverPlayerEntity, BlockPos blockPos) {
        if (this.stageManager.isVoting) {
            return ActionResult.FAIL;
        }

        BuildArena buildArena = this.gameMap.getBuildArea(blockPos);
        BBPlayer bbPlayer = this.participants.get(PlayerRef.of(serverPlayerEntity));

        if (buildArena != null && bbPlayer != null && this.stageManager.waitTime <= -1 && buildArena.canBuild(blockPos, bbPlayer)) {
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

        if (buildArena != null && bbPlayer != null && this.stageManager.waitTime <= -1 && buildArena.canBuild(blockPos, bbPlayer)) {
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
    }

    private void onClose() {

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
        return ActionResult.FAIL;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.spawnParticipant(player);
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, this.stageManager.isVoting ? GameMode.ADVENTURE : GameMode.CREATIVE);
        if (this.stageManager.isVoting) {
            player.inventory.insertStack(BBItems.WT.getItemStack());
            player.inventory.insertStack(BBItems.BAD.getItemStack());
            player.inventory.insertStack(BBItems.NB.getItemStack());
            player.inventory.insertStack(BBItems.OKAY.getItemStack());
            player.inventory.insertStack(BBItems.GOOD.getItemStack());
            player.inventory.insertStack(BBItems.GREAT.getItemStack());
            player.inventory.insertStack(BBItems.WOW.getItemStack());
        }

        this.spawnLogic.spawnPlayer(player);
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    private void tick() {
        ServerWorld world = this.gameSpace.getWorld();
        long time = world.getTime();

        BBStageManager.IdleTickResult result = this.stageManager.tick(time, this.gameSpace);

        switch (result) {
            case BUILD_WAIT:
                this.gameSpace.getPlayers().sendMessage(new LiteralText("» ").formatted(Formatting.GRAY)
                        .append(new TranslatableText("buildbattle.text.buildend").formatted(Formatting.GREEN)));
                return;
            case BUILD_FINISHED_TICK:
                this.votingArenaIterator = this.gameMap.buildArenas.iterator();
                this.votingNextArena();

                this.gameSpace.getPlayers().sendSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 10f, 1);
                return;
            case TICK_FINISHED:
                return;
            case VOTE_NEXT:
                if (this.votingNextArena()) {
                    this.gameSpace.getPlayers().sendMessage(new LiteralText("» ").formatted(Formatting.GRAY)
                            .append(new TranslatableText("buildbattle.text.nextarea").formatted(Formatting.BLUE)));

                    this.gameSpace.getPlayers().sendSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 10f, 1);
                }
                return;
            case VOTE_WAIT:
                this.countScore();
                this.gameSpace.getPlayers().sendMessage(new LiteralText("» ").formatted(Formatting.GRAY)
                        .append(new TranslatableText("buildbattle.text.buildby",
                                this.votedArea.getBuilders(this)
                        ).formatted(Formatting.AQUA)));
                this.gameSpace.getPlayers().sendMessage(new LiteralText("» ").formatted(Formatting.GRAY)
                        .append(new TranslatableText("buildbattle.text.buildscore",
                                new LiteralText("" + this.votedArea.score).formatted(Formatting.GOLD)
                        ).formatted(Formatting.LIGHT_PURPLE)));
                return;
            case GAME_FINISHED:
                this.broadcastWin();
                return;
            case GAME_CLOSED:
                this.gameSpace.close(GameCloseReason.FINISHED);
                return;
        }

        this.timerBar.update(this.stageManager.finishTime - time, this.stageManager.finishDiffTime, this.theme);
    }

    private void countScore() {
        if (this.votedArea != null) {
            for (BBPlayer bbPlayer : this.participants.values()) {
                this.votedArea.score += bbPlayer.getAndClearCurrentVote();
            }
        }
    }

    private boolean votingNextArena() {
        while (this.votingArenaIterator.hasNext()) {
            BuildArena arena = this.votingArenaIterator.next();
            if (arena.players.size() > 0) {
                this.votedArea = arena;
                for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                    if (this.participants.get(PlayerRef.of(player)) != null) {
                        this.spawnParticipant(player);
                    }
                    else {
                        this.spawnSpectator(player);
                    }
                }
                return true;
            }
        }

        this.stageManager.isFinished = true;
        return false;
    }

    private void broadcastWin() {
        List<BuildArena> buildArenaList = this.gameMap.buildArenas.stream()
                .sorted(Comparator.comparingDouble(p -> -p.score))
                .collect(Collectors.toList());

        Text message = new LiteralText("» ").formatted(Formatting.GRAY).append(
                new TranslatableText("buildbattle.text.gameend").formatted(Formatting.GOLD)
        );
        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);

        for (int x = 0; x < 5; x++) {
            BuildArena arena = buildArenaList.get(x);
            players.sendMessage(new TranslatableText("buildbattle.text.winplace",
                    x + 1,
                    arena.getBuilders(this),
                    new LiteralText("" + arena.score).formatted(Formatting.WHITE)
            ).formatted(Formatting.YELLOW));
        }

        for (BuildArena arena : buildArenaList) {
            int place = buildArenaList.indexOf(arena) + 1;
            Text mes = new LiteralText("» ").formatted(Formatting.GRAY).append(
                    new TranslatableText("buildbattle.text.yourscore",
                            new LiteralText("" + (place)).append(Helper.getOrdinal(place)).formatted(Formatting.WHITE),
                            new LiteralText("" + arena.score).formatted(Formatting.WHITE)
                    ).formatted(Formatting.GOLD)
            );
            for (BBPlayer bbPlayer : arena.players) {
                ServerPlayerEntity player = bbPlayer.playerRef.getEntity(this.gameSpace.getWorld());
                if (player != null) {
                    player.sendMessage(mes, false);
                }

            }
        }

        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }
}
