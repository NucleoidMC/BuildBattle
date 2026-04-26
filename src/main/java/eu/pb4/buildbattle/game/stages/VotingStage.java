package eu.pb4.buildbattle.game.stages;

import com.mojang.datafixers.util.Pair;
import eu.pb4.buildbattle.custom.BBRegistry;
import eu.pb4.buildbattle.custom.items.VotingItem;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.PlayerData;
import eu.pb4.buildbattle.game.TimerBar;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.game.map.GameplayMap;
import eu.pb4.buildbattle.other.FormattingUtil;
import eu.pb4.buildbattle.other.TextHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerS2CPacketEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

import java.util.*;
import java.util.stream.Collectors;

public class VotingStage {
    public final GameSpace gameSpace;
    public final GameplayMap gameMap;
    public final ServerLevel world;
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

    private VotingStage(GameSpace gameSpace, ServerLevel world, GameplayMap map, GlobalWidgets widgets, BuildBattleConfig config, String theme, Object2ObjectMap<PlayerRef, PlayerData> participants) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.participants = participants;
        this.world = world;
        this.theme = theme;

        this.timerBar = new TimerBar(widgets);
        this.timerBar.setColor(BossEvent.BossBarColor.YELLOW);
        this.votingArenaIterator = map.buildArena.iterator();
    }

    public static void open(GameSpace gameSpace, GameplayMap map, ServerLevel world, String theme, Object2ObjectMap<PlayerRef, PlayerData> participants, BuildBattleConfig config) {
        gameSpace.setActivity(game -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(game);
            VotingStage active = new VotingStage(gameSpace, world, map, widgets, config, theme, participants);

            game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
            game.setRule(GameRuleType.PORTALS, EventResult.DENY);
            game.setRule(GameRuleType.PVP, EventResult.DENY);
            game.setRule(GameRuleType.HUNGER, EventResult.DENY);
            game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
            game.setRule(GameRuleType.INTERACTION, EventResult.PASS);
            game.setRule(GameRuleType.BLOCK_DROPS, EventResult.DENY);
            game.setRule(GameRuleType.THROW_ITEMS, EventResult.DENY);
            game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
            game.setRule(GameRuleType.BREAK_BLOCKS, EventResult.DENY);
            game.setRule(GameRuleType.PLACE_BLOCKS, EventResult.DENY);
            game.setRule(GameRuleType.MODIFY_INVENTORY, EventResult.DENY);

            game.listen(GameActivityEvents.ENABLE, active::onOpen);

            game.listen(GamePlayerEvents.OFFER, offer -> offer.intent() == JoinIntent.SPECTATE ? offer.accept() : offer.pass());
            game.listen(GamePlayerEvents.ACCEPT, offer -> offer.teleport(world, active.votedArea != null ? active.votedArea.spawn.center() : map.buildArena.getFirst().spawn.center()));
            game.listen(GamePlayerEvents.ADD, active::addPlayer);
            game.listen(GamePlayerEvents.REMOVE, active::removePlayer);
            game.listen(ItemUseEvent.EVENT, active::onItemUse);
            game.listen(GameActivityEvents.TICK, active::tick);
            game.listen(ExplosionDetonatedEvent.EVENT, (e, b) -> EventResult.DENY);
            game.listen(EntitySpawnEvent.EVENT, (x) -> x instanceof Mob ? EventResult.DENY : EventResult.PASS);
            game.listen(PlayerS2CPacketEvent.EVENT, active::onServerPacket);

            game.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
        });
    }

    private volatile boolean skipPacket = false;

    protected EventResult onServerPacket(ServerPlayer player, Packet<?> packet) {
        if (skipPacket) {
            return EventResult.PASS;
        }

        var x = transformPacket(player, packet);

        if (x == null) {
            return EventResult.DENY;
        } else if (x == packet) {
            return EventResult.PASS;
        } else {
            skipPacket = true;
            player.connection.send(x);
            skipPacket = false;
            return EventResult.DENY;
        }
    }


    protected Packet<ClientGamePacketListener> transformPacket(ServerPlayer player, Packet<?> packet) {
        if (packet instanceof ClientboundBundlePacket bundleS2CPacket) {
            var list = new ArrayList<Packet<? super ClientGamePacketListener>>();

            boolean needChanging = false;

            for (var x : bundleS2CPacket.subPackets()) {
                var y = transformPacket(player, x);

                if (y != null) {
                    list.add(y);
                }

                if (x != y) {
                    needChanging = true;
                }
            }

            return needChanging ? new ClientboundBundlePacket(list) : bundleS2CPacket;
        } else if (packet instanceof ClientboundSetEquipmentPacket equipmentUpdate) {
            var list = new ArrayList<Pair<EquipmentSlot, ItemStack>>();

            for (var pair : equipmentUpdate.getSlots()) {
                list.add(new Pair<>(pair.getFirst(), ItemStack.EMPTY));
            }

            if (list.size() > 0) {
                return new ClientboundSetEquipmentPacket(equipmentUpdate.getEntity(), list);
            }
        }

        return (Packet<ClientGamePacketListener>) packet;
    }

    private InteractionResult onItemUse(ServerPlayer player, InteractionHand hand) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));
        if (playerData != null && this.allowVoting) {
            if (this.votedArea.players.contains(playerData)) {
                player.sendSystemMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, Component.translatable("text.buildbattle.vote_own").withStyle(ChatFormatting.RED)), false);

                return InteractionResult.FAIL;
            }

            ItemStack itemStack = player.getItemInHand(hand);
            if (itemStack.getItem() instanceof VotingItem) {
                playerData.currentVote = ((VotingItem) itemStack.getItem()).score;
                player.sendSystemMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, Component.translatable("text.buildbattle.vote", itemStack.getHoverName()).withStyle(ChatFormatting.WHITE)), false);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }


    private void onOpen() {
        this.nextArena();

    }

    private void addPlayer(ServerPlayer player) {
        if (this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnParticipant(player);
        } else {
            this.spawnSpectator(player);
        }
    }

    private void removePlayer(ServerPlayer player) {

    }

    private EventResult onPlayerDamage(ServerPlayer player, DamageSource source, float amount) {
        return EventResult.DENY;
    }


    private void spawnParticipant(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setGameMode(GameType.ADVENTURE);

        var inv = player.getInventory();

        inv.setItem(1, BBRegistry.VOTE_TERRIBLE.getDefaultInstance());
        inv.setItem(2, BBRegistry.VOTE_BAD.getDefaultInstance());
        inv.setItem(3, BBRegistry.VOTE_NOT_BAD.getDefaultInstance());
        inv.setItem(4, BBRegistry.VOTE_OKAY.getDefaultInstance());
        inv.setItem(5, BBRegistry.VOTE_GOOD.getDefaultInstance());
        inv.setItem(6, BBRegistry.VOTE_GREAT.getDefaultInstance());
        inv.setItem(7, BBRegistry.VOTE_WOW.getDefaultInstance());
        inv.setSelectedSlot(4);
        player.connection.send(new ClientboundSetHeldSlotPacket(inv.getSelectedSlot()));

        player.getAbilities().mayfly = true;
        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));

        if (this.votedArea != null) {
            this.votedArea.teleportPlayer(player, this.world);
        }
    }

    private void spawnSpectator(ServerPlayer player) {
        player.setGameMode(GameType.SPECTATOR);
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
                            Component.translatable("text.buildbattle.build_by",
                                    this.votedArea.getBuildersText(this.gameSpace)
                            ).withStyle(ChatFormatting.AQUA)));
                    this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.STAR_PREFIX,
                            Component.translatable("text.buildbattle.build_score",
                                    Component.literal("" + this.votedArea.score).withStyle(ChatFormatting.GOLD)
                            ).withStyle(ChatFormatting.LIGHT_PURPLE)));

                    this.timerBar.update(Component.translatable(this.votingArenaIterator.hasNext() ? "text.buildbattle.timer_bar.next_arena" : "text.buildbattle.timer_bar.finishing_game"), 0);

                    for (ServerPlayer player : this.gameSpace.getPlayers()) {
                        player.getInventory().clearContent();
                    }
                    break;
                }

                int ticksLeft = this.currentVotingDuration - time;

                int secondsUntilEnd = ticksLeft / 20 + 1;

                int minutes = secondsUntilEnd / 60;
                int seconds = secondsUntilEnd % 60;

                this.timerBar.update(Component.translatable("text.buildbattle.timer_bar.time_left", String.format("%02d:%02d", minutes, seconds))
                                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                                .append(Component.translatable("text.buildbattle.timer_bar.theme").withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(theme)),

                        ((float) ticksLeft) / (this.config.votingTimeSecs() * 20));
            }
            case WAITING -> {
                if (time >= this.switchToNextArenaTime) {
                    if (this.nextArena()) {
                        this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX,
                                Component.translatable("text.buildbattle.next_arena").withStyle(ChatFormatting.BLUE)));

                        this.gameSpace.getPlayers().playSound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 10f, 1);
                        this.allowVoting = true;
                        this.phase = Phase.VOTING;
                    } else {
                        this.phase = Phase.GAME_ENDS;
                        this.gameEndTime = this.currentTick + 200;
                        this.finishGame();

                        this.timerBar.update(Component.translatable("text.buildbattle.timer_bar.game_ended", this.votedArea.getBuildersText(this.gameSpace)), 1);
                        this.timerBar.setColor(BossEvent.BossBarColor.YELLOW);
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
                        ItemStack itemStack = ItemStackBuilder.firework(DyeColor.values()[(int) (Math.random() * DyeColor.values().length - 1)].getFireworkColor(), 1, FireworkExplosion.Shape.LARGE_BALL).build();
                        ServerPlayer player = playerData.playerRef.getEntity(world);
                        if (player != null) {
                            FireworkRocketEntity entity = new FireworkRocketEntity(world, player.getX(), player.getY() + 2, player.getZ(), itemStack);
                            entity.noPhysics = true;
                            entity.push(0, 0.2, 0);
                            world.addFreshEntity(entity);
                        }
                    }
                }
            }
        }
    }

    private void countScore() {
        if (this.votedArea != null) {
            for (var playerData : this.participants.values()) {
                if (this.votedArea != playerData.arena) {
                    this.votedArea.score += playerData.getAndClearCurrentVote();
                }
            }
        }
    }

    private boolean nextArena() {
        while (this.votingArenaIterator.hasNext()) {
            var arena = this.votingArenaIterator.next();
            if (arena.getPlayerCount() > 0) {
                this.votedArea = arena;
                this.allowVoting = true;
                this.currentVotingDuration = this.currentTick + this.config.votingTimeSecs() * 20;
                this.switchToNextArenaTime = this.currentVotingDuration + 100;
                for (ServerPlayer player : this.gameSpace.getPlayers()) {
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
        var buildArenaList = this.gameMap.buildArena.stream()
                .sorted(Comparator.comparingDouble(p -> -p.score))
                .filter(arena -> arena.getPlayerCount() != 0)
                .collect(Collectors.toList());

        var message = FormattingUtil.format(FormattingUtil.FLAG_PREFIX, Component.translatable("text.buildbattle.game_ended").withStyle(ChatFormatting.GOLD));
        var players = this.gameSpace.getPlayers();
        players.sendMessage(message);

        for (int x = 0; x < 5; x++) {
            if (buildArenaList.size() <= x) {
                break;
            }

            BuildArena arena = buildArenaList.get(x);

            players.sendMessage(Component.translatable("text.buildbattle.win_place",
                    x + 1,
                    arena.getBuildersText(this.gameSpace),
                    Component.literal("" + arena.score).withStyle(ChatFormatting.WHITE)
            ).withStyle(ChatFormatting.YELLOW));
        }

        for (BuildArena arena : buildArenaList) {
            int arenaPlace = buildArenaList.indexOf(arena) + 1;
            Component yourScore = FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, FormattingUtil.WIN_STYLE, Component.translatable("text.buildbattle.your_score",
                    Component.literal("" + (arenaPlace)).append(TextHelper.getOrdinal(arenaPlace)).withStyle(ChatFormatting.WHITE),
                    Component.literal("" + arena.score).withStyle(ChatFormatting.WHITE)));

            for (UUID uuid : arena.playersUuid) {
                ServerPlayer player = this.gameSpace.getPlayers().getEntity(uuid);

                if (player != null) {
                    player.sendSystemMessage(yourScore, false);
                }

            }
        }

        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            this.votedArea = buildArenaList.get(0);
            this.votedArea.teleportPlayer(player, this.world);
        }
        players.playSound(SoundEvents.VILLAGER_YES);
    }

    enum Phase {
        VOTING,
        WAITING,
        GAME_ENDS
    }
}
