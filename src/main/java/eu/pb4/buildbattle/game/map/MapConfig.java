package eu.pb4.buildbattle.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public record MapConfig(Identifier arena, Identifier lobby,
                        float entityRotation) {
    public static final Codec<MapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("arena").forGetter(config -> config.arena),
            Identifier.CODEC.fieldOf("lobby").forGetter(config -> config.lobby),
            Codec.FLOAT.optionalFieldOf("entity_rotation", 0f).forGetter(config -> config.entityRotation)
    ).apply(instance, MapConfig::new));

}
