package eu.pb4.buildbattle.game.stages;

import eu.pb4.buildbattle.BuildBattle;
import eu.pb4.buildbattle.game.TimerBar;
import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.PlayerData;
import eu.pb4.buildbattle.game.map.GameplayMap;
import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.game.map.BuildArena;
import eu.pb4.buildbattle.other.FormattingUtil;
import eu.pb4.buildbattle.other.GeneralUtils;
import eu.pb4.buildbattle.other.ParticleOutlineRenderer;
import eu.pb4.buildbattle.themes.Theme;
import eu.pb4.buildbattle.themes.ThemeVotingManager;
import eu.pb4.buildbattle.themes.ThemesRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.explosion.Explosion;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.world.GameMode;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;
import xyz.nucleoid.stimuli.event.world.FluidFlowEvent;

import java.util.*;
import java.util.stream.Collectors;

public class BuildingStage {
    private final BuildBattleConfig config;

    public final GameSpace gameSpace;
    public final GameplayMap gameMap;
    public final ServerWorld world;

    public final Object2ObjectMap<PlayerRef, PlayerData> participants;
    private final TimerBar timerBar;

    private final int themeVotingTime;
    private final int buildingTimeDuration;
    private final int switchToVotingTime;
    private ThemeVotingManager themeVotingManager;
    private boolean lockBuilding = true;

    private Phase phase = Phase.THEME_VOTING;

    public int currentTick = 0;

