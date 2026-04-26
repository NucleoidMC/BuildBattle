package eu.pb4.buildbattle.game.stages;

import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.custom.BBRegistry;
import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.custom.items.WrappedItem;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.PlayerData;
import eu.pb4.buildbattle.game.TimerBar;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.game.map.GameplayMap;
import eu.pb4.buildbattle.other.BbUtils;
import eu.pb4.buildbattle.other.FormattingUtil;
import eu.pb4.buildbattle.other.ParticleOutlineRenderer;
import eu.pb4.buildbattle.themes.Theme;
import eu.pb4.buildbattle.themes.ThemeVotingManager;
import eu.pb4.buildbattle.themes.ThemesRegistry;
import eu.pb4.buildbattle.ui.UtilsUi;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3f;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerSwingHandEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;
import xyz.nucleoid.stimuli.event.world.FluidFlowEvent;

import java.util.*;
import java.util.stream.Collectors;

public class BuildingStage {
    public final GameSpace gameSpace;
    public final GameplayMap gameMap;
    public final ServerLevel world;
    public final Object2ObjectMap<PlayerRef, PlayerData> participants;
    private final BuildBattleConfig config;
    private final TimerBar timerBar;

    private final int themeVotingTime;
    private final int buildingTimeDuration;
    private final int switchToVotingTime;
    public int currentTick = 0;
    public String theme;
    private ThemeVotingManager themeVotingManager;
    private boolean lockBuilding = true;
    private Phase phase = Phase.THEME_VOTING;

    private BuildingStage(GameSpace gameSpace, ServerLevel world, GameplayMap map, GlobalWidgets widgets, BuildBattleConfig config, Set<PlayerRef> participants) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.participants = new Object2ObjectOpenHashMap<>();
        this.world = world;

        Iterator<BuildArena> arenaIterator = map.buildArena.iterator();

        ArrayList<PlayerRef> randomPlayers = new ArrayList<>(participants);
        Collections.shuffle(randomPlayers);

        BuildArena arena = arenaIterator.next();

        for (PlayerRef player : participants) {
            if (arena.getPlayerCount() >= this.config.teamSize()) {
                arena = arenaIterator.next();
            }
            this.participants.put(player, new PlayerData(arena, player));
        }

        this.timerBar = new TimerBar(widgets);

        Theme theme = ThemesRegistry.get(config.theme());
        this.timerBar.setColor(BossEvent.BossBarColor.GREEN);

        if (config.forcedTheme().isPresent()) {
            this.theme = config.forcedTheme().get();
            this.themeVotingTime = 0;
            this.switchToBuilding();
        } else if (config.themeVoting()) {
            this.themeVotingManager = new ThemeVotingManager(theme);
            this.themeVotingTime = 200;
            this.timerBar.setColor(BossEvent.BossBarColor.BLUE);
            this.timerBar.update(Component.translatable("text.buildbattle.timer_bar.voting_theme"), 1);
        } else {
            assert theme != null;
            this.theme = theme.getRandom();
            this.themeVotingTime = 0;
            this.switchToBuilding();
        }


        this.buildingTimeDuration = this.themeVotingTime + config.timeLimitSecs() * 20;
        this.switchToVotingTime = this.buildingTimeDuration + 60;


        this.gameMap.buildArena.removeIf((a) -> a.getPlayerCount() == 0);

