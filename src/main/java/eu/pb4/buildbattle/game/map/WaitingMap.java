package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.game.BuildBattleConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.Objects;

public class WaitingMap {
    public final BuildBattleConfig config;
    public final MapTemplate template;
    private final MinecraftServer server;
    public final Vec3d hologramPos;
    private final Box spawnArea;

    public WaitingMap(MinecraftServer server, BuildBattleConfig config) {
        this.config = config;
        this.server = server;
        try {
            this.template = MapTemplateSerializer.loadFromResource(server, this.config.mapConfig().lobby());
            this.template.setBiome(BiomeKeys.FOREST);
        } catch (IOException e) {
            throw new GameOpenException(Text.literal("Failed to load template"), e);
        }

        this.spawnArea = Objects.requireNonNull(this.template.getMetadata().getFirstRegionBounds("wait_spawn")).asBox();
        this.hologramPos = Objects.requireNonNull(this.template.getMetadata().getFirstRegionBounds("wait_info")).centerBottom();
    }

    public Vec3d getSpawnLocation() {
        double x = Math.random() * this.spawnArea.getXLength();
        double z = Math.random() * this.spawnArea.getZLength();

        return new Vec3d(this.spawnArea.minX + x, this.spawnArea.minY, this.spawnArea.minZ + z);
    }

    public ChunkGenerator asGenerator() {
        return new TemplateChunkGenerator(this.server, this.template);
    }
}

