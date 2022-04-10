package eu.pb4.buildbattle.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import eu.pb4.buildbattle.game.map.MapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

import java.util.Optional;

public record BuildBattleConfig(PlayerConfig playerConfig,
                                MapConfig mapConfig, int timeLimitSecs, int votingTimeSecs,
                                int teamSize, Identifier theme, boolean themeVoting, Optional<String> forcedTheme, String gamemode, boolean enableTools) {

    public static final Codec<BuildBattleConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
            MapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
            Codec.INT.fieldOf("time_limit_secs").forGetter(config -> config.timeLimitSecs),
            Codec.INT.fieldOf("voting_time_secs").forGetter(config -> config.timeLimitSecs),
            Codec.INT.fieldOf("team_size").forGetter(config -> config.teamSize),
            Identifier.CODEC.fieldOf("theme").forGetter(config -> config.theme),
            Codec.BOOL.optionalFieldOf("theme_voting", true).forGetter(config -> config.themeVoting),
            Codec.STRING.optionalFieldOf("forced_theme").forGetter(config -> config.forcedTheme),
            Codec.STRING.fieldOf("gamemode").forGetter(config -> config.gamemode),
            Codec.BOOL.optionalFieldOf("tools", false).forGetter(config -> config.enableTools)
    ).apply(instance, BuildBattleConfig::new));
}
