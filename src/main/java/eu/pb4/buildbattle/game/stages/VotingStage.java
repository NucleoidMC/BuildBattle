package eu.pb4.buildbattle.game.stages;

import com.mojang.datafixers.util.Pair;
import eu.pb4.buildbattle.custom.BBItems;
import eu.pb4.buildbattle.custom.VotingItem;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.PlayerData;
import eu.pb4.buildbattle.game.TimerBar;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.game.map.GameplayMap;
import eu.pb4.buildbattle.other.FormattingUtil;
import eu.pb4.buildbattle.other.TextHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.FireworkItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerS2CPacketEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VotingStage {
    public final GameSpace gameSpace;
    public final GameplayMap gameMap;
    public final ServerWorld world;
    public final Object2ObjectMap<PlayerRef, PlayerData> participants;
    public final String theme;
    private final BuildBattleConfig config;
    private final TimerBar timerBar;
    private final Iterator<BuildArena> votingArenaIterator;
    public int currentTick = 0;
    public BuildArena votedArea = null;
    private int currentVotingDuration = -1;
    private int switchToNextArenaTime = -1;
    private int gameEndTime = -1;
    private Phase phase = Phase.VOTING;
    private boolean allowVoting = false;

    private VotingStage(GameSpace gameSpace, ServerWorld world, GameplayMap map, GlobalWidgets widgets, BuildBattleConfig config, String theme, Object2ObjectMap<PlayerRef, PlayerData> participants) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.participants = participants;
        this.world = world;
        this.theme = theme;

        this.timerBar = new TimerBar(widgets);
        this.timerBar.setColor(BossBar.Color.YELLOW);
        this.votingArenaIterator = map.buildArena.iterator();
    }

    public static void open(GameSpace gameSpace, GameplayMap map, ServerWorld world, String theme, Object2ObjectMap<PlayerRef, PlayerData> participants, BuildBattleConfig config) {
        gameSpace.setActivity(game -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(game);
            VotingStage active = new VotingStage(gameSpace, world, map, widgets, config, theme, participants);

            game.setRule(GameRuleType.CRAFTING, ActionResult.FAIL);
            game.setRule(GameRuleType.PORTALS, ActionResult.FAIL);
            game.setRule(GameRuleType.PVP, ActionResult.FAIL);
            game.setRule(GameRuleType.HUNGER, ActionResult.FAIL);
            game.setRule(GameRuleType.FALL_DAMAGE, ActionResult.FAIL);
            game.setRule(GameRuleType.INTERACTION, ActionResult.PASS);
            game.setRule(GameRuleType.BLOCK_DROPS, ActionResult.FAIL);
            game.setRule(GameRuleType.THROW_ITEMS, ActionResult.FAIL);
            game.setRule(GameRuleType.FALL_DAMAGE, ActionResult.FAIL);
            game.setRule(GameRuleType.BREAK_BLOCKS, ActionResult.FAIL);
            game.setRule(GameRuleType.PLACE_BLOCKS, ActionResult.FAIL);
            game.setRule(GameRuleType.MODIFY_INVENTORY, ActionResult.FAIL);

            game.listen(GameActivityEvents.ENABLE, active::onOpen);

            game.listen(GamePlayerEvents.OFFER, offer -> offer.accept(world, Vec3d.ZERO));
            game.listen(GamePlayerEvents.ADD, active::addPlayer);
            game.listen(GamePlayerEvents.REMOVE, active::removePlayer);
            game.listen(ItemUseEvent.EVENT, active::onItemUse);
            game.listen(GameActivityEvents.TICK, active::tick);
            game.listen(ExplosionDetonatedEvent.EVENT, (e, b) -> e.clearAffectedBlocks());
            game.listen(EntitySpawnEvent.EVENT, (x) -> x instanceof MobEntity ? ActionResult.FAIL : ActionResult.PASS);
            game.listen(PlayerS2CPacketEvent.EVENT, active::onServerPacket);

            game.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
        });
    }

    private ActionResult onServerPacket(ServerPlayerEntity playerEntity, Packet<?> packet) {
        if (packet instanceof EntityEquipmentUpdateS2CPacket equipmentUpdate) {
            equipmentUpdate.getEquipmentList().removeIf((p) -> p.getFirst().getType() == EquipmentSlot.Type.HAND);
            equipmentUpdate.getEquipmentList().add(new Pair<>(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
        }

        return ActionResult.PASS;
    }

    private TypedActionResult<ItemStack> onItemUse(ServerPlayerEntity player, Hand hand) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));
        if (playerData != null && this.allowVoting) {
            if (this.votedArea.players.contains(playerData)) {
                player.sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, new TranslatableText("text.buildbattle.vote_own").formatted(Formatting.RED)), false);

                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            ItemStack itemStack = player.getStackInHand(hand);
            if (itemStack.getItem() instanceof VotingItem) {
                playerData.currentVote = ((VotingItem) itemStack.getItem()).score;
                player.sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, new TranslatableText("text.buildbattle.vote", itemStack.getName()).formatted(Formatting.WHITE)), false);
            }
            return TypedActionResult.success(player.getStackInHand(hand), true);
        }
        return TypedActionResult.pass(player.getStackInHand(hand));
    }


    private void onOpen() {
        this.nextArena();

    }

    private void addPlayer(ServerPlayerEntity player) {
        if (this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnParticipant(player);
        } else {
            this.spawnSpectator(player);
        }    }

    private void removePlayer(ServerPlayerEntity player) {

    }

    private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        return ActionResult.FAIL;
    }


    private void spawnParticipant(ServerPlayerEntity player) {
        player.getInventory().clear();
        player.changeGameMode(GameMode.ADVENTURE);

        player.getInventory().insertStack(BBItems.WT.getDefaultStack());
        player.getInventory().insertStack(BBItems.BAD.getDefaultStack());
        player.getInventory().insertStack(BBItems.NB.getDefaultStack());
        player.getInventory().insertStack(BBItems.OKAY.getDefaultStack());
        player.getInventory().insertStack(BBItems.GOOD.getDefaultStack());
        player.getInventory().insertStack(BBItems.GREAT.getDefaultStack());
        player.getInventory().insertStack(BBItems.WOW.getDefaultStack());

        player.getAbilities().allowFlying = true;
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));

        if (this.votedArea != null) {
            this.votedArea.teleportPlayer(player, this.world);
        }
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SPECTATOR);
        if (this.votedArea != null) {
            this.votedArea.teleportPlayer(player, this.world);
        }
    }

    private void tick() {
        int time = this.currentTick;
        this.currentTick++;

        switch (this.phase) {
            case VOTING -> {
                if (time >= this.currentVotingDuration) {
                    this.phase = Phase.WAITING;
                    this.allowVoting = false;
                    this.countScore();
                    this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.PICKAXE_PREFIX,
                            new TranslatableText("text.buildbattle.build_by",
                                    this.votedArea.getBuildersText(this.gameSpace)
                            ).formatted(Formatting.AQUA)));
                    this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.STAR_PREFIX,
                            new TranslatableText("text.buildbattle.build_score",
                                    new LiteralText("" + this.votedArea.score).formatted(Formatting.GOLD)
                            ).formatted(Formatting.LIGHT_PURPLE)));

                    this.timerBar.update(new TranslatableText(this.votingArenaIterator.hasNext() ? "text.buildbattle.timer_bar.next_arena" : "text.buildbattle.timer_bar.finishing_game"), 0);

                    for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                        player.getInventory().clear();
                    }
                    break;
                }

                int ticksLeft = this.currentVotingDuration - time;

                int secondsUntilEnd = ticksLeft / 20 + 1;

                int minutes = secondsUntilEnd / 60;
                int seconds = secondsUntilEnd % 60;

                this.timerBar.update(new TranslatableText("text.buildbattle.timer_bar.time_left", String.format("%02d:%02d", minutes, seconds))
                                .append(new LiteralText(" - ").formatted(Formatting.GRAY))
                                .append(new TranslatableText("text.buildbattle.timer_bar.theme").formatted(Formatting.YELLOW))
                                .append(new LiteralText(theme)),

                        ((float) ticksLeft) / (this.config.votingTimeSecs() * 20));
            }
            case WAITING -> {
                if (time >= this.switchToNextArenaTime) {
                    if (this.nextArena()) {
                        this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX,
                                new TranslatableText("text.buildbattle.next_arena").formatted(Formatting.BLUE)));

                        this.gameSpace.getPlayers().playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 10f, 1);
                        this.allowVoting = true;
                        this.phase = Phase.VOTING;
                    } else {
                        this.phase = Phase.GAME_ENDS;
                        this.gameEndTime = this.currentTick + 200;
                        this.finishGame();

                        this.timerBar.update(new TranslatableText("text.buildbattle.timer_bar.game_ended", this.votedArea.getBuildersText(this.gameSpace)), 1);
                        this.timerBar.setColor(BossBar.Color.YELLOW);
                    }
                }
            }
            case GAME_ENDS -> {
                if (time >= this.gameEndTime) {
                    this.gameSpace.close(GameCloseReason.FINISHED);
                    return;
                }

                if (time % 20 == 0) {
                    for (PlayerData playerData : this.votedArea.players) {
                        ItemStack itemStack = ItemStackBuilder.firework(DyeColor.values()[(int) (Math.random() * DyeColor.values().length - 1)].getFireworkColor(), 1, FireworkItem.Type.LARGE_BALL).build();
                        ServerPlayerEntity player = playerData.playerRef.getEntity(world);
                        if (player != null) {
                            FireworkRocketEntity entity = new FireworkRocketEntity(world, player.getX(), player.getY() + 2, player.getZ(), itemStack);
                            entity.noClip = true;
                            entity.addVelocity(0, 0.2, 0);
                            world.spawnEntity(entity);
                        }
                    }
                }
            }
        }
    }

    private void countScore() {
        if (this.votedArea != null) {
            for (PlayerData playerData : this.participants.values()) {
                this.votedArea.score += playerData.getAndClearCurrentVote();
            }
        }
    }

    private boolean nextArena() {
        while (this.votingArenaIterator.hasNext()) {
            BuildArena arena = this.votingArenaIterator.next();
            if (arena.getPlayerCount() > 0) {
                this.votedArea = arena;
                this.allowVoting = true;
                this.currentVotingDuration = this.currentTick + this.config.votingTimeSecs() * 20;
                this.switchToNextArenaTime = this.currentVotingDuration + 100;
                for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                    if (this.participants.containsKey(PlayerRef.of(player))) {
                        this.spawnParticipant(player);
                    } else {
                        this.spawnSpectator(player);
                    }
                }
                return true;
            }
        }
        this.allowVoting = false;
        return false;
    }

    private void finishGame() {
        List<BuildArena> buildArenaList = this.gameMap.buildArena.stream()
                .sorted(Comparator.comparingDouble(p -> -p.score))
                .filter(arena -> arena.getPlayerCount() != 0)
                .collect(Collectors.toList());

        Text message = FormattingUtil.format(FormattingUtil.FLAG_PREFIX, new TranslatableText("text.buildbattle.game_ended").formatted(Formatting.GOLD));
        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);

        for (int x = 0; x < 5; x++) {
            if (buildArenaList.size() <= x) {
                break;
            }

            BuildArena arena = buildArenaList.get(x);

            players.sendMessage(new TranslatableText("text.buildbattle.win_place",
                    x + 1,
                    arena.getBuildersText(this.gameSpace),
                    new LiteralText("" + arena.score).formatted(Formatting.WHITE)
            ).formatted(Formatting.YELLOW));
        }

        for (BuildArena arena : buildArenaList) {
            int arenaPlace = buildArenaList.indexOf(arena) + 1;
            Text yourScore = FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.WIN_STYLE, new TranslatableText("text.buildbattle.your_score",
                    new LiteralText("" + (arenaPlace)).append(TextHelper.getOrdinal(arenaPlace)).formatted(Formatting.WHITE),
                    new LiteralText("" + arena.score).formatted(Formatting.WHITE)));

            for (UUID uuid : arena.playersUuid) {
                ServerPlayerEntity player = this.gameSpace.getPlayers().getEntity(uuid);

                if (player != null) {
                    player.sendMessage(yourScore, false);
                }

            }
        }

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            this.votedArea = buildArenaList.get(0);
            this.votedArea.teleportPlayer(player, this.world);
        }
        players.playSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    enum Phase {
        VOTING,
        WAITING,
        GAME_ENDS
    }
}
