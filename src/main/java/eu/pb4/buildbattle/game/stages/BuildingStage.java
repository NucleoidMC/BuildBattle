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
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.explosion.Explosion;
import org.joml.Vector3f;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
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
    public final ServerWorld world;
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

    private BuildingStage(GameSpace gameSpace, ServerWorld world, GameplayMap map, GlobalWidgets widgets, BuildBattleConfig config, Set<PlayerRef> participants) {
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
        this.timerBar.setColor(BossBar.Color.GREEN);

        if (config.forcedTheme().isPresent()) {
            this.theme = config.forcedTheme().get();
            this.themeVotingTime = 0;
            this.switchToBuilding();
        } else if (config.themeVoting()) {
            this.themeVotingManager = new ThemeVotingManager(theme);
            this.themeVotingTime = 200;
            this.timerBar.setColor(BossBar.Color.BLUE);
            this.timerBar.update(Text.translatable("text.buildbattle.timer_bar.voting_theme"), 1);
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

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asGenerator())
                .setGameRule(GameRules.DO_WEATHER_CYCLE, false);

        ServerWorld world = gameSpace.getWorlds().add(worldConfig);

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
            game.listen(GamePlayerEvents.ACCEPT, offer -> offer.teleport(world, Vec3d.ZERO));
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

    private EventResult onFluidFlow(ServerWorld world, BlockPos blockPos, BlockState state, Direction direction, BlockPos blockPos1, BlockState state1) {
        var arena = this.gameMap.getArena(blockPos1);

        if (arena != null && arena.buildingArea.contains(blockPos1)) {
            return EventResult.PASS;
        }

        return EventResult.DENY;
    }

    private EventResult onEntitySpawn(Entity entity) {
        if (entity instanceof ServerPlayerEntity) {
            return EventResult.PASS;
        } else if (BbUtils.equalsOrInstance(entity, ItemEntity.class)) {
            return EventResult.DENY;
        } else {
            var arena = this.gameMap.getArena(entity.getBlockPos());

            if (arena != null) {
                if (entity.getEntityWorld().getOtherEntities(null, arena.bounds.asBox(), (e) -> !(e instanceof PlayerEntity)).size() > 32) {
                    return EventResult.DENY;
                }
            }
        }

        return EventResult.PASS;
    }

    private EventResult onClientPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (packet instanceof BookUpdateC2SPacket) {
            return EventResult.DENY;
        }

        if (packet instanceof CreativeInventoryActionC2SPacket packet1) {
            ItemStack stack = packet1.stack();

            if (!Registries.ITEM.getId(stack.getItem()).getNamespace().equals(BuildBattle.ID)) {
                if (stack.isIn(BuildBattle.BANNED_ITEMS)) {
                    stack = ItemStack.EMPTY;
                } else {
                    if (stack.getItem() instanceof BlockItem || (stack.getItem() instanceof BucketItem && !(stack.getItem() instanceof EntityBucketItem))) {

                    } else {
                        stack = WrappedItem.createWrapped(stack);
                    }
                }
            }

            BbUtils.setCreativeStack(packet1, stack);
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, 0, packet1.slot(), stack));
        }

        return EventResult.PASS;
    }


    private EventResult onEntityDamage(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (entity instanceof FloorChangingEntity) {
            return EventResult.DENY;
        }

        BuildArena arena = this.gameMap.getArena(entity.getBlockPos());

        if (arena != null && arena.isBuilder(player)) {
            return EventResult.PASS;
        }

        return EventResult.DENY;
    }

    private EventResult onExplosion(Explosion explosion, List<BlockPos> blockPosList) {
        return EventResult.DENY;
    }

    private ActionResult onItemUse(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        if (BbUtils.equalsOrInstance(item, Items.CHORUS_FRUIT, Items.ENDER_PEARL, Items.ENDER_EYE)) {
            return ActionResult.FAIL;
        }

        if (item == BBRegistry.UTIL_OPENER) {
            UtilsUi.open(player, this.participants.get(PlayerRef.of(player)), this);
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.FAIL;
    }

    private ActionResult onBlockUse(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        BuildArena arena = this.gameMap.getArena(hitResult.getBlockPos());

        if (arena == null || !(arena.canBuild(hitResult.getBlockPos(), player) || arena.canBuild(hitResult.getBlockPos().offset(hitResult.getSide()), player))) {
            return ActionResult.FAIL;
        }

        ItemStack stack = player.getStackInHand(hand);
        var item = stack.getItem();
        if (BbUtils.equalsOrInstance(item, Items.ARMOR_STAND, EntityBucketItem.class, SpawnEggItem.class, BoatItem.class)) {
            return ActionResult.FAIL;
        }

        var data = this.participants.get(PlayerRef.of(player));
        if (hand == Hand.MAIN_HAND) {
            data.lastTryFill = System.currentTimeMillis();
            if (item == BBRegistry.FILL_WAND) {
                data.selectionStart = hitResult.getBlockPos();
                if (data.selectionEnd == null) {
                    data.selectionEnd = data.selectionStart;
                }
                return ActionResult.FAIL;
            } else if (data.isSelected()) {
                tryFill(player);
                return ActionResult.FAIL;
            }
        }

        return ActionResult.PASS;
    }

    private EventResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (this.lockBuilding) {
            return EventResult.DENY;
        }

        BuildArena buildArena = this.gameMap.getArena(pos);

        if (buildArena != null && buildArena.canBuild(pos, player)) {
            var data = this.participants.get(PlayerRef.of(player));
            data.lastTryFill = System.currentTimeMillis();
            if (player.getMainHandStack().getItem() == BBRegistry.FILL_WAND) {
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

    private void tryFill(ServerPlayerEntity player) {
        if (!this.config.enableTools()) {
            return;
        }

        var data = this.participants.get(PlayerRef.of(player));
        var stack = player.getMainHandStack();
        var item = player.getMainHandStack().getItem();

        if (data.isSelected()) {
            BlockState state = BbUtils.getStateFrom(player, stack);

            if (state != null) {
                for (var pos : BlockBounds.of(data.selectionStart, data.selectionEnd)) {
                    player.getWorld().setBlockState(pos, state);
                }
            }

            data.resetSelection();
        }
    }

    private void onPlayerSwing(ServerPlayerEntity player, Hand hand) {
        if (Thread.currentThread() != player.server.getThread()) {
            return;
        }

        if (hand == Hand.MAIN_HAND && player.getMainHandStack().getItem() == BBRegistry.FILL_WAND) {
            var data = this.participants.get(PlayerRef.of(player));
            if (System.currentTimeMillis() - data.lastTryFill > 500) {
                data.resetSelection();
            }
        }
    }

    private EventResult onPlaceBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, ItemUsageContext itemUsageContext) {
        if (this.lockBuilding) {
            return EventResult.DENY;
        }

        BuildArena buildArena = this.gameMap.getArena(pos);

        if (buildArena != null && buildArena.canBuild(pos, player)) {
            return EventResult.ALLOW;
        }

        return EventResult.DENY;
    }

    private ActionResult onFluidPlace(ServerPlayerEntity player, BlockPos blockPos) {
        if (this.lockBuilding) {
            return ActionResult.FAIL;
        }

        BuildArena buildArena = this.gameMap.getArena(blockPos);

        if (buildArena != null && buildArena.canBuild(blockPos, player)) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private void onOpen() {
        for (PlayerRef ref : this.participants.keySet()) {
            if (this.gameSpace.getPlayers().contains(ref)) {
                ref.ifOnline(world, (p) -> {
                    this.spawnParticipant(p);
                    if (this.themeVotingManager != null) {
                        this.themeVotingManager.addPlayer(p);
                    }
                });

            }
        }
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnSpectator(player);
        }
    }

    private void removePlayer(ServerPlayerEntity player) {

    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        if (this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnParticipant(player);
        } else {
            this.spawnSpectator(player);
        }
        return EventResult.DENY;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        player.getInventory().clear();
        player.changeGameMode(GameMode.CREATIVE);
        //player.getInventory().offerOrDrop(WrappedItem.createWrapped("test"));
        this.participants.get(PlayerRef.of(player)).arena.teleportPlayer(player, this.world);
    }

    private void spawnSpectator(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SPECTATOR);
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
                    this.gameSpace.getPlayers().playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.5f);
                }

                this.timerBar.update(Text.translatable("text.buildbattle.timer_bar.voting_theme"), ((float) (this.themeVotingTime - time)) / this.themeVotingTime);
            }
            case BUILDING -> {
                if (time >= this.buildingTimeDuration) {
                    this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.HOURGLASS_PREFIX, Text.translatable("text.buildbattle.build_time_ended").formatted(Formatting.GREEN)));
                    this.lockBuilding = true;
                    this.timerBar.setColor(BossBar.Color.RED);
                    this.timerBar.update(Text.translatable("text.buildbattle.timer_bar.times_up"), 0);
                    gameSpace.setAttachment(BuildBattle.ACTIVE_GAME, null);

                    for (BuildArena buildArena : this.gameMap.buildArena) {
                        buildArena.removeEntity(world);
                    }
                    this.phase = Phase.WAITING;
                    this.gameSpace.getPlayers().playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.5f);

                } else {
                    int ticksLeft = this.buildingTimeDuration - time;

                    int secondsUntilEnd = ticksLeft / 20 + 1;

                    int minutes = secondsUntilEnd / 60;
                    int seconds = secondsUntilEnd % 60;
                    if (minutes == 0 && seconds < 10 && time % 20 == 0) {
                        this.gameSpace.getPlayers().playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    }

                    this.timerBar.setColor(BossBar.Color.GREEN);
                    this.timerBar.update(Text.translatable("text.buildbattle.timer_bar.time_left", String.format("%02d:%02d", minutes, seconds))
                            .append(Text.literal(" - ").formatted(Formatting.GRAY))
                            .append(Text.translatable("text.buildbattle.timer_bar.theme").formatted(Formatting.YELLOW))
                            .append(Text.literal(theme)), ((float) ticksLeft) / (this.buildingTimeDuration - this.themeVotingTime));

                    if (time % 10 == 0) {
                        var borderEffect = new DustParticleEffect(ColorHelper.fromFloats(0, 0.8f, 0.8f, 0.8f), 2.0F);
                        var selectionEffect = new DustParticleEffect(ColorHelper.fromFloats(0, 0.8f, 0.3f, 0.3f), 1.8F);
                        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                            var data = this.participants.get(PlayerRef.of(player));
                            if (data != null) {
                                ParticleOutlineRenderer.render(player, data.arena.buildingArea.min(), data.arena.buildingArea.max().add(1, 1, 1), borderEffect);

                                if (data.isSelected()) {
                                    ParticleOutlineRenderer.render(player, BlockBounds.min(data.selectionStart, data.selectionEnd), BlockBounds.max(data.selectionStart, data.selectionEnd).add(1, 1, 1), selectionEffect);
                                }
                            }
                        }

                        ParticleEffect effect2 = new DustParticleEffect(ColorHelper.fromFloats(0, 0f, 1f, 0f), 2.0F);
                        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                            PlayerData data = this.participants.get(PlayerRef.of(player));
                            if (data != null) {
                                ParticleOutlineRenderer.render(player, data.arena.bounds.min(), data.arena.bounds.max().add(1, 1, 1), effect2);
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
        gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, Text.translatable("text.buildbattle.theme",
                Text.literal(this.theme).formatted(Formatting.GOLD)
        ).formatted(Formatting.WHITE)));

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
