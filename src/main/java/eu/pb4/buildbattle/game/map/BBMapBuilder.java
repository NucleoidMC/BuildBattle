package eu.pb4.buildbattle.game.map;

import eu.pb4.buildbattle.game.BBConfig;
import net.minecraft.text.LiteralText;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateMetadata;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class BBMapBuilder {

    private final BBMapConfig config;

    public BBMapBuilder(BBMapConfig config) {
        this.config = config;
    }

    public BBMap create(BBConfig config) throws GameOpenException {
        try {
            MapTemplate templateWait = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.waitId);
            BlockBounds waitSpawn = templateWait.getMetadata().getFirstRegionBounds("wait_spawn");
            MapTemplate templateArea = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.templateId);

            MapTemplate template = MapTemplate.createEmpty();

            Set<BuildArena> buildArenas = new LinkedHashSet<>();
            template.mergeFrom(templateWait);


            int waitOffset = templateWait.getBounds().getSize().getX();
            int areaOffset = templateArea.getBounds().getSize().getX() + 16;


            for (int x = 0; x <= config.playerConfig.getMaxPlayers(); x++) {
                MapTemplate translated = templateArea.translated(waitOffset + x * areaOffset,0,0);
                MapTemplateMetadata meta = translated.getMetadata();
                buildArenas.add(new BuildArena(
                        meta.getFirstRegionBounds("build_area"),
                        meta.getFirstRegionBounds("build_ground"),
                        translated.getBounds(),
                        meta.getFirstRegionBounds("build_spawn")));

                template.mergeFrom(translated);
            }


            BBMap map = new BBMap(template, config.mapConfig, buildArenas, waitSpawn);
            template.setBiome(BiomeKeys.FOREST);

            return map;
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText("Failed to load template"), e);
        }
    }
}

