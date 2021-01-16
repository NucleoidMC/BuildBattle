package eu.pb4.buildbattle.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class BBMapConfig {
    public static final Codec<BBMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(config -> config.templateId),
            Identifier.CODEC.fieldOf("waitId").forGetter(config -> config.waitId)

    ).apply(instance, BBMapConfig::new));

    public final Identifier templateId;
    public final Identifier waitId;

    public BBMapConfig(Identifier templateId, Identifier waitId) {
        this.templateId = templateId;
        this.waitId = waitId;
    }
}
