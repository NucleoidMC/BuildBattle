package eu.pb4.buildbattle.other;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// Copied from https://github.com/NucleoidMC/plasmid/blob/1.16/src/main/java/xyz/nucleoid/plasmid/map/workspace/editor/ParticleOutlineRenderer.java
public class ParticleOutlineRenderer {
    public static void render(ServerPlayer player, BlockPos min, BlockPos max, ParticleOptions effect) {
        Edge[] edges = edges(min, max);

        int maxInterval = 5;
        int maxCount = 20;

        for (Edge edge : edges) {
            int length = edge.length();

            int interval = 1;
            if (length > 0) {
                interval = Mth.clamp(length / Math.min(maxCount, length), 1, maxInterval);
            }

            int steps = (length + interval - 1) / interval;
            for (int i = 0; i <= steps; i++) {
                double m = (double) (i * interval) / length;
                spawnParticleIfVisible(
                        player, effect,
                        edge.projX(m), edge.projY(m), edge.projZ(m)
                );
            }
        }
    }

    private static void spawnParticleIfVisible(ServerPlayer player, ParticleOptions effect, double x, double y, double z) {
        ServerLevel world = player.level();

        Vec3 delta = player.position().subtract(x, y, z);
        double length2 = delta.lengthSqr();
        if (length2 > 256 * 256) {
            return;
        }

        Vec3 rotation = player.getViewVector(1.0F);
        double dot = (delta.scale(1.0 / Math.sqrt(length2))).dot(rotation);
        if (dot > 0.0) {
            return;
        }

        world.sendParticles(
                player, effect, true, true,
                x, y, z,
                1,
                0.0, 0.0, 0.0,
                0.0
        );
    }

    private static Edge[] edges(BlockPos min, BlockPos max) {
        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();
        int maxX = max.getX();// + 1;
        int maxY = max.getY();// + 1;
        int maxZ = max.getZ();// + 1;

        return new Edge[] {
                // edges
                new Edge(minX, minY, minZ, minX, minY, maxZ),
                new Edge(minX, maxY, minZ, minX, maxY, maxZ),
                new Edge(maxX, minY, minZ, maxX, minY, maxZ),
                new Edge(maxX, maxY, minZ, maxX, maxY, maxZ),

                // front
                new Edge(minX, minY, minZ, minX, maxY, minZ),
                new Edge(maxX, minY, minZ, maxX, maxY, minZ),
                new Edge(minX, minY, minZ, maxX, minY, minZ),
                new Edge(minX, maxY, minZ, maxX, maxY, minZ),

                // back
                new Edge(minX, minY, maxZ, minX, maxY, maxZ),
                new Edge(maxX, minY, maxZ, maxX, maxY, maxZ),
                new Edge(minX, minY, maxZ, maxX, minY, maxZ),
                new Edge(minX, maxY, maxZ, maxX, maxY, maxZ),
        };
    }

    private record Edge(int startX, int startY, int startZ, int endX, int endY, int endZ) {

        double projX(double m) {
            return this.startX + (this.endX - this.startX) * m;
        }

        double projY(double m) {
            return this.startY + (this.endY - this.startY) * m;
        }

        double projZ(double m) {
            return this.startZ + (this.endZ - this.startZ) * m;
        }

        int length() {
            int dx = this.endX - this.startX;
            int dy = this.endY - this.startY;
            int dz = this.endZ - this.startZ;
            return Mth.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
    }
}
