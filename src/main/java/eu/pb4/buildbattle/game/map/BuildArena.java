package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.custom.FloorChangingVillager;
import eu.pb4.buildbattle.game.BBActive;
import eu.pb4.buildbattle.game.BBPlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.util.BlockBounds;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.ArrayList;
import java.util.List;

public class BuildArena {
    public final BlockBounds area;
    public final BlockBounds ground;
    public final BlockBounds bounds;
    public final BlockBounds spawn;
    public final List<BBPlayer> players = new ArrayList();
    public final Vec3d villagerPos;
    public FloorChangingVillager villager = null;
    public BBActive game = null;
    public int score = 0;

    public BuildArena(BlockBounds area, BlockBounds ground, BlockBounds bounds, BlockBounds spawn, Vec3d villagerPos) {
        this.area = area;
        this.ground = ground;
        this.bounds = bounds;
        this.spawn = spawn;
        this.villagerPos = villagerPos;
    }

    public boolean canBuild(BlockPos blockPos, BBPlayer bbPlayer) {
        return this.players.contains(bbPlayer) && this.area.contains(blockPos);
    }

    public Text getBuilders(BBActive game) {
        if (this.players.isEmpty()) {
            return new TranslatableText("buildbattle.text.nobody").formatted(Formatting.GRAY).formatted(Formatting.ITALIC);
        } else {
            MutableText text = new LiteralText("").formatted(Formatting.WHITE);

            for (BBPlayer bbPlayer : this.players) {
                int index = this.players.indexOf(bbPlayer);
                if (index != 0) {
                    if (this.players.size() - index == 1) {
                        text.append(new TranslatableText("buildbattle.text.and").formatted(Formatting.GOLD));
                    } else {
                        text.append(new LiteralText(", ").formatted(Formatting.GOLD));
                    }
                }

                if (game.participants.get(bbPlayer.playerRef) != null) {
                    text.append(game.gameSpace.getPlayers().getEntity(bbPlayer.playerRef.getId()).getDisplayName());
                } else {
                    text.append(new TranslatableText("buildbattle.text.disconnected").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
                }
            }
            return text;
        }
    }

    public void trySpawningVillager() {
        if (this.game != null && this.villager == null) {
            FloorChangingVillager villager = new FloorChangingVillager(this, this.game, this.game.gameSpace.getWorld());
            villager.setPos(this.villagerPos.x, this.villagerPos.y, this.villagerPos.z);
            System.out.println(villager);
            this.game.gameSpace.getWorld().getChunk(villager.getBlockPos());
            this.game.gameSpace.getWorld().spawnEntity(villager);
            this.villager = villager;
        }
    }


    public void teleportPlayer(ServerPlayerEntity player) {
        double x = MathHelper.nextDouble(player.getRandom(), this.game.votedArea.spawn.getMin().getX(), this.game.votedArea.spawn.getMax().getX());
        double y = this.game.votedArea.spawn.getMin().getY();
        double z = MathHelper.nextDouble(player.getRandom(), this.game.votedArea.spawn.getMin().getZ(), this.game.votedArea.spawn.getMax().getZ());

        player.teleport(game.gameSpace.getWorld(), x, y, z, player.yaw, player.pitch);
    }
}
