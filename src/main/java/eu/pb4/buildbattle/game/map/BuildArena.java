package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.custom.FloorChangingEntity;
import eu.pb4.buildbattle.game.PlayerData;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class BuildArena {
    public final BlockBounds buildingArea;
    public final BlockBounds ground;
    public final BlockBounds bounds;
    public final BlockBounds spawn;
    public final List<PlayerData> players = new ArrayList<>();
    public final Set<UUID> playersUuid = new HashSet<>();
    private final Vec3 entityPos;
    public int score = 0;
    private UUID entityUuid;

    public BuildArena(BlockBounds area, BlockBounds ground, BlockBounds bounds, BlockBounds spawn, Vec3 entityPos) {
        this.buildingArea = area;
        this.ground = ground;
        this.bounds = bounds;
        this.spawn = spawn;
        this.entityPos = entityPos;
    }

    public boolean canBuild(BlockPos blockPos, ServerPlayer player) {
        return this.playersUuid.contains(player.getUUID()) && this.buildingArea.contains(blockPos);
    }

    public Component getBuildersText(GameSpace gameSpace) {
        if (this.players.isEmpty()) {
            return Component.translatable("text.buildbattle.nobody").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC);
        } else {
            MutableComponent text = Component.empty().withStyle(ChatFormatting.WHITE);

            for (PlayerData playerData : this.players) {
                int index = this.players.indexOf(playerData);
                if (index != 0) {
                    if (this.players.size() - index == 1) {
                        text.append(Component.translatable("text.buildbattle.and").withStyle(ChatFormatting.GOLD));
                    } else {
                        text.append(Component.literal(", ").withStyle(ChatFormatting.GOLD));
                    }
                }

                ServerPlayer player = playerData.playerRef.getEntity(gameSpace.getServer());

                if (player != null) {
                    text.append(player.getDisplayName());
                } else {
                    text.append(Component.translatable("text.buildbattle.disconnected").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
                }
            }
            return text;
        }
    }

    public void addPlayer(PlayerData data) {
        this.playersUuid.add(data.playerRef.id());
        this.players.add(data);
    }

    public void spawnEntity(ServerLevel world, float yaw) {
        FloorChangingEntity entity = new FloorChangingEntity(world);
        entity.setPos(this.entityPos.x, this.entityPos.y, this.entityPos.z);
        world.addFreshEntity(entity);
        entity.setYRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setYHeadRot(yaw);
        this.entityUuid = entity.getUUID();
    }

    public void removeEntity(ServerLevel world) {
        if (this.entityUuid != null) {
            var entity = world.getEntity(this.entityUuid);

            if (entity != null) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public List<ServerPlayer> getPlayersInArena(ServerLevel world) {
        return world.getPlayers((player) -> this.bounds.contains(player.blockPosition()));
    }

    public void teleportPlayer(ServerPlayer player, ServerLevel world) {
        double x = Mth.nextDouble(player.getRandom(), this.spawn.min().getX(), this.spawn.max().getX());
        double y = this.spawn.min().getY();
        double z = Mth.nextDouble(player.getRandom(), this.spawn.min().getZ(), this.spawn.max().getZ());

        player.teleportTo(world, x, y, z, Set.of(), player.getYRot(), player.getXRot(), true);
    }

    public boolean isBuilder(Player player) {
        return player instanceof ServerPlayer && this.playersUuid.contains(player.getUUID());
    }


    public int getPlayerCount() {
        return this.playersUuid.size();
    }
}
