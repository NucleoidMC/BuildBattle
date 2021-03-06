package eu.pb4.buildbattle.game;

import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.GameSpace;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.world.GameMode;
import eu.pb4.buildbattle.game.map.BBMap;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class BBSpawnLogic {
    private final GameSpace gameSpace;
    private final BBActive game;
    private final BBMap map;

    public BBSpawnLogic(GameSpace gameSpace, BBMap map, BBActive game) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.game = game;
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.setGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.inventory.clear();
        player.fallDistance = 0.0f;
        player.abilities.allowFlying = true;
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.abilities));
    }

    public void spawnPlayer(ServerPlayerEntity player) {
        ServerWorld world = this.gameSpace.getWorld();

        if (this.game != null) {
            if (this.game.votedArea != null) {
                this.game.votedArea.teleportPlayer(player);
                return;
            }

            BBPlayer bbPlayer = this.game.participants.get(PlayerRef.of(player));

            if (bbPlayer != null) {
                double x = MathHelper.nextDouble(player.getRandom(), bbPlayer.arena.spawn.getMin().getX(), bbPlayer.arena.spawn.getMax().getX());
                double y = bbPlayer.arena.spawn.getMin().getY();
                double z = MathHelper.nextDouble(player.getRandom(), bbPlayer.arena.spawn.getMin().getZ(), bbPlayer.arena.spawn.getMax().getZ());

                player.teleport(world, x, y, z, player.yaw, player.pitch);

                bbPlayer.arena.trySpawningVillager();
                return;
            }
        }

        double x = MathHelper.nextDouble(player.getRandom(), this.map.waitSpawn.getMin().getX(), this.map.waitSpawn.getMax().getX());
        double y = this.map.waitSpawn.getMin().getY();
        double z = MathHelper.nextDouble(player.getRandom(), this.map.waitSpawn.getMin().getZ(), this.map.waitSpawn.getMax().getZ());

        player.teleport(world, x, y, z, player.yaw, player.pitch);
    }
}
