package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.game.BuildBattleConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateMetadata;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameplayMap {
    public final List<BuildArena> buildArena = new ArrayList<>();
    private final MapTemplate template;
    private final MinecraftServer server;

    public GameplayMap(MinecraftServer server, BuildBattleConfig config, int size) {
        this.server = server;
        try {
            MapTemplate templateArea = MapTemplateSerializer.loadFromResource(server, config.mapConfig().arena());

            MapTemplate template = MapTemplate.createEmpty();

            int sizeX = templateArea.getBounds().size().getX();
            int sizeZ = templateArea.getBounds().size().getZ();

            int squareRoot = (int) Math.ceil(Math.sqrt(size));
            int half = squareRoot / 2;

            int count = 0;

            for (int x = 0; x <= squareRoot; x++) {
                for (int z = 0; z <= squareRoot; z++) {
                    if (count >= size) {
                        break;
                    }
                    count++;
                    MapTemplate translated = templateArea.translated((x - half) * sizeX * 2, 0, (z - half) * sizeZ * 2);
                    MapTemplateMetadata meta = translated.getMetadata();

                    this.buildArena.add(new BuildArena(
                            meta.getFirstRegionBounds("build_area"),
                            meta.getFirstRegionBounds("build_ground"),
                            translated.getBounds(),
                            meta.getFirstRegionBounds("build_spawn"),
                            Objects.requireNonNull(meta.getFirstRegionBounds("build_villager")).centerBottom()
                    ));

                    template.mergeFrom(translated);
                }
            }

            template.setBiome(BiomeKeys.FOREST);

            this.template = template;
        } catch (IOException e) {
            throw new GameOpenException(Text.literal("Failed to load template"), e);
        }
    }

    @Nullable
    public BuildArena getArena(BlockPos pos) {
        for (BuildArena arena : this.buildArena) {
            if (arena.bounds.contains(pos)) {
                return arena;
            }
        }
        return null;
    }


    public ChunkGenerator asGenerator() {
        return new TemplateChunkGenerator(this.server, this.template);
    }
}

