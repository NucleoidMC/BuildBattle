package eu.pb4.buildbattle.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import eu.pb4.buildbattle.game.map.BBMapConfig;

public class BBConfig {
    public static final Codec<BBConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
            BBMapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
            Codec.INT.fieldOf("time_limit_secs").forGetter(config -> config.timeLimitSecs),
            Codec.INT.fieldOf("voting_time_secs").forGetter(config -> config.timeLimitSecs),
            Codec.INT.fieldOf("team_size").forGetter(config -> config.teamSize),
            Identifier.CODEC.fieldOf("theme").forGetter(config -> config.theme),
            Codec.BOOL.optionalFieldOf("buildswap", false).forGetter(config -> config.isBuildSwap)
        ).apply(instance, BBConfig::new));

    public final PlayerConfig playerConfig;
    public final BBMapConfig mapConfig;
    public final int timeLimitSecs;
    public final int votingTimeSecs;
    public final int teamSize;
    public final Identifier theme;
    public final boolean isBuildSwap;

    public BBConfig(PlayerConfig players, BBMapConfig mapConfig, int timeLimitSecs, int votingTimeSecs, int teamSize, Identifier theme, boolean buildswap) {
        this.playerConfig = players;
        this.mapConfig = mapConfig;
        this.timeLimitSecs = timeLimitSecs;
        this.votingTimeSecs = votingTimeSecs;
        this.teamSize = teamSize;
        this.theme = theme;
        this.isBuildSwap = buildswap;
    }
}
