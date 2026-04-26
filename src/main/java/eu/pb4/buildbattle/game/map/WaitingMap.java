package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.game.BuildBattleConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.Objects;

public class WaitingMap {
    public final BuildBattleConfig config;
    public final MapTemplate template;
    private final MinecraftServer server;
    public final Vec3 hologramPos;
    private final AABB spawnArea;

    public WaitingMap(MinecraftServer server, BuildBattleConfig config) {
        this.config = config;
        this.server = server;
        try {
            this.template = MapTemplateSerializer.loadFromResource(server, this.config.mapConfig().lobby());
            this.template.setBiome(Biomes.FOREST);
        } catch (IOException e) {
            throw new GameOpenException(Component.literal("Failed to load template"), e);
        }

        this.spawnArea = Objects.requireNonNull(this.template.getMetadata().getFirstRegionBounds("wait_spawn")).asBox();
        this.hologramPos = Objects.requireNonNull(this.template.getMetadata().getFirstRegionBounds("wait_info")).centerBottom();
    }

    public Vec3 getSpawnLocation() {
        double x = Math.random() * this.spawnArea.getXsize();
        double z = Math.random() * this.spawnArea.getZsize();

        return new Vec3(this.spawnArea.minX + x, this.spawnArea.minY, this.spawnArea.minZ + z);
    }

    public ChunkGenerator asGenerator() {
        return new TemplateChunkGenerator(this.server, this.template);
    }
}

