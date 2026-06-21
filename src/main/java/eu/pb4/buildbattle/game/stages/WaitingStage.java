package eu.pb4.buildbattle.game.stages;

import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.map.WaitingMap;
import eu.pb4.buildbattle.other.TextHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.gamerules.GameRules;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public record WaitingStage(GameSpace gameSpace, WaitingMap map, BuildBattleConfig config, ServerLevel world) {
    public static GameOpenProcedure open(GameOpenContext<BuildBattleConfig> context) {
        BuildBattleConfig config = context.config();
        WaitingMap waitingMap = new WaitingMap(context.server(), config);

        RuntimeLevelConfig worldConfig = new RuntimeLevelConfig()
                .setGenerator(waitingMap.asGenerator())
                .setGameRule(GameRules.ADVANCE_WEATHER, false);

        return context.openWithLevel(worldConfig, (game, world) -> {
            WaitingStage waiting = new WaitingStage(game.getGameSpace(), waitingMap, config, world);

            GameWaitingLobby.addTo(game, config.playerConfig());

            game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            game.listen(GamePlayerEvents.ADD, waiting::addPlayer);
            game.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
            game.listen(GamePlayerEvents.ACCEPT, offer -> offer.teleport(world, waitingMap.getSpawnLocation()));
            game.listen(EntitySpawnEvent.EVENT, (x) -> x instanceof Mob ? EventResult.DENY : EventResult.PASS);

            var display = EntityTypes.TEXT_DISPLAY.create(world, EntitySpawnReason.STRUCTURE);
            assert display != null;
            display.setPos(waitingMap.hologramPos.subtract(0, 1, 0));
            display.setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
            display.setText(TextHelper.getHologramLines(game.getGameSpace(), config));
            world.addFreshEntity(display);
        });
    }

    private GameResult requestStart() {
        this.gameSpace.getServer().execute(() -> {
            BuildingStage.open(this.gameSpace, this.config, () -> gameSpace.getLevels().remove(this.world));
        });
        return GameResult.ok();
    }

    private void addPlayer(ServerPlayer player) {
        this.spawnPlayer(player);
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void spawnPlayer(ServerPlayer player) {
        player.setGameMode(GameType.ADVENTURE);
        player.setDeltaMovement(Vec3.ZERO);
        player.getInventory().clearContent();
        player.fallDistance = 0.0f;
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();

        Vec3 vec3d = this.map.getSpawnLocation();

        player.teleportTo(this.world, vec3d.x, vec3d.y, vec3d.z, Set.of(), 0f, 0f, true);
    }

}
