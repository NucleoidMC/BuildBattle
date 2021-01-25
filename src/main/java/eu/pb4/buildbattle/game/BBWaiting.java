package eu.pb4.buildbattle.game;

import eu.pb4.buildbattle.Helper;
import eu.pb4.buildbattle.game.map.BBMapBuilder;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import sun.jvm.hotspot.opto.Block;
import xyz.nucleoid.plasmid.entity.FloatingText;
import xyz.nucleoid.plasmid.game.*;
import xyz.nucleoid.plasmid.game.event.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import eu.pb4.buildbattle.game.map.BBMap;
import xyz.nucleoid.fantasy.BubbleWorldConfig;

public class BBWaiting {
    private final GameSpace gameSpace;
    private final BBMap map;
    private final BBConfig config;
    private final BBSpawnLogic spawnLogic;

    private BBWaiting(GameSpace gameSpace, BBMap map, BBConfig config) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.spawnLogic = new BBSpawnLogic(gameSpace, map, null);
    }

    public static GameOpenProcedure open(GameOpenContext<BBConfig> context) {
        BBConfig config = context.getConfig();
        BBMapBuilder generator = new BBMapBuilder(config.mapConfig);
        BBMap map = generator.create(config);

        BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                .setGenerator(map.asGenerator(context.getServer()))
                .setDefaultGameMode(GameMode.SPECTATOR)
                .setGameRule(GameRules.DO_WEATHER_CYCLE, false);




        return context.createOpenProcedure(worldConfig, game -> {
            BBWaiting waiting = new BBWaiting(game.getSpace(), map, context.getConfig());

            GameWaitingLobby.applyTo(game, config.playerConfig);
            game.on(RequestStartListener.EVENT, waiting::requestStart);
            game.on(PlayerAddListener.EVENT, waiting::addPlayer);
            game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);

            game.getSpace().getWorld().getChunk(new BlockPos(0, 0, 0));

            Vec3d center = map.waitInfoArea.getCenter();

            FloatingText.spawn(game.getSpace().getWorld(), center.add(0.5, 0, 0.5), Helper.getAboutHologramText(game, config), FloatingText.VerticalAlign.CENTER);


        });
    }

    private StartResult requestStart() {
        BBActive.open(this.gameSpace, this.map, this.config);
        return StartResult.OK;
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }
}
