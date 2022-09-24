package eu.pb4.buildbattle.themes;

/*
 * Based on https://github.com/NucleoidMC/plasmid/blob/1.16/src/main/java/xyz/nucleoid/plasmid/game/config/GameConfigs.java
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import eu.pb4.buildbattle.BuildBattle;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import com.mojang.datafixers.util.Pair;
import xyz.nucleoid.plasmid.registry.TinyRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Set;

public class ThemesRegistry {
    private static final TinyRegistry<Theme> THEMES = TinyRegistry.create();

    public static void register() {
        ResourceManagerHelper serverData = ResourceManagerHelper.get(ResourceType.SERVER_DATA);

        serverData.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(BuildBattle.ID, "themes");
            }

            @Override
            public void reload(ResourceManager manager) {
                THEMES.clear();

                var resources = manager.findResources("themes", path -> path.getPath().endsWith(".json"));

                for (var pair : resources.entrySet()) {
                    try {
                        Resource resource = pair.getValue();
                        try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                            JsonElement json = JsonParser.parseReader(reader);

                            Identifier identifier = identifierFromPath(pair.getKey());

                            DataResult<Theme> result = Theme.CODEC.decode(JsonOps.INSTANCE, json).map(Pair::getFirst);

                            result.result().ifPresent(game -> THEMES.register(identifier, game));

                            result.error().ifPresent(error -> BuildBattle.LOGGER.error("Failed to decode game at {}: {}", pair.getKey(), error.toString()));
                        }
                    } catch (IOException e) {
                        BuildBattle.LOGGER.error("Failed to read configured game at {}", pair.getKey(), e);
                    }
                }
            }
        });
    }

    private static Identifier identifierFromPath(Identifier location) {
        String path = location.getPath();
        path = path.substring("themes/".length(), path.length() - ".json".length());
        return new Identifier(location.getNamespace(), path);
    }

    @Nullable
    public static Theme get(Identifier identifier) {
        return THEMES.get(identifier);
    }

    public static Set<Identifier> getKeys() {
        return THEMES.keySet();
    }

}
