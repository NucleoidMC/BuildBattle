package eu.pb4.buildbattle.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import sun.jvm.hotspot.opto.Block;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.Set;

public class BBMap {
    private final MapTemplate template;
    private final BBMapConfig config;
    public final Set<BuildArena> buildArenas;
    public final BlockBounds waitSpawn;
    public final BlockBounds waitInfoArea;

    public BBMap(MapTemplate template, BBMapConfig config, Set<BuildArena> buildArenaSet, BlockBounds waitSpawn, BlockBounds waitInfoArea) {
        this.template = template;
        this.config = config;
        this.buildArenas = buildArenaSet;
        this.waitSpawn = waitSpawn;
        this.waitInfoArea = waitInfoArea;
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }

    public BuildArena getBuildArea(BlockPos blockPos) {
        for (BuildArena buildArena : this.buildArenas) {
            if (buildArena.bounds.contains(blockPos)) {
                return buildArena;
            }
        }
        return null;
    }
}