        for (BuildArena buildArena : this.gameMap.buildArena) {
            buildArena.spawnEntity(world, this.config.mapConfig().entityRotation());
        }
        gameSpace.setAttachment(BuildBattle.ACTIVE_GAME, this);
    }

    public static void open(GameSpace gameSpace, BuildBattleConfig config, Runnable runAfterTeleporting) {
        Set<PlayerRef> participants = gameSpace.getPlayers().stream().map(PlayerRef::of).collect(Collectors.toSet());

        GameplayMap map = new GameplayMap(gameSpace.getServer(), config, (int) Math.ceil(((double) gameSpace.getPlayers().size()) / config.teamSize()));

        RuntimeLevelConfig worldConfig = new RuntimeLevelConfig()
                .setGenerator(map.asGenerator())
                .setGameRule(GameRules.ADVANCE_WEATHER, false);

        ServerLevel world = gameSpace.getLevels().add(worldConfig);

        gameSpace.setActivity(game -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(game);
            BuildingStage active = new BuildingStage(gameSpace, world, map, widgets, config, participants);

            game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
            game.setRule(GameRuleType.PORTALS, EventResult.DENY);
            game.setRule(GameRuleType.PVP, EventResult.DENY);
            game.setRule(GameRuleType.HUNGER, EventResult.DENY);
            game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
            game.setRule(GameRuleType.INTERACTION, EventResult.PASS);
            game.setRule(GameRuleType.BLOCK_DROPS, EventResult.DENY);
            game.setRule(BuildBattle.CREATIVE_LIMIT, EventResult.DENY);

            game.listen(GameActivityEvents.ENABLE, () -> {
                active.onOpen();
                gameSpace.getServer().execute(runAfterTeleporting);
            });

            game.listen(GamePlayerEvents.OFFER, offer -> offer.intent() == JoinIntent.SPECTATE ? offer.accept() : offer.pass());
            game.listen(GamePlayerEvents.ACCEPT, offer -> offer.teleport(world, map.buildArena.getFirst().spawn.center()));
            game.listen(GamePlayerEvents.ADD, active::addPlayer);
            game.listen(GamePlayerEvents.REMOVE, active::removePlayer);
            game.listen(BlockPlaceEvent.BEFORE, active::onPlaceBlock);
            game.listen(BlockBreakEvent.EVENT, active::onBreakBlock);
            game.listen(ItemUseEvent.EVENT, active::onItemUse);
            game.listen(BlockUseEvent.EVENT, active::onBlockUse);
            game.listen(BuildBattle.ON_BUCKET_USAGE, active::onFluidPlace);

            game.listen(PlayerAttackEntityEvent.EVENT, active::onEntityDamage);
            game.listen(GameActivityEvents.TICK, active::tick);
            game.listen(ExplosionDetonatedEvent.EVENT, active::onExplosion);
            game.listen(FluidFlowEvent.EVENT, active::onFluidFlow);

            game.listen(PlayerC2SPacketEvent.EVENT, active::onClientPacket);

            game.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);
            game.listen(PlayerSwingHandEvent.EVENT, active::onPlayerSwing);

            game.listen(EntitySpawnEvent.EVENT, active::onEntitySpawn);
        });
    }

    private EventResult onFluidFlow(ServerLevel world, BlockPos blockPos, BlockState state, Direction direction, BlockPos blockPos1, BlockState state1) {
        var arena = this.gameMap.getArena(blockPos1);

        if (arena != null && arena.buildingArea.contains(blockPos1)) {
            return EventResult.PASS;
        }

        return EventResult.DENY;
    }

    private EventResult onEntitySpawn(Entity entity) {
        if (entity instanceof ServerPlayer) {
            return EventResult.PASS;
        } else if (BbUtils.equalsOrInstance(entity, ItemEntity.class)) {
            return EventResult.DENY;
        } else {
            var arena = this.gameMap.getArena(entity.blockPosition());

            if (arena != null) {
                if (entity.level().getEntities((Entity) null, arena.bounds.asBox(), (e) -> !(e instanceof Player)).size() > 32) {
                    return EventResult.DENY;
                }
            }
        }

        return EventResult.PASS;
    }

    private EventResult onClientPacket(ServerPlayer player, Packet<?> packet) {
        if (packet instanceof ServerboundEditBookPacket) {
            return EventResult.DENY;
        }

        if (packet instanceof ServerboundSetCreativeModeSlotPacket packet1) {
            ItemStack stack = packet1.itemStack();

            if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(BuildBattle.ID)) {
                if (stack.is(BuildBattle.BANNED_ITEMS)) {
                    stack = ItemStack.EMPTY;
                } else {
                    if (stack.getItem() instanceof BlockItem || (stack.getItem() instanceof BucketItem && !(stack.getItem() instanceof MobBucketItem))) {

                    } else {
                        stack = WrappedItem.createWrapped(stack);
                    }
                }
            }

            BbUtils.setCreativeStack(packet1, stack);
            player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, 0, packet1.slotNum(), stack));
        }

        return EventResult.PASS;
    }


    private EventResult onEntityDamage(ServerPlayer player, InteractionHand hand, Entity entity, EntityHitResult entityHitResult) {
        if (entity instanceof FloorChangingEntity) {
            return EventResult.DENY;
        }

        BuildArena arena = this.gameMap.getArena(entity.blockPosition());

        if (arena != null && arena.isBuilder(player)) {
            return EventResult.PASS;
        }

        return EventResult.DENY;
    }

    private EventResult onExplosion(Explosion explosion, List<BlockPos> blockPosList) {
        return EventResult.DENY;
    }

    private InteractionResult onItemUse(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        if (BbUtils.equalsOrInstance(item, Items.CHORUS_FRUIT, Items.ENDER_PEARL, Items.ENDER_EYE)) {
            return InteractionResult.FAIL;
        }

        if (item == BBRegistry.UTIL_OPENER) {
            UtilsUi.open(player, this.participants.get(PlayerRef.of(player)), this);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.FAIL;
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        BuildArena arena = this.gameMap.getArena(hitResult.getBlockPos());

        if (arena == null || !(arena.canBuild(hitResult.getBlockPos(), player) || arena.canBuild(hitResult.getBlockPos().relative(hitResult.getDirection()), player))) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(hand);
        var item = stack.getItem();
        if (BbUtils.equalsOrInstance(item, Items.ARMOR_STAND, MobBucketItem.class, SpawnEggItem.class, BoatItem.class)) {
            return InteractionResult.FAIL;
        }

        var data = this.participants.get(PlayerRef.of(player));
        if (hand == InteractionHand.MAIN_HAND) {
            data.lastTryFill = System.currentTimeMillis();
            if (item == BBRegistry.FILL_WAND) {
                data.selectionStart = hitResult.getBlockPos();
                if (data.selectionEnd == null) {
                    data.selectionEnd = data.selectionStart;
                }
                return InteractionResult.FAIL;
            } else if (data.isSelected()) {
                tryFill(player);
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }

    private EventResult onBreakBlock(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (this.lockBuilding) {
            return EventResult.DENY;
        }

        BuildArena buildArena = this.gameMap.getArena(pos);

        if (buildArena != null && buildArena.canBuild(pos, player)) {
            var data = this.participants.get(PlayerRef.of(player));
            data.lastTryFill = System.currentTimeMillis();
            if (player.getMainHandItem().getItem() == BBRegistry.FILL_WAND) {
                data.selectionEnd = pos;
                if (data.selectionStart == null) {
                    data.selectionStart = data.selectionEnd;
                }
                return EventResult.DENY;
            } else if (data.isSelected()) {
                tryFill(player);
                return EventResult.DENY;
            }
            return EventResult.ALLOW;
        }

        return EventResult.DENY;
    }

    private void tryFill(ServerPlayer player) {
        if (!this.config.enableTools()) {
            return;
        }

        var data = this.participants.get(PlayerRef.of(player));
        var stack = player.getMainHandItem();
        var item = player.getMainHandItem().getItem();

        if (data.isSelected()) {
            BlockState state = BbUtils.getStateFrom(player, stack);

            if (state != null) {
                for (var pos : BlockBounds.of(data.selectionStart, data.selectionEnd)) {
                    player.level().setBlockAndUpdate(pos, state);
                }
            }

            data.resetSelection();
        }
    }

    private void onPlayerSwing(ServerPlayer player, InteractionHand hand) {
        if (!player.level().getServer().isSameThread()) {
            return;
        }

        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().getItem() == BBRegistry.FILL_WAND) {
            var data = this.participants.get(PlayerRef.of(player));
            if (System.currentTimeMillis() - data.lastTryFill > 500) {
                data.resetSelection();
            }
        }
    }

    private EventResult onPlaceBlock(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state, UseOnContext itemUsageContext) {
        if (this.lockBuilding) {
            return EventResult.DENY;
        }

        BuildArena buildArena = this.gameMap.getArena(pos);

        if (buildArena != null && buildArena.canBuild(pos, player)) {
            return EventResult.ALLOW;
        }

        return EventResult.DENY;
    }

    private InteractionResult onFluidPlace(ServerPlayer player, BlockPos blockPos) {
        if (this.lockBuilding) {
            return InteractionResult.FAIL;
        }

        BuildArena buildArena = this.gameMap.getArena(blockPos);

        if (buildArena != null && buildArena.canBuild(blockPos, player)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private void onOpen() {
        for (PlayerRef ref : this.participants.keySet()) {
            if (this.gameSpace.getPlayers().contains(ref)) {
                ref.ifOnline(world, this::spawnParticipant);

            }
        }
    }

    private void addPlayer(ServerPlayer player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnSpectator(player);
        }
    }

    private void removePlayer(ServerPlayer player) {

    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        if (this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnParticipant(player);
        } else {
            this.spawnSpectator(player);
        }
        return EventResult.DENY;
    }

    private void spawnParticipant(ServerPlayer player) {
        player.getInventory().clearContent();
        player.setGameMode(GameType.CREATIVE);
        //player.getInventory().offerOrDrop(WrappedItem.createWrapped("test"));
        this.participants.get(PlayerRef.of(player)).arena.teleportPlayer(player, this.world);
    }

    private void spawnSpectator(ServerPlayer player) {
        player.setGameMode(GameType.SPECTATOR);
        this.gameMap.buildArena.get(0).teleportPlayer(player, this.world);
    }

    private void tick() {
        int time = this.currentTick;
        this.currentTick++;

        switch (this.phase) {
            case THEME_VOTING -> {
                if (time >= this.themeVotingTime) {
                    this.theme = this.themeVotingManager.getResultsAndClose();
                    this.themeVotingManager = null;

                    this.switchToBuilding();
                    this.gameSpace.getPlayers().playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                } else {
                    for (PlayerRef ref : this.participants.keySet()) {
                        if (this.gameSpace.getPlayers().contains(ref)) {
                            ref.ifOnline(world, (p) -> {
                                if (!p.touchingUnloadedChunk()) {
                                    this.themeVotingManager.addPlayer(p);
                                }
                            });
                        }
                    }
                }

                this.timerBar.update(Component.translatable("text.buildbattle.timer_bar.voting_theme"), ((float) (this.themeVotingTime - time)) / this.themeVotingTime);
            }
            case BUILDING -> {
                if (time >= this.buildingTimeDuration) {
                    this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.HOURGLASS_PREFIX, Component.translatable("text.buildbattle.build_time_ended").withStyle(ChatFormatting.GREEN)));
                    this.lockBuilding = true;
                    this.timerBar.setColor(BossEvent.BossBarColor.RED);
                    this.timerBar.update(Component.translatable("text.buildbattle.timer_bar.times_up"), 0);
                    gameSpace.setAttachment(BuildBattle.ACTIVE_GAME, null);

                    for (BuildArena buildArena : this.gameMap.buildArena) {
                        buildArena.removeEntity(world);
                    }
                    this.phase = Phase.WAITING;
                    this.gameSpace.getPlayers().playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);

                } else {
                    int ticksLeft = this.buildingTimeDuration - time;

                    int secondsUntilEnd = ticksLeft / 20 + 1;

                    int minutes = secondsUntilEnd / 60;
                    int seconds = secondsUntilEnd % 60;
                    if (minutes == 0 && seconds < 10 && time % 20 == 0) {
                        this.gameSpace.getPlayers().playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }

                    this.timerBar.setColor(BossEvent.BossBarColor.GREEN);
                    this.timerBar.update(Component.translatable("text.buildbattle.timer_bar.time_left", String.format("%02d:%02d", minutes, seconds))
                            .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                            .append(Component.translatable("text.buildbattle.timer_bar.theme").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(theme)), ((float) ticksLeft) / (this.buildingTimeDuration - this.themeVotingTime));

                    if (time % 10 == 0) {
                        var borderEffect = new DustParticleOptions(ARGB.colorFromFloat(0, 0.8f, 0.8f, 0.8f), 2.0F);
                        var selectionEffect = new DustParticleOptions(ARGB.colorFromFloat(0, 0.8f, 0.3f, 0.3f), 1.8F);
                        for (ServerPlayer player : this.gameSpace.getPlayers()) {
                            var data = this.participants.get(PlayerRef.of(player));
                            if (data != null) {
                                ParticleOutlineRenderer.render(player, data.arena.buildingArea.min(), data.arena.buildingArea.max().offset(1, 1, 1), borderEffect);

                                if (data.isSelected()) {
                                    ParticleOutlineRenderer.render(player, BlockBounds.min(data.selectionStart, data.selectionEnd), BlockBounds.max(data.selectionStart, data.selectionEnd).offset(1, 1, 1), selectionEffect);
                                }
                            }
                        }

                        ParticleOptions effect2 = new DustParticleOptions(ARGB.colorFromFloat(0, 0f, 1f, 0f), 2.0F);
                        for (ServerPlayer player : this.gameSpace.getPlayers()) {
                            PlayerData data = this.participants.get(PlayerRef.of(player));
                            if (data != null) {
                                ParticleOutlineRenderer.render(player, data.arena.bounds.min(), data.arena.bounds.max().offset(1, 1, 1), effect2);
                            }
                        }
                    }
                }
            }
            case WAITING -> {
                if (time >= this.switchToVotingTime) {
                    this.switchToVoting();
                }
            }
        }
    }


    private void switchToBuilding() {
        gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, Component.translatable("text.buildbattle.theme",
                Component.literal(this.theme).withStyle(ChatFormatting.GOLD)
        ).withStyle(ChatFormatting.WHITE)));

        this.phase = Phase.BUILDING;
        this.lockBuilding = false;
    }

    private void switchToVoting() {
        VotingStage.open(this.gameSpace, this.gameMap, this.world, this.theme, this.participants, this.config);
    }

    enum Phase {
        THEME_VOTING,
        BUILDING,
        WAITING
    }
}