    public String theme;

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
            this.timerBar.update(new TranslatableText("text.buildbattle.timer_bar.voting_theme"), 1);
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
    }

    public static void open(GameSpace gameSpace, BuildBattleConfig config, Runnable runAfterTeleporting) {
        Set<PlayerRef> participants = gameSpace.getPlayers().stream().map(PlayerRef::of).collect(Collectors.toSet());

        GameplayMap map = new GameplayMap(gameSpace.getServer(), config, (int) Math.ceil(((double) gameSpace.getPlayerCount()) / config.teamSize()));

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asGenerator())
                .setGameRule(GameRules.DO_WEATHER_CYCLE, false);

        ServerWorld world = gameSpace.addWorld(worldConfig);

        gameSpace.setActivity(game -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(game);
            BuildingStage active = new BuildingStage(gameSpace, world, map, widgets, config, participants);

            game.setRule(GameRuleType.CRAFTING, ActionResult.FAIL);
            game.setRule(GameRuleType.PORTALS, ActionResult.FAIL);
            game.setRule(GameRuleType.PVP, ActionResult.FAIL);
            game.setRule(GameRuleType.HUNGER, ActionResult.FAIL);
            game.setRule(GameRuleType.FALL_DAMAGE, ActionResult.FAIL);
            game.setRule(GameRuleType.INTERACTION, ActionResult.PASS);
            game.setRule(GameRuleType.BLOCK_DROPS, ActionResult.FAIL);
            game.setRule(BuildBattle.CREATIVE_LIMIT, ActionResult.FAIL);

            game.listen(GameActivityEvents.ENABLE, () -> {
                active.onOpen();
                runAfterTeleporting.run();
            });

            game.listen(GamePlayerEvents.OFFER, offer -> offer.accept(world, Vec3d.ZERO));
            game.listen(GamePlayerEvents.ADD, active::addPlayer);
            game.listen(GamePlayerEvents.REMOVE, active::removePlayer);
            game.listen(BlockPlaceEvent.BEFORE, active::onPlaceBlock);
            game.listen(BlockBreakEvent.EVENT, active::onBreakBlock);
            game.listen(ItemUseEvent.EVENT, active::onItemUse);
            game.listen(BlockUseEvent.EVENT, active::onBlockUse);
            game.listen(BuildBattle.ON_BUCKET_USAGE, active::onFluidPlace);

            game.listen(ItemUseEvent.EVENT, active::onItemUse);

            game.listen(PlayerAttackEntityEvent.EVENT, active::onEntityDamage);
            game.listen(GameActivityEvents.TICK, active::tick);
            game.listen(ExplosionDetonatedEvent.EVENT, active::onExplosion);
            game.listen(FluidFlowEvent.EVENT, active::onFluidFlow);

            game.listen(PlayerC2SPacketEvent.EVENT, active::onClientPacket);

            game.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);

            game.listen(EntitySpawnEvent.EVENT, active::onEntitySpawn);
        });
    }

    private ActionResult onFluidFlow(ServerWorld world, BlockPos blockPos, BlockState state, Direction direction, BlockPos blockPos1, BlockState state1) {
        var arena = this.gameMap.getArena(blockPos1);

        if (arena != null && arena.buildingArea.contains(blockPos1)) {
            return ActionResult.PASS;
        }

        return ActionResult.FAIL;
    }

    private ActionResult onEntitySpawn(Entity entity) {
        if (GeneralUtils.equalsOrInstance(entity, ItemEntity.class)) {
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private ActionResult onClientPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (packet instanceof BookUpdateC2SPacket) {
            return ActionResult.FAIL;
        }

        if (packet instanceof CreativeInventoryActionC2SPacket packet1) {
            ItemStack stack = packet1.getItemStack();
            Identifier identifier = Registry.ITEM.getId(stack.getItem());
            if (GeneralUtils.equalsOrInstance(stack.getItem(), Items.BARRIER, Items.LIGHT, Items.STRUCTURE_VOID, CommandBlockItem.class)
                    || !GeneralUtils.equalsOrInstance(identifier.getNamespace(), "minecraft", BuildBattle.ID)) {
                stack = ItemStack.EMPTY;
            } else if (stack.hasTag()) {
                NbtCompound nbt = new NbtCompound();
                NbtCompound og = stack.getTag();

                assert og != null;
                if (og.contains("Patterns", NbtElement.LIST_TYPE)) {
                    NbtList list = og.getList("Patterns", NbtElement.COMPOUND_TYPE);
                    if (list.size() <= 6) {
                        nbt.put("Patterns", list);
                    }
                }

                stack.setTag(nbt);
            }

            GeneralUtils.setCreativeStack(packet1, stack);
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, 0, packet1.getSlot(), stack));
        }

        return ActionResult.PASS;
    }


    private ActionResult onEntityDamage(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (entity instanceof FloorChangingEntity) {
            return ActionResult.FAIL;
        }

        BuildArena arena = this.gameMap.getArena(entity.getBlockPos());

        if (arena != null && arena.isBuilder(player)) {
            return ActionResult.PASS;
        }

        return ActionResult.FAIL;
    }

    private void onExplosion(Explosion explosion, boolean particles) {
        explosion.clearAffectedBlocks();
    }

    private TypedActionResult<ItemStack> onItemUse(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        if (GeneralUtils.equalsOrInstance(item, Items.CHORUS_FRUIT, Items.ENDER_PEARL, Items.ENDER_EYE)) {
            return TypedActionResult.fail(stack);
        }

        return TypedActionResult.pass(stack);
    }

    private ActionResult onBlockUse(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        BuildArena arena = this.gameMap.getArena(hitResult.getBlockPos());

        if (arena == null || !(arena.canBuild(hitResult.getBlockPos(), player) || arena.canBuild(hitResult.getBlockPos().offset(hitResult.getSide()), player))) {
            return ActionResult.FAIL;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (GeneralUtils.equalsOrInstance(stack.getItem(), Items.ARMOR_STAND, EntityBucketItem.class, SpawnEggItem.class, BoatItem.class)) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    private ActionResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (this.lockBuilding) {
            return ActionResult.FAIL;
        }

        BuildArena buildArena = this.gameMap.getArena(pos);

        if (buildArena != null && buildArena.canBuild(pos, player)) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private ActionResult onPlaceBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, ItemUsageContext itemUsageContext) {
        if (this.lockBuilding) {
            return ActionResult.FAIL;
        }

        BuildArena buildArena = this.gameMap.getArena(pos);

        if (buildArena != null && buildArena.canBuild(pos, player)) {
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
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
                    this.themeVotingManager.addPlayer(p);
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

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        if (this.participants.containsKey(PlayerRef.of(player))) {
            this.spawnParticipant(player);
        } else {
            this.spawnSpectator(player);
        }
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        player.getInventory().clear();
        player.changeGameMode(GameMode.CREATIVE);
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

                this.timerBar.update(new TranslatableText("text.buildbattle.timer_bar.voting_theme"), ((float) (this.themeVotingTime - time)) / this.themeVotingTime);
            }
            case BUILDING -> {
                if (time >= this.buildingTimeDuration) {
                    this.gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.HOURGLASS_PREFIX, new TranslatableText("text.buildbattle.build_time_ended").formatted(Formatting.GREEN)));
                    this.lockBuilding = true;
                    this.timerBar.setColor(BossBar.Color.RED);
                    this.timerBar.update(new TranslatableText("text.buildbattle.timer_bar.times_up"), 0);
                    for (BuildArena buildArena : this.gameMap.buildArena) {
                        buildArena.removeEntity();
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
                    this.timerBar.update(new TranslatableText("text.buildbattle.timer_bar.time_left", String.format("%02d:%02d", minutes, seconds))
                            .append(new LiteralText(" - ").formatted(Formatting.GRAY))
                            .append(new TranslatableText("text.buildbattle.timer_bar.theme").formatted(Formatting.YELLOW))
                            .append(new LiteralText(theme)), ((float) ticksLeft) / (this.buildingTimeDuration - this.themeVotingTime));

                    if (time % 10 == 0) {
                        ParticleEffect effect = new DustParticleEffect(new Vec3f(0.8f, 0.8f, 0.8f), 2.0F);
                        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                            PlayerData data = this.participants.get(PlayerRef.of(player));
                            if (data != null) {
                                ParticleOutlineRenderer.render(player, data.arena.buildingArea.min(), data.arena.buildingArea.max().add(1, 1, 1), effect);
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
        gameSpace.getPlayers().sendMessage(FormattingUtil.format(FormattingUtil.GENERAL_PREFIX, new TranslatableText("text.buildbattle.theme",
                new LiteralText(this.theme).formatted(Formatting.GOLD)
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
