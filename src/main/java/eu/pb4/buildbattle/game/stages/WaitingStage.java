package eu.pb4.buildbattle.game.stages;

import eu.pb4.buildbattle.game.BuildBattleConfig;
import eu.pb4.buildbattle.game.map.WaitingMap;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.Set;

public record WaitingStage(GameSpace gameSpace, WaitingMap map, BuildBattleConfig config, ServerWorld world) {
    public static GameOpenProcedure open(GameOpenContext<BuildBattleConfig> context) {
        BuildBattleConfig config = context.config();
        WaitingMap waitingMap = new WaitingMap(context.server(), config);

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(waitingMap.asGenerator())
                .setGameRule(GameRules.DO_WEATHER_CYCLE, false);

        return context.openWithWorld(worldConfig, (game, world) -> {
            WaitingStage waiting = new WaitingStage(game.getGameSpace(), waitingMap, config, world);

            GameWaitingLobby.addTo(game, config.playerConfig());

            game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            game.listen(GamePlayerEvents.ADD, waiting::addPlayer);
            game.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
            game.listen(GamePlayerEvents.ACCEPT, offer -> offer.teleport(world, Vec3d.ZERO));
            game.listen(EntitySpawnEvent.EVENT, (x) -> x instanceof MobEntity ? EventResult.DENY : EventResult.PASS);

            // Todo
            //Holograms.create(world, waitingMap.hologramPos, TextHelper.getHologramLines(game.getGameSpace(), config)).show();
        });
    }

    private GameResult requestStart() {
        this.gameSpace.getServer().execute(() -> {
            BuildingStage.open(this.gameSpace, this.config, () -> gameSpace.getWorlds().remove(this.world));
        });
        return GameResult.ok();
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.ADVENTURE);
        player.setVelocity(Vec3d.ZERO);
        player.getInventory().clear();
        player.fallDistance = 0.0f;
        player.getAbilities().allowFlying = true;
        player.sendAbilitiesUpdate();

        Vec3d vec3d = this.map.getSpawnLocation();

        player.teleport(this.world, vec3d.x, vec3d.y, vec3d.z, Set.of(), 0f, 0f, true);
    }

}
