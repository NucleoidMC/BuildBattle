package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.game.PlayerData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.GameSpace;

import java.util.*;

public class BuildArena {
    public final BlockBounds buildingArea;
    public final BlockBounds ground;
    public final BlockBounds bounds;
    public final BlockBounds spawn;
    public final List<PlayerData> players = new ArrayList<>();
    public final Set<UUID> playersUuid = new HashSet<>();
    private final Vec3d entityPos;
    public int score = 0;
    private UUID entityUuid;

    public BuildArena(BlockBounds area, BlockBounds ground, BlockBounds bounds, BlockBounds spawn, Vec3d entityPos) {
        this.buildingArea = area;
        this.ground = ground;
        this.bounds = bounds;
        this.spawn = spawn;
        this.entityPos = entityPos;
    }

    public boolean canBuild(BlockPos blockPos, ServerPlayerEntity player) {
        return this.playersUuid.contains(player.getUuid()) && this.buildingArea.contains(blockPos);
    }

    public Text getBuildersText(GameSpace gameSpace) {
        if (this.players.isEmpty()) {
            return Text.translatable("text.buildbattle.nobody").formatted(Formatting.GRAY).formatted(Formatting.ITALIC);
        } else {
            MutableText text = Text.empty().formatted(Formatting.WHITE);

            for (PlayerData playerData : this.players) {
                int index = this.players.indexOf(playerData);
                if (index != 0) {
                    if (this.players.size() - index == 1) {
                        text.append(Text.translatable("text.buildbattle.and").formatted(Formatting.GOLD));
                    } else {
                        text.append(Text.literal(", ").formatted(Formatting.GOLD));
                    }
                }

                ServerPlayerEntity player = playerData.playerRef.getEntity(gameSpace.getServer());

                if (player != null) {
                    text.append(player.getDisplayName());
                } else {
                    text.append(Text.translatable("text.buildbattle.disconnected").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
                }
            }
            return text;
        }
    }

    public void addPlayer(PlayerData data) {
        this.playersUuid.add(data.playerRef.id());
        this.players.add(data);
    }

    public void spawnEntity(ServerWorld world, float yaw) {
        FloorChangingEntity entity = new FloorChangingEntity(world);
        entity.setPosition(this.entityPos.x, this.entityPos.y, this.entityPos.z);
        world.spawnEntity(entity);
        entity.setYaw(yaw);
        entity.setBodyYaw(yaw);
        entity.setHeadYaw(yaw);
        this.entityUuid = entity.getUuid();
    }

    public void removeEntity(ServerWorld world) {
        if (this.entityUuid != null) {
            var entity = world.getEntity(this.entityUuid);

            if (entity != null) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public List<ServerPlayerEntity> getPlayersInArena(ServerWorld world) {
        return world.getPlayers((player) -> this.bounds.contains(player.getBlockPos()));
    }

    public void teleportPlayer(ServerPlayerEntity player, ServerWorld world) {
        double x = MathHelper.nextDouble(player.getRandom(), this.spawn.min().getX(), this.spawn.max().getX());
        double y = this.spawn.min().getY();
        double z = MathHelper.nextDouble(player.getRandom(), this.spawn.min().getZ(), this.spawn.max().getZ());

        player.teleport(world, x, y, z, player.getYaw(), player.getPitch());
    }

    public boolean isBuilder(PlayerEntity player) {
        return player instanceof ServerPlayerEntity && this.playersUuid.contains(player.getUuid());
    }


    public int getPlayerCount() {
        return this.playersUuid.size();
    }
}
